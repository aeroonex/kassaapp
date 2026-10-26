package com.aeroonex.kassaapp

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.aeroonex.kassaapp.databinding.ActivityMainBinding

/**
 * Cashier screen and kiosk launcher. Stays pinned via lock task mode so the
 * cashier cannot leave to the home screen, recents, or settings. The actual
 * POS/cashier UI will be built out here; for now this shows setup status.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (MyDeviceAdminReceiver.isDeviceOwner(this)) {
            MyDeviceAdminReceiver.applyKioskPolicy(this)
            startLockTaskIfNeeded()
            ScreenshotUploadService.start(this)
            binding.statusText.text = getString(R.string.monitoring_notice)
        } else {
            binding.statusText.text = getString(R.string.setup_required)
            Log.w("KassaMain", "App is not device owner yet; kiosk mode and monitoring are disabled")
        }
    }

    private fun startLockTaskIfNeeded() {
        val dpm = getSystemService(DevicePolicyManager::class.java)
        if (dpm?.isLockTaskPermitted(packageName) == true) {
            try {
                startLockTask()
            } catch (e: Exception) {
                Log.w("KassaMain", "startLockTask failed: ${e.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If the system ever manages to kick us off-screen, bring ourselves back.
        if (MyDeviceAdminReceiver.isDeviceOwner(this)) {
            startLockTaskIfNeeded()
        }
    }

    // Intercept hardware back/recents where possible; lock task already
    // blocks most of this on API 28+, this is defence in depth on older OS.
    override fun onBackPressed() {
        // no-op: cashiers stay inside the app
    }
}
