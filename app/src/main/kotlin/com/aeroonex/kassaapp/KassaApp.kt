package com.aeroonex.kassaapp

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Tracks the current foreground activity so the screenshot service can
 * capture its window without holding a static Activity reference elsewhere.
 */
class KassaApp : Application() {

    companion object {
        var currentActivity: Activity? = null
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity
            }

            override fun onActivityPaused(activity: Activity) {
                if (currentActivity === activity) currentActivity = null
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
