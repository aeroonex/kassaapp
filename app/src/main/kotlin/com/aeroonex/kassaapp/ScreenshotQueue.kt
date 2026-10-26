package com.aeroonex.kassaapp

import android.content.Context
import java.io.File

/**
 * Persists captured screenshots on disk until they are uploaded, so a
 * phone with no network right now (offline shift, Wi-Fi outage, etc.)
 * doesn't lose what it captured — it just uploads everything once a
 * connection comes back.
 *
 * Uses filesDir (not cacheDir) so the OS is less likely to clear pending,
 * not-yet-uploaded screenshots under storage pressure.
 */
object ScreenshotQueue {

    // At 1 shot/5s this is roughly 24h of offline capture before we start
    // dropping the oldest ones — enough to ride out a bad Wi-Fi day
    // without the queue growing unbounded on a phone that's offline for weeks.
    private const val MAX_QUEUED = 17280

    private fun dir(context: Context): File {
        val d = File(context.filesDir, "pending_screenshots")
        if (!d.exists()) d.mkdirs()
        return d
    }

    @Synchronized
    fun enqueue(context: Context, jpegBytes: ByteArray): File {
        val d = dir(context)
        val file = File(d, "shot_${System.currentTimeMillis()}.jpg")
        file.writeBytes(jpegBytes)
        trim(d)
        return file
    }

    @Synchronized
    fun pending(context: Context): List<File> =
        dir(context).listFiles()?.sortedBy { it.name } ?: emptyList()

    private fun trim(d: File) {
        val files = d.listFiles()?.sortedBy { it.name } ?: return
        if (files.size > MAX_QUEUED) {
            files.take(files.size - MAX_QUEUED).forEach { it.delete() }
        }
    }
}
