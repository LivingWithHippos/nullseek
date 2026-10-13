package com.github.livingwithhippos.nullseek.utils

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

fun openWebPage(
    context: Context?,
    url: String,
) {
    if (context == null) return
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    context.startActivity(intent)
}

fun shareText(
    context: Context?,
    text: String,
    title: String? = null,
) {
    if (context == null) return
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    context.startActivity(Intent.createChooser(intent, title))
}

