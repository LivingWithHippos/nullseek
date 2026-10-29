package com.github.livingwithhippos.nullseek.data.remote

import com.github.livingwithhippos.nullseek.data.model.SearchResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

internal val searchJson = Json { ignoreUnknownKeys = true }

internal fun parseDateMillis(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(value).toInstant().toEpochMilli()
    } catch (_: Exception) {
        try {
            LocalDateTime.parse(value).toInstant(ZoneOffset.UTC).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}

@Serializable
data class JackettResponseDto(
    @SerialName("Results") val results: List<JackettResultDto> = emptyList(),
)

@Serializable
data class JackettResultDto(
    @SerialName("Title") val title: String? = null,
    @SerialName("Size") val size: Long? = null,
    @SerialName("Seeders") val seeders: Int? = null,
    @SerialName("Peers") val peers: Int? = null,
    @SerialName("PublishDate") val publishDate: String? = null,
    @SerialName("MagnetUri") val magnetUri: String? = null,
    @SerialName("Link") val link: String? = null,
    @SerialName("Details") val details: String? = null,
    @SerialName("Tracker") val tracker: String? = null,
    @SerialName("CategoryDesc") val categoryDesc: String? = null,
    @SerialName("InfoHash") val infoHash: String? = null,
) {
    fun toSearchResult(fallbackIndexer: String): SearchResult =
        SearchResult(
            title = title.orEmpty(),
            indexerName = tracker?.takeIf { it.isNotBlank() } ?: fallbackIndexer,
            sizeBytes = size ?: 0,
            seeders = seeders ?: 0,
            peers = peers ?: 0,
            publishedAt = parseDateMillis(publishDate),
            magnetLink = magnetUri?.takeIf { it.isNotBlank() },
            downloadLink = link?.takeIf { it.isNotBlank() },
            infoUrl = details?.takeIf { it.isNotBlank() },
            categories =
                categoryDesc
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    .orEmpty(),
            infoHash = infoHash?.takeIf { it.isNotBlank() },
        )
}

@Serializable
data class ProwlarrCategoryDto(
    val name: String? = null,
)

@Serializable
data class ProwlarrResultDto(
    val title: String? = null,
    val size: Long? = null,
    val seeders: Int? = null,
    val leechers: Int? = null,
    val publishDate: String? = null,
    val magnetUrl: String? = null,
    val downloadUrl: String? = null,
    val infoUrl: String? = null,
    val indexer: String? = null,
    val categories: List<ProwlarrCategoryDto> = emptyList(),
    val infoHash: String? = null,
) {
    fun toSearchResult(fallbackIndexer: String): SearchResult =
        SearchResult(
            title = title.orEmpty(),
            indexerName = indexer?.takeIf { it.isNotBlank() } ?: fallbackIndexer,
            sizeBytes = size ?: 0,
            seeders = seeders ?: 0,
            peers = leechers ?: 0,
            publishedAt = parseDateMillis(publishDate),
            magnetLink = magnetUrl?.takeIf { it.isNotBlank() },
            downloadLink = downloadUrl?.takeIf { it.isNotBlank() },
            infoUrl = infoUrl?.takeIf { it.isNotBlank() },
            categories = categories.mapNotNull { it.name?.takeIf { n -> n.isNotBlank() } },
            infoHash = infoHash?.takeIf { it.isNotBlank() },
        )
}

internal fun parseJackettResults(
    body: String,
    fallbackIndexer: String,
): List<SearchResult> =
    searchJson
        .decodeFromString<JackettResponseDto>(body)
        .results
        .filter { !it.title.isNullOrBlank() }
        .map { it.toSearchResult(fallbackIndexer) }

internal fun parseProwlarrResults(
    body: String,
    fallbackIndexer: String,
): List<SearchResult> =
    searchJson
        .decodeFromString<List<ProwlarrResultDto>>(body)
        .filter { !it.title.isNullOrBlank() }
        .map { it.toSearchResult(fallbackIndexer) }
