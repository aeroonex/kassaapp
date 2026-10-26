package com.aeroonex.kassaapp

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives Device Owner lifecycle callbacks. Device Owner status is granted
 * out-of-band (adb `dpm set-device-owner` during provisioning, before any
 * user account is added to the phone) — this class only reacts to it.
 */
class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "KassaDeviceAdmin"

        fun getComponentName(context: Context): ComponentName =
            ComponentName(context.applicationContext, MyDeviceAdminReceiver::class.java)

        fun isDeviceOwner(context: Context): Boolean {
            val dpm = context.getSystemService(DevicePolicyManager::class.java)
            return dpm?.isDeviceOwnerApp(context.packageName) == true
        }

        /** Locks the launcher app down and enables kiosk (lock task) mode. */
        fun applyKioskPolicy(context: Context) {
            val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return
            val admin = getComponentName(context)
            if (!dpm.isDeviceOwnerApp(context.packageName)) {
                Log.w(TAG, "Not device owner yet, skipping kiosk policy")
                return
            }

            dpm.setLockTaskPackages(admin, arrayOf(context.packageName))

            // Make our app the persistent preferred home/launcher app.
            val intentFilter = android.content.IntentFilter(Intent.ACTION_MAIN)
            intentFilter.addCategory(Intent.CATEGORY_HOME)
            intentFilter.addCategory(Intent.CATEGORY_DEFAULT)
            dpm.addPersistentPreferredActivity(
                admin,
                intentFilter,
                ComponentName(context.packageName, MainActivity::class.java.name)
            )

            dpm.setStatusBarDisabled(admin, true)
            dpm.setKeyguardDisabled(admin, true)

            // Keep the app from being swiped away / killed by the user.
            try {
                dpm.setLockTaskFeatures(
                    admin,
                    DevicePolicyManager.LOCK_TASK_FEATURE_NONE
                )
            } catch (e: Exception) {
                Log.w(TAG, "setLockTaskFeatures failed: ${e.message}")
            }
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Device admin enabled")
        applyKioskPolicy(context)
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Log.i(TAG, "Provisioning complete")
        applyKioskPolicy(context)
        val launch = Intent(context, MainActivity::class.java)
        launch.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(launch)
    }
}
