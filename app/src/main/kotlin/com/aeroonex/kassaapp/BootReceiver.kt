package com.aeroonex.kassaapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Launches the cashier app right after boot (or after an app update), since
 * as device owner / home app it won't otherwise appear until the user
 * navigates to it, and we need it running to show the kiosk screen and
 * start the screenshot-upload service.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i("KassaBootReceiver", "Boot/update detected, launching MainActivity")
            val launch = Intent(context, MainActivity::class.java)
            launch.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(launch)
        }
    }
}
