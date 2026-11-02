package com.github.livingwithhippos.nullseek.core.domain

import kotlinx.coroutines.delay

/** Where a freshly added torrent stands once its metadata has been looked up. */
sealed interface ConversionOutcome {
    /** the files must be picked, see [DebridService.selectFiles] */
    data class NeedsSelection(
        val torrent: DebridTorrent,
    ) : ConversionOutcome

    /** already downloading or ready, nothing to pick */
    data class Started(
        val torrent: DebridTorrent,
    ) : ConversionOutcome

    data class Failed(
        val reason: String,
    ) : ConversionOutcome

    /** still converting the magnet, the selection can be finished from the torrent page */
    data object TimedOut : ConversionOutcome
}

/**
 * Polls a torrent until it leaves the magnet conversion step,
 * backing off from [initialDelayMs] up to [maxDelayMs].
 */
suspend fun DebridService.awaitConversion(
    torrentId: String,
    timeoutMs: Long = 60_000,
    initialDelayMs: Long = 1_500,
    maxDelayMs: Long = 3_000,
    clock: () -> Long = System::currentTimeMillis,
): ConversionOutcome {
    val start = clock()
    var wait = initialDelayMs
    var networkErrors = 0
    while (true) {
        when (val result = torrent(torrentId)) {
            is DebridResult.Ok -> {
                val torrent = result.value
                when (torrent.status) {
                    DownloadStatus.AWAITING_FILE_SELECTION -> {
                        return ConversionOutcome.NeedsSelection(torrent)
                    }

                    DownloadStatus.FAILED, DownloadStatus.EXPIRED -> {
                        return ConversionOutcome.Failed(torrent.downloadState)
                    }

                    DownloadStatus.CONVERTING, DownloadStatus.UNKNOWN -> {
                        Unit
                    }

                    else -> {
                        return ConversionOutcome.Started(torrent)
                    }
                }
            }

            is DebridResult.Err -> {
                when (result.error) {
                    is DebridError.Network, DebridError.RateLimited -> {
                        if (++networkErrors > 3) return ConversionOutcome.Failed(result.error.describe("Torrent"))
                    }

                    else -> {
                        return ConversionOutcome.Failed(result.error.describe("Torrent"))
                    }
                }
            }
        }
        if (clock() - start + wait > timeoutMs) return ConversionOutcome.TimedOut
        delay(wait)
        wait = (wait * 2).coerceAtMost(maxDelayMs)
    }
}

/** null when every file is picked, so the provider can use its "all" shortcut */
fun selectionToRequest(
    allFileIds: Collection<Int>,
    selected: Collection<Int>,
): List<Int>? = if (selected.toSet().containsAll(allFileIds)) null else selected.sorted()

/** a magnet link for a torrent hash, to add it again */
fun magnetFromHash(hash: String): String = "magnet:?xt=urn:btih:$hash"

/**
 * The files of [newTorrent] to check in the picker when re-adding [oldTorrent]:
 * the ones selected before, every file when none of them exist anymore.
 */
fun requeuePreselection(
    oldTorrent: DebridTorrent,
    newTorrent: DebridTorrent,
): Set<Int> {
    val newIds = newTorrent.files.map { it.id }.toSet()
    val kept =
        oldTorrent.files
            .filter { it.selected }
            .map { it.id }
            .filter { it in newIds }
            .toSet()
    return kept.ifEmpty { newIds }
}
