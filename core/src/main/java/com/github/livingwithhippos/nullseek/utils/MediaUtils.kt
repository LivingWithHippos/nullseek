package com.github.livingwithhippos.nullseek.utils

import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode

private val VIDEO_EXTENSIONS =
    setOf(
        "mkv",
        "mp4",
        "avi",
        "mov",
        "wmv",
        "flv",
        "webm",
        "m4v",
        "ts",
        "mpg",
        "mpeg",
        "vob",
        "iso",
    )

private val AUDIO_EXTENSIONS =
    setOf(
        "mp3",
        "flac",
        "wav",
        "aac",
        "m4a",
        "ogg",
        "opus",
        "wma",
        "alac",
        "aiff",
        "ape",
    )

private val SUBTITLE_EXTENSIONS =
    setOf(
        "srt",
        "ass",
        "ssa",
        "sub",
        "idx",
        "vtt",
        "sup",
        "smi",
        "ttml",
    )

private val DOCUMENT_EXTENSIONS =
    setOf(
        "pdf",
        "doc",
        "docx",
        "txt",
        "rtf",
        "odt",
        "ods",
        "odp",
        "md",
        "nfo",
        "csv",
        "xls",
        "xlsx",
        "ppt",
        "pptx",
    )

private val BOOK_EXTENSIONS =
    setOf(
        "epub",
        "mobi",
        "azw",
        "azw3",
        "djvu",
        "fb2",
        "cbr",
        "cbz",
        "lit",
        "lrf",
        "pdb",
        "kfx",
    )

private val ARCHIVE_EXTENSIONS =
    setOf(
        "zip",
        "rar",
        "7z",
        "tar",
        "gz",
        "bz2",
        "xz",
        "tgz",
        "zst",
        "cab",
    )

private val EXECUTABLE_EXTENSIONS =
    setOf(
        "exe",
        "msi",
        "apk",
        "aab",
        "dmg",
        "pkg",
        "deb",
        "rpm",
        "appimage",
        "bat",
        "sh",
        "cmd",
        "bin",
        "run",
        "jar",
    )

enum class FileTypeCategory {
    VIDEO,
    AUDIO,
    DOCUMENT,
    BOOK,
    ARCHIVE,
    SUBTITLE,
    EXECUTABLE,
}

private fun String.fileExtension(): String = substringAfterLast('.', "").lowercase()

fun isVideoFile(
    name: String,
    mimetype: String? = null,
): Boolean =
    mimetype?.startsWith(
        "video/",
        ignoreCase = true,
    ) == true || name.fileExtension() in VIDEO_EXTENSIONS

fun isAudioFile(
    name: String,
    mimetype: String? = null,
): Boolean =
    mimetype?.startsWith(
        "audio/",
        ignoreCase = true,
    ) == true || name.fileExtension() in AUDIO_EXTENSIONS

fun isSubtitleFile(name: String): Boolean = name.fileExtension() in SUBTITLE_EXTENSIONS

fun isDocumentFile(
    name: String,
    mimetype: String? = null,
): Boolean =
    name.fileExtension() in DOCUMENT_EXTENSIONS ||
        mimetype?.equals("application/pdf", ignoreCase = true) == true ||
        mimetype?.startsWith("text/", ignoreCase = true) == true

fun isBookFile(
    name: String,
    mimetype: String? = null,
): Boolean =
    name.fileExtension() in BOOK_EXTENSIONS ||
        mimetype?.equals("application/epub+zip", ignoreCase = true) == true ||
        mimetype?.equals("application/x-mobipocket-ebook", ignoreCase = true) == true

fun isArchiveFile(
    name: String,
    mimetype: String? = null,
): Boolean =
    name.fileExtension() in ARCHIVE_EXTENSIONS ||
        mimetype?.equals("application/zip", ignoreCase = true) == true ||
        mimetype?.equals("application/x-rar-compressed", ignoreCase = true) == true ||
        mimetype?.equals("application/x-7z-compressed", ignoreCase = true) == true ||
        mimetype?.equals("application/x-tar", ignoreCase = true) == true ||
        mimetype?.equals("application/gzip", ignoreCase = true) == true

fun isExecutableFile(
    name: String,
    mimetype: String? = null,
): Boolean =
    name.fileExtension() in EXECUTABLE_EXTENSIONS ||
        mimetype?.equals(
            "application/vnd.android.package-archive",
            ignoreCase = true,
        ) == true ||
        mimetype?.equals("application/x-msdownload", ignoreCase = true) == true ||
        mimetype?.equals("application/x-executable", ignoreCase = true) == true

fun detectFileTypeCategory(
    name: String,
    mimetype: String? = null,
): FileTypeCategory? =
    when {
        isVideoFile(name, mimetype) -> FileTypeCategory.VIDEO
        isAudioFile(name, mimetype) -> FileTypeCategory.AUDIO
        isSubtitleFile(name) -> FileTypeCategory.SUBTITLE
        isBookFile(name, mimetype) -> FileTypeCategory.BOOK
        isArchiveFile(name, mimetype) -> FileTypeCategory.ARCHIVE
        isExecutableFile(name, mimetype) -> FileTypeCategory.EXECUTABLE
        isDocumentFile(name, mimetype) -> FileTypeCategory.DOCUMENT
        else -> null
    }

/** What to offer instead of a zip for an item with several files. */
sealed interface MultiFileVideos<out T> {
    /** The only video among other files: it can get its own direct link. */
    data class Single<T>(
        val file: T,
    ) : MultiFileVideos<T>

    /** Several videos: the user has to pick one from the file list. */
    data class Many(
        val count: Int,
    ) : MultiFileVideos<Nothing>
}

/** Null for single-file items or items without videos. */
fun <T> multiFileVideos(
    files: List<T>,
    name: (T) -> String,
    mimetype: (T) -> String?,
): MultiFileVideos<T>? {
    if (files.size <= 1) return null
    val videos = files.filter { isVideoFile(name(it), mimetype(it)) }
    return when (videos.size) {
        0 -> null
        1 -> MultiFileVideos.Single(videos.first())
        else -> MultiFileVideos.Many(videos.size)
    }
}

/**
 * True when downloading the whole item gives a zip, matching the detail view models' zip rules:
 * several files are always zipped, a single file follows the zip setting. ASK is false, the dialog decides.
 */
fun <T> wholeDownloadIsZipped(
    mode: DownloadZipMode,
    files: List<T>,
    name: (T) -> String,
    mimetype: (T) -> String?,
): Boolean {
    if (files.size > 1) return true
    return when (mode) {
        DownloadZipMode.ZIP -> true
        DownloadZipMode.ASK, DownloadZipMode.UNCOMPRESSED -> false
        DownloadZipMode.ZIP_NON_MEDIA -> files.firstOrNull()?.let { !isStreamableOrSubtitle(name(it), mimetype(it)) } ?: true
    }
}

/** Media and subtitles are kept uncompressed so they can be streamed or sent to external players. */
fun isStreamableOrSubtitle(
    name: String,
    mimetype: String? = null,
): Boolean = isVideoFile(name, mimetype) || isAudioFile(name, mimetype) || isSubtitleFile(name)
