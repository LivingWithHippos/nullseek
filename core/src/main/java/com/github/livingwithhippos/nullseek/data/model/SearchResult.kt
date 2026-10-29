package com.github.livingwithhippos.nullseek.data.model

import com.github.livingwithhippos.nullseek.core.utils.extractInfoHash
import com.github.livingwithhippos.nullseek.core.utils.normalizeInfoHash

data class SearchResult(
    val title: String,
    val indexerName: String,
    val sizeBytes: Long = 0,
    val seeders: Int = 0,
    val peers: Int = 0,
    /** Publication time in epoch milliseconds, null if unknown. */
    val publishedAt: Long? = null,
    val magnetLink: String? = null,
    val downloadLink: String? = null,
    val infoUrl: String? = null,
    val categories: List<String> = emptyList(),
    val isDirectStream: Boolean = false,
    val isFromStremioAddon: Boolean = false,
    /** Torrent info hash reported by the indexer, when there is one. */
    val infoHash: String? = null,
    /** True when TorBox reports this torrent as cached. */
    val isCached: Boolean = false,
)

fun SearchResult.hasDownloadLink(): Boolean = !magnetLink.isNullOrBlank() || !downloadLink.isNullOrBlank()

/** True when this result represents a direct playable/downloadable stream rather than a torrent/magnet/NZB. */
fun SearchResult.isDirectStreamResult(): Boolean = isDirectStream && !downloadLink.isNullOrBlank()

/** The magnet link if present, otherwise the torrent download link. */
fun SearchResult.preferredLink(): String? = magnetLink?.takeIf { it.isNotBlank() } ?: downloadLink?.takeIf { it.isNotBlank() }

/** The torrent info hash as lowercase hex, from the indexer or the magnet link. */
fun SearchResult.torrentHash(): String? = normalizeInfoHash(infoHash) ?: extractInfoHash(magnetLink)

/** Marks the result as cached when its torrent hash is in [cached]. */
fun SearchResult.markCached(cached: Set<String>): SearchResult {
    if (cached.isEmpty()) return this
    val isCached = torrentHash()?.let { it in cached } == true
    return if (isCached == this.isCached) this else copy(isCached = isCached)
}

/** Progress of a single indexer during a search. */
sealed interface IndexerSearchOutcome {
    val indexer: Indexer

    data class Success(
        override val indexer: Indexer,
        val results: List<SearchResult>,
    ) : IndexerSearchOutcome

    data class Failure(
        override val indexer: Indexer,
        val error: Throwable,
    ) : IndexerSearchOutcome
}
