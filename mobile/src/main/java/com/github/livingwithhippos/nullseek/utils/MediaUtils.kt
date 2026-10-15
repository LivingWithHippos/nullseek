package com.github.livingwithhippos.nullseek.utils

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

private fun String.fileExtension(): String = substringAfterLast('.', "").lowercase()

fun isVideoFile(
    name: String,
    mimetype: String? = null,
): Boolean = mimetype?.startsWith("video/", ignoreCase = true) == true || name.fileExtension() in VIDEO_EXTENSIONS

fun isAudioFile(
    name: String,
    mimetype: String? = null,
): Boolean = mimetype?.startsWith("audio/", ignoreCase = true) == true || name.fileExtension() in AUDIO_EXTENSIONS

fun isSubtitleFile(name: String): Boolean = name.fileExtension() in SUBTITLE_EXTENSIONS

/** Media and subtitles are kept uncompressed so they can be streamed or sent to external players. */
fun isStreamableOrSubtitle(
    name: String,
    mimetype: String? = null,
): Boolean = isVideoFile(name, mimetype) || isAudioFile(name, mimetype) || isSubtitleFile(name)
