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
