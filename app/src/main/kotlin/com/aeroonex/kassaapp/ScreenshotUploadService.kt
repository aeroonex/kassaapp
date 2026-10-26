package com.aeroonex.kassaapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import androidx.core.app.NotificationCompat
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Foreground service that captures the cashier app's own window every
 * [CAPTURE_INTERVAL_MS]. Every capture is first written to
 * [ScreenshotQueue] on disk, then an upload flush is attempted — so
 * screenshots taken while offline are not lost, and the moment the
 * phone regains Wi-Fi/mobile data (detected via [ConnectivityManager]),
 * everything queued up is uploaded in order.
 *
 * Only our own window is captured (via PixelCopy against our window's
 * Surface), so no MediaProjection / "record screen" consent dialog is
 * needed — that consent is only required to capture content outside our
 * own app. Cashiers are informed this runs; the persistent notification
 * below is the visible indicator required by that agreement.
 */
class ScreenshotUploadService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val uploadExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var running = false
    private lateinit var connectivityManager: ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.i(TAG, "Network available, flushing queued screenshots")
            flushQueue()
        }
    }

    companion object {
        private const val TAG = "KassaScreenshot"
        private const val CHANNEL_ID = "kassa_monitoring"
        private const val NOTIF_ID = 1001
        const val CAPTURE_INTERVAL_MS = 5000L

        fun start(context: Context) {
            val intent = Intent(context, ScreenshotUploadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(ConnectivityManager::class.java)
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running) {
            running = true
            startForeground(NOTIF_ID, buildNotification())
            flushQueue()
            scheduleCapture()
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID, "Kassa monitoring", NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.monitoring_notice))
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    private fun scheduleCapture() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                captureToQueue()
                handler.postDelayed(this, CAPTURE_INTERVAL_MS)
            }
        }, CAPTURE_INTERVAL_MS)
    }

    private fun captureToQueue() {
        val activity = KassaApp.currentActivity ?: return
        val window = activity.window ?: return
        val view = window.decorView
        if (view.width == 0 || view.height == 0) return

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PixelCopy.request(window, bitmap, { result ->
                    if (result == PixelCopy.SUCCESS) {
                        enqueueAndFlush(bitmap)
                    } else {
                        Log.w(TAG, "PixelCopy failed: $result")
                    }
                }, handler)
            } else {
                view.draw(android.graphics.Canvas(bitmap))
                enqueueAndFlush(bitmap)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Capture failed: ${e.message}")
        }
    }

    private fun enqueueAndFlush(bitmap: Bitmap) {
        uploadExecutor.execute {
            try {
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out)
                ScreenshotQueue.enqueue(this, out.toByteArray())
            } catch (e: Exception) {
                Log.w(TAG, "Queue write failed: ${e.message}")
            }
            flushQueueInternal()
        }
    }

    /** Safe to call from any thread; the actual network/disk work always runs on [uploadExecutor]. */
    private fun flushQueue() {
        uploadExecutor.execute { flushQueueInternal() }
    }

    private fun flushQueueInternal() {
        if (!isOnline()) return
        val deviceId = ApiConfig.getDeviceId(this)
        val serverUrl = ApiConfig.getServerUrl(this)

        for (file in ScreenshotQueue.pending(this)) {
            val uploaded = uploadFile(file, serverUrl, deviceId)
            if (uploaded) {
                file.delete()
            } else {
                // Stop at the first failure so screenshots stay queued in
                // order instead of hammering a server that's still down.
                break
            }
        }
    }

    private fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun uploadFile(file: File, serverUrl: String, deviceId: String): Boolean {
        return try {
            // The queued timestamp is in the filename (shot_<millis>.jpg), so the
            // server sees when it was actually captured, not when it was sent.
            val timestamp = file.name.removePrefix("shot_").removeSuffix(".jpg")

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("device_id", deviceId)
                .addFormDataPart("timestamp", timestamp)
                .addFormDataPart(
                    "screenshot", file.name,
                    file.asRequestBody("image/jpeg".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("$serverUrl/api/devices/$deviceId/screenshots")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Upload failed: HTTP ${response.code}")
                }
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.w(TAG, "Upload error: ${e.message}")
            false
        }
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.w(TAG, "unregisterNetworkCallback failed: ${e.message}")
        }
        uploadExecutor.shutdown()
        super.onDestroy()
    }
}
