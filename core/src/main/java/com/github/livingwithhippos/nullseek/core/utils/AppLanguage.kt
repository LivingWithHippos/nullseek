package com.github.livingwithhippos.nullseek.core.utils

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.github.livingwithhippos.nullseek.core.R

/**
 * In-app language choice. Android stores it (natively from Android 13, through AppCompat before),
 * so it survives restarts without a preference of our own.
 */
enum class AppLanguage(
    val tag: String,
    @StringRes val labelRes: Int,
) {
    SYSTEM("", R.string.language_system),
    ENGLISH("en", R.string.language_english),
    ITALIAN("it", R.string.language_italian),
    FRENCH("fr", R.string.language_french),
    ;

    /** Switches the app language; visible activities are recreated with it. */
    fun apply() {
        val locales = if (this == SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    companion object {
        fun current(): AppLanguage {
            val tag = AppCompatDelegate.getApplicationLocales()[0]?.language ?: return SYSTEM
            return entries.firstOrNull { it != SYSTEM && it.tag == tag } ?: SYSTEM
        }
    }
}
