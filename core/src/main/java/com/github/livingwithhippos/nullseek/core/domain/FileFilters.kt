package com.github.livingwithhippos.nullseek.core.domain

import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.utils.isAudioFile
import com.github.livingwithhippos.nullseek.utils.isSubtitleFile
import com.github.livingwithhippos.nullseek.utils.isVideoFile

/** The file types the file pickers can filter by. */
enum class FileTypeFilter { VIDEO, AUDIO, SUBTITLES }

val FileTypeFilter.labelRes: Int
    get() =
        when (this) {
            FileTypeFilter.VIDEO -> R.string.filter_video
            FileTypeFilter.AUDIO -> R.string.filter_audio
            FileTypeFilter.SUBTITLES -> R.string.filter_subtitles
        }

fun DebridFile.matches(filter: FileTypeFilter): Boolean =
    when (filter) {
        FileTypeFilter.VIDEO -> isVideoFile(name, mimetype)
        FileTypeFilter.AUDIO -> isAudioFile(name, mimetype)
        FileTypeFilter.SUBTITLES -> isSubtitleFile(name)
    }

/** The files matching any of [filters], every file when empty. */
fun List<DebridFile>.filterByType(filters: Set<FileTypeFilter>): List<DebridFile> =
    if (filters.isEmpty()) this else filter { file -> filters.any { file.matches(it) } }

/** The files whose name or folder contains [query], ignoring case. */
fun List<DebridFile>.filterByQuery(query: String): List<DebridFile> {
    val clean = query.trim()
    if (clean.isEmpty()) return this
    return filter { it.name.contains(clean, ignoreCase = true) || it.shortName.contains(clean, ignoreCase = true) }
}

/** The types with at least one file, to hide the useless chips. */
fun List<DebridFile>.availableTypes(): List<FileTypeFilter> = FileTypeFilter.entries.filter { type -> any { it.matches(type) } }
