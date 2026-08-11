package com.github.livingwithhippos.nullseek

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import com.github.livingwithhippos.nullseek.core.di.appModule
import com.github.livingwithhippos.nullseek.di.mobileModule

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule(debug = true), mobileModule())
        }
    }
}