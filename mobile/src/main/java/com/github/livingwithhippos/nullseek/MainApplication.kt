package com.github.livingwithhippos.nullseek

import android.app.Application
import com.github.livingwithhippos.nullseek.core.di.appModule
import com.github.livingwithhippos.nullseek.di.mobileModule
import logcat.AndroidLogcatLogger
import logcat.LogPriority.VERBOSE
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = VERBOSE)

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule(debug = true), mobileModule())
        }
    }
}
