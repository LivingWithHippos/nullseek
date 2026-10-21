package com.github.livingwithhippos.nullseek.data.model

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
)

fun SearchResult.hasDownloadLink(): Boolean = !magnetLink.isNullOrBlank() || !downloadLink.isNullOrBlank()

/** The magnet link if present, otherwise the torrent download link. */
fun SearchResult.preferredLink(): String? = magnetLink?.takeIf { it.isNotBlank() } ?: downloadLink?.takeIf { it.isNotBlank() }

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
