package com.github.livingwithhippos.nullseek.tv.analytics

import android.app.Application

/** Analytics are only in debug builds: release builds do nothing. */
object AppAnalytics {
    @Suppress("UNUSED_PARAMETER")
    fun init(app: Application) {}
}
