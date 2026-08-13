package com.github.livingwithhippos.nullseek.utils

import android.content.ClipDescription.MIMETYPE_TEXT_HTML
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context

fun getClipboardText(context: Context?): String? {
    if (context == null) return null
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    if (clipboard.hasPrimaryClip().not()) return null
    if (clipboard.primaryClipDescription?.hasMimeType(
            MIMETYPE_TEXT_PLAIN,
        ) == true || clipboard.primaryClipDescription?.hasMimeType(
            MIMETYPE_TEXT_HTML,
        ) == true
    ) {
        val item = clipboard.primaryClip?.getItemAt(0)
        return item?.text.toString()
    }
    return null
}

fun setClipboardText(
    context: Context?,
    text: String,
) {
    if (context == null) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = android.content.ClipData.newPlainText("Copied Text", text)
    clipboard.setPrimaryClip(clip)
}
