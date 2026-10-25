package com.github.livingwithhippos.nullseek.ui.search

import com.github.livingwithhippos.nullseek.utils.detectFileTypeCategory
import com.github.livingwithhippos.nullseek.utils.isStreamableOrSubtitle

/**
 * Resolves a filename with a media extension from [title] or [url] so MIME type detection and
 * streamable checks recognize Stremio direct streams (even when multi-line descriptions are used).
 */
fun resolveStreamFileName(
    title: String,
    url: String,
): String {
    val lines =
        title
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
    for (line in lines) {
        val candidate = line.substringAfterLast('/').substringAfterLast('\\').trim()
        if (isStreamableOrSubtitle(candidate)) {
            return candidate
        }
    }

    val urlPathSegment =
        url
            .substringBefore('?')
            .substringBefore('#')
            .substringAfterLast('/')
            .trim()
    if (isStreamableOrSubtitle(urlPathSegment)) {
        return urlPathSegment
    }

    val firstLine =
        lines
            .firstOrNull()
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            .orEmpty()
            .ifBlank { "stream" }
    return if (detectFileTypeCategory(firstLine) != null) {
        firstLine
    } else {
        "$firstLine.mp4"
    }
}
