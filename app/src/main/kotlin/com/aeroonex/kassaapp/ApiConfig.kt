package com.aeroonex.kassaapp

import android.content.Context

/**
 * Server address and device id are read from a local prefs file so each
 * of the 300 phones can be provisioned without rebuilding the APK.
 * They can be set during provisioning via DPM NVRAM-backed admin extras,
 * or edited here as a fallback default before building a device-specific APK.
 */
object ApiConfig {
    private const val PREFS = "kassa_config"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_DEVICE_ID = "device_id"

    // Fallback defaults; replace with the real monitoring server before shipping.
    private const val DEFAULT_SERVER_URL = "https://kassa-monitor.example.com"

    fun getServerUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    fun setServerUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_SERVER_URL, url).apply()
    }

    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id == null) {
            id = android.provider.Settings.Secure.getString(
                context.contentResolver, android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown-${System.currentTimeMillis()}"
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }
}
