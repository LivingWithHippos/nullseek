package com.github.livingwithhippos.nullseek.ui.newdownload

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.StringRes
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.data.repository.SeedingOption

fun Context.getFileName(uri: Uri): String {
    var name = "Unknown file"
    if (uri.scheme == "content") {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) name = cursor.getString(index)
            }
        }
    } else if (uri.scheme == "file") {
        name = uri.lastPathSegment ?: name
    }
    return name
}

fun isValidDownloadLink(link: String): Boolean {
    val trimmed = link.trim()
    return trimmed.startsWith("magnet:?xt=urn:btih:", ignoreCase = true) ||
        trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
}

/** Splits free text into the links that can be sent to Torbox. */
fun parseDownloadLinks(text: String): List<String> =
    if (text.isBlank()) emptyList() else text.split("\\s+".toRegex()).filter { isValidDownloadLink(it) }

/** Reads the picked files as (name, bytes) pairs, skipping the ones that can't be opened. */
fun Context.readPickedFiles(uris: List<Uri>): List<Pair<String, ByteArray>> =
    uris.mapNotNull { uri ->
        val bytes = runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        if (bytes != null) getFileName(uri) to bytes else null
    }

/** Appends [addition] on its own line; blank additions leave [current] unchanged. */
fun appendLinkLine(
    current: String,
    addition: String?,
): String {
    val clean = addition?.trim().orEmpty()
    if (clean.isEmpty()) return current
    val base = current.trimEnd()
    return if (base.isEmpty()) clean else "$base\n$clean"
}

/** Label of a seeding choice, shared by the New Download forms and the settings screens. */
@StringRes
fun SeedingOption.labelRes(): Int =
    when (this) {
        SeedingOption.AUTO -> R.string.seeding_auto
        SeedingOption.SEED -> R.string.seeding_seed
        SeedingOption.DONT_SEED -> R.string.seeding_dont_seed
    }
