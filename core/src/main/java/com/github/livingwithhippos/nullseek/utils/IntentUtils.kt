package com.github.livingwithhippos.nullseek.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.net.toUri
import com.github.livingwithhippos.nullseek.core.R

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

/**
 * Shows the system app chooser for a link, always prompting even if a default handler is set.
 * The MIME type is guessed from [fileName] so that media players and download managers
 * (which usually filter by type) are listed alongside browsers.
 */
fun openWith(
    context: Context?,
    url: String,
    fileName: String? = null,
    chooserTitle: String? = null,
) {
    if (context == null) return
    val uri = url.toUri()
    val extension =
        fileName
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
    val mimeType = extension?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }

    val viewIntent =
        Intent(Intent.ACTION_VIEW).apply {
            if (mimeType != null) setDataAndType(uri, mimeType) else data = uri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    try {
        context.startActivity(Intent.createChooser(viewIntent, chooserTitle))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.open_with_no_app, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Opens a file saved by the app, e.g. a playlist, in another app.
 * @return false when no app can open it
 */
fun openSavedFile(
    context: Context,
    uri: Uri,
    mimeType: String,
): Boolean {
    val intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
