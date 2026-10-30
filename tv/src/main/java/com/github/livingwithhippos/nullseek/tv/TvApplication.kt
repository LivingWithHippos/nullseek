package com.github.livingwithhippos.nullseek.tv

import android.app.Application
import com.github.livingwithhippos.nullseek.core.di.appModule
import com.github.livingwithhippos.nullseek.tv.analytics.AppAnalytics
import logcat.AndroidLogcatLogger
import logcat.LogPriority.VERBOSE
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TvApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = VERBOSE)

        startKoin {
            androidLogger()
            androidContext(this@TvApplication)
            modules(appModule(debug = BuildConfig.DEBUG))
        }

        // Countly in debug builds, a no-op in release
        AppAnalytics.init(this)
    }
}
