package com.github.livingwithhippos.nullseek.ui.lists

import androidx.compose.runtime.Immutable
import com.github.livingwithhippos.nullseek.core.domain.DebridFile
import com.github.livingwithhippos.nullseek.core.domain.DebridLink
import com.github.livingwithhippos.nullseek.core.domain.DebridResult
import com.github.livingwithhippos.nullseek.core.domain.reason
import com.github.livingwithhippos.nullseek.utils.FileTypeCategory
import com.github.livingwithhippos.nullseek.utils.detectFileTypeCategory
import com.github.livingwithhippos.nullseek.utils.isAudioFile
import com.github.livingwithhippos.nullseek.utils.isVideoFile

/** A file of a download, unlocked with its own link. */
@Immutable
data class UnlockedFile(
    val name: String,
    val size: Long?,
    val url: String,
    val streamingPageUrl: String?,
    val fileType: FileTypeCategory?,
    /** the file it comes from, when the provider unlocks single files (TorBox) */
    val fileId: Int? = null,
    /** why the provider refused to unlock it, the url is empty then */
    val error: String? = null,
) {
    val failed: Boolean get() = error != null
}

/** The video and audio files that can be downloaded. */
fun List<DebridFile>.mediaFiles(): List<DebridFile> =
    filter { it.selected && (isVideoFile(it.name, it.mimetype) || isAudioFile(it.name, it.mimetype)) }

/** How an unlock of several files ended. */
data class UnlockOutcome(
    val unlocked: Int,
    val failures: List<String>,
)

/**
 * Asks a link for each of [targets], one at a time, adding each file to [existing] as soon as it's ready.
 * Files that failed before are replaced by the new try.
 * @param onUpdate gets the files so far and the done/total progress, null progress when finished
 * @return null when [isStillCurrent] turned false, e.g. the account or the item changed
 */
suspend fun unlockOneByOne(
    targets: List<DebridFile>,
    existing: List<UnlockedFile>,
    fetch: suspend (DebridFile) -> DebridResult<DebridLink>,
    onUpdate: (List<UnlockedFile>, Pair<Int, Int>?) -> Unit,
    isStillCurrent: () -> Boolean = { true },
): UnlockOutcome? {
    val retried = targets.map { it.id }.toSet()
    var files = existing.filterNot { it.failed && it.fileId in retried }
    onUpdate(files, 0 to targets.size)
    var unlocked = 0
    val failures = mutableListOf<String>()
    targets.forEachIndexed { index, file ->
        val result = fetch(file)
        if (!isStillCurrent()) return null
        val name = file.shortName.ifBlank { file.name.substringAfterLast('/') }
        val fileType = detectFileTypeCategory(file.name, file.mimetype)
        files =
            files +
            when (result) {
                is DebridResult.Ok -> {
                    unlocked++
                    UnlockedFile(
                        name = name,
                        size = file.size,
                        url = result.value.url,
                        streamingPageUrl = result.value.streamingPageUrl,
                        fileType = fileType,
                        fileId = file.id,
                    )
                }

                is DebridResult.Err -> {
                    val reason = result.error.reason()
                    failures.add(reason)
                    UnlockedFile(
                        name = name,
                        size = file.size,
                        url = "",
                        streamingPageUrl = null,
                        fileType = fileType,
                        fileId = file.id,
                        error = reason,
                    )
                }
            }
        onUpdate(files, index + 1 to targets.size)
    }
    onUpdate(files, null)
    return UnlockOutcome(unlocked, failures)
}

/** The files of [candidates] not unlocked yet, the failed ones are tried again. */
fun List<DebridFile>.notUnlockedIn(unlocked: List<UnlockedFile>): List<DebridFile> {
    val done = unlocked.filter { !it.failed }.mapNotNull { it.fileId }.toSet()
    return filter { it.id !in done }
}

/** The final message of an unlock, naming the reason when all the failures share it. */
fun unlockSummary(
    unlocked: Int,
    failures: List<String>,
): String {
    val reason = failures.distinct().singleOrNull()
    return when {
        failures.isEmpty() -> "Unlocked $unlocked files"
        unlocked == 0 && reason != null -> "Failed to unlock the files: $reason"
        unlocked == 0 -> "Failed to unlock the files, see the list for the reasons"
        reason != null -> "Unlocked $unlocked files, ${failures.size} failed: $reason"
        else -> "Unlocked $unlocked files, ${failures.size} failed, see the list for the reasons"
    }
}

/**
 * Compares names ignoring case and reading runs of digits as numbers,
 * so "Ep2" comes before "Ep10" and a series plays in order.
 */
val naturalNameOrder: Comparator<String> =
    Comparator { a, b ->
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val ca = a[i]
            val cb = b[j]
            if (ca.isDigit() && cb.isDigit()) {
                val startA = i
                val startB = j
                while (i < a.length && a[i].isDigit()) i++
                while (j < b.length && b[j].isDigit()) j++
                val numA = a.substring(startA, i).trimStart('0')
                val numB = b.substring(startB, j).trimStart('0')
                if (numA.length != numB.length) return@Comparator numA.length - numB.length
                val byValue = numA.compareTo(numB)
                if (byValue != 0) return@Comparator byValue
            } else {
                val byChar = ca.lowercaseChar().compareTo(cb.lowercaseChar())
                if (byChar != 0) return@Comparator byChar
                i++
                j++
            }
        }
        (a.length - i) - (b.length - j)
    }

/** The type of the saved playlists. */
const val PLAYLIST_MIME_TYPE = "application/vnd.apple.mpegurl"

/** An M3U8 playlist with the links that worked, in list order or sorted by name. */
fun buildM3u8Playlist(
    files: List<UnlockedFile>,
    sortByName: Boolean,
): String {
    val working = files.filter { !it.failed && it.url.isNotBlank() }
    val ordered = if (sortByName) working.sortedWith(compareBy(naturalNameOrder) { it.name }) else working
    return buildString {
        append("#EXTM3U\n")
        ordered.forEach { file ->
            val title = file.name.replace('\r', ' ').replace('\n', ' ')
            append("#EXTINF:-1,").append(title).append('\n')
            append(file.url).append('\n')
        }
    }
}

/** A file name for the playlist of [itemName], without characters file systems refuse. */
fun playlistFileName(itemName: String): String {
    val clean =
        itemName
            .replace(Regex("""[/\\:*?"<>|]"""), "_")
            .trim()
            .take(100)
            .ifBlank { "playlist" }
    return "$clean.m3u8"
}
