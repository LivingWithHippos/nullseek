package com.github.livingwithhippos.nullseek.tv.analytics

import android.app.Application
import com.github.livingwithhippos.nullseek.tv.BuildConfig
import logcat.logcat
import ly.count.android.sdk.Countly
import ly.count.android.sdk.CountlyConfig

/** Countly analytics, compiled only into debug builds. */
object AppAnalytics {
    fun init(app: Application) {
        if (BuildConfig.COUNTLY_SERVER_URL.isBlank() || BuildConfig.COUNTLY_APP_KEY.isBlank()) {
            logcat { "Countly not configured, skipping" }
            return
        }
        val config =
            CountlyConfig(app, BuildConfig.COUNTLY_APP_KEY, BuildConfig.COUNTLY_SERVER_URL)
                .setLoggingEnabled(true)
        config.crashes.enableCrashReporting()
        Countly.sharedInstance().init(config)
    }
}
