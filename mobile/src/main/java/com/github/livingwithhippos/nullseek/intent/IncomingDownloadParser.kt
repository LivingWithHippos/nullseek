package com.github.livingwithhippos.nullseek.intent

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import com.github.livingwithhippos.nullseek.ui.newdownload.getFileName
import com.github.livingwithhippos.nullseek.ui.newdownload.isValidDownloadLink

/** Links and files another app handed to us, ready to prefill the New Download screen. */
data class IncomingDownload(
    val links: String,
    val files: List<Uri>,
)

private val SUPPORTED_FILE_EXTENSIONS = listOf(".torrent", ".nzb")

// http(s) links and magnets inside free text, stopping at whitespace or common wrapping characters
private val LINK_REGEX = Regex("""(?:https?://|magnet:\?)[^\s<>"']+""", RegexOption.IGNORE_CASE)

/**
 * Reads magnet links, .torrent/.nzb files and shared links from an incoming intent.
 * Returns null if the intent is not one of ours or contains nothing we can download.
 */
fun Context.parseIncomingDownload(intent: Intent): IncomingDownload? {
    val links = mutableListOf<String>()
    val files = mutableListOf<Uri>()

    when (intent.action) {
        Intent.ACTION_VIEW -> {
            val data = intent.data ?: return null
            when (data.scheme?.lowercase()) {
                "magnet" -> intent.dataString?.let(links::add)
                "content", "file" -> files.add(data)
            }
        }

        Intent.ACTION_SEND -> {
            intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.let { text ->
                links += extractLinks(text.toString())
            }
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                ?.let(files::add)
        }

        Intent.ACTION_SEND_MULTIPLE -> {
            IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                ?.let(files::addAll)
        }

        else -> return null
    }

    val supportedFiles = files.filter { uri ->
        val name = runCatching { getFileName(uri) }.getOrDefault("")
        SUPPORTED_FILE_EXTENSIONS.any { name.endsWith(it, ignoreCase = true) }
    }

    if (links.isEmpty() && supportedFiles.isEmpty()) return null
    return IncomingDownload(
        links = links.distinct().joinToString("\n"),
        files = supportedFiles.distinct(),
    )
}

internal fun extractLinks(text: String): List<String> =
    LINK_REGEX.findAll(text)
        .map { it.value.trimEnd('.', ',', ')', ']', ';') }
        .filter { isValidDownloadLink(it) }
        .toList()
