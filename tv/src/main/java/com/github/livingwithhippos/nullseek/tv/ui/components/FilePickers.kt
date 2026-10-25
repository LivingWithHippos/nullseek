package com.github.livingwithhippos.nullseek.tv.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.github.livingwithhippos.nullseek.core.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Many TVs ship without a document picker, so launching one must not crash the app. */
fun <I> ActivityResultLauncher<I>.launchSafely(
    context: Context,
    input: I,
) {
    try {
        launch(input)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_file_manager, Toast.LENGTH_LONG).show()
    }
}

suspend fun Context.readTextFromUri(uri: Uri): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        }.getOrNull()
    }

suspend fun Context.writeTextToUri(
    uri: Uri,
    text: String,
): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                ?: error("Unable to open output stream")
        }.isSuccess
    }
