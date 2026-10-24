package com.github.livingwithhippos.nullseek.core.data.stremio

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Domain model representing a movie or series returned from IMDb / Cinemeta title search.
 */
@Keep
@Serializable
@Parcelize
data class ImdbTitleResult(
    val imdbId: String,
    val title: String,
    val year: String? = null,
    val type: String = "movie",
    val posterUrl: String? = null,
    val stars: String? = null,
) : Parcelable {
    val isSeries: Boolean
        get() = type.equals("series", ignoreCase = true)
}

/**
 * Domain model representing a single episode of a series from Cinemeta metadata.
 */
@Keep
@Serializable
@Parcelize
data class SeriesEpisodeItem(
    val id: String,
    val season: Int,
    val episode: Int,
    val title: String,
    val released: String? = null,
    val overview: String? = null,
    val thumbnail: String? = null,
) : Parcelable

@Keep
@Serializable
internal data class ImdbSuggestionResponse(
    @SerialName("d") val items: List<ImdbSuggestionItem> = emptyList(),
)

@Keep
@Serializable
internal data class ImdbSuggestionItem(
    @SerialName("id") val id: String? = null,
    @SerialName("l") val title: String? = null,
    @SerialName("q") val qualifier: String? = null,
    @SerialName("qid") val qualifierId: String? = null,
    @SerialName("y") val year: Int? = null,
    @SerialName("yr") val yearRange: String? = null,
    @SerialName("s") val stars: String? = null,
    @SerialName("i") val image: ImdbSuggestionImage? = null,
) {
    fun toImdbTitleResultOrNull(): ImdbTitleResult? {
        val rawId = id?.trim().orEmpty()
        val rawTitle = title?.trim().orEmpty()
        if (!rawId.startsWith("tt") || rawTitle.isEmpty()) return null

        val normalizedQid = qualifierId?.trim()?.lowercase()
        val normalizedQ = qualifier?.trim()?.lowercase()

        // Exclude non-video entries such as video games, podcasts, or people
        if (normalizedQid in EXCLUDED_QIDS) return null

        val resolvedType =
            when {
                normalizedQid in SERIES_QIDS -> "series"

                normalizedQid in MOVIE_QIDS -> "movie"

                normalizedQ != null && normalizedQ.contains("series") -> "series"

                normalizedQ != null &&
                    (
                        normalizedQ.contains("movie") || normalizedQ.contains("film") || normalizedQ.contains("feature") ||
                            normalizedQ.contains("short") ||
                            normalizedQ.contains("video")
                    ) -> "movie"

                normalizedQid == null && normalizedQ == null -> return null

                else -> "movie"
            }

        val resolvedYear =
            yearRange?.trim()?.takeIf { it.isNotEmpty() }
                ?: year?.toString()

        return ImdbTitleResult(
            imdbId = rawId,
            title = rawTitle,
            year = resolvedYear,
            type = resolvedType,
            posterUrl = image?.imageUrl?.trim()?.takeIf { it.isNotEmpty() },
            stars = stars?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    private companion object {
        val SERIES_QIDS = setOf("tvseries", "tvminiseries", "tvepisode")
        val MOVIE_QIDS = setOf("movie", "feature", "tvmovie", "short", "video", "tvspecial")
        val EXCLUDED_QIDS = setOf("videogame", "podcastseries", "podcastepisode", "musicvideo")
    }
}

@Keep
@Serializable
internal data class ImdbSuggestionImage(
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("width") val width: Int? = null,
    @SerialName("height") val height: Int? = null,
)

@Keep
@Serializable
internal data class CinemetaCatalogResponse(
    @SerialName("metas") val metas: List<CinemetaCatalogItem> = emptyList(),
)

@Keep
@Serializable
internal data class CinemetaCatalogItem(
    @SerialName("id") val id: String? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("releaseInfo") val releaseInfo: String? = null,
    @SerialName("year") val year: String? = null,
    @SerialName("poster") val poster: String? = null,
) {
    fun toImdbTitleResultOrNull(defaultType: String): ImdbTitleResult? {
        val resolvedId =
            imdbId?.trim()?.takeIf { it.startsWith("tt") }
                ?: id?.trim()?.takeIf { it.startsWith("tt") }
                ?: return null
        val resolvedTitle = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val resolvedType =
            when (type?.trim()?.lowercase()) {
                "series" -> "series"
                "movie" -> "movie"
                else -> defaultType
            }
        val resolvedYear =
            releaseInfo?.trim()?.takeIf { it.isNotEmpty() }
                ?: year?.trim()?.takeIf { it.isNotEmpty() }
        return ImdbTitleResult(
            imdbId = resolvedId,
            title = resolvedTitle,
            year = resolvedYear,
            type = resolvedType,
            posterUrl = poster?.trim()?.takeIf { it.isNotEmpty() },
        )
    }
}

@Keep
@Serializable
internal data class CinemetaMetaResponse(
    @SerialName("meta") val meta: CinemetaSeriesMeta? = null,
)

@Keep
@Serializable
internal data class CinemetaSeriesMeta(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("videos") val videos: List<CinemetaVideoDto> = emptyList(),
)

@Keep
@Serializable
internal data class CinemetaVideoDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("season") val season: Int? = null,
    @SerialName("episode") val episode: Int? = null,
    @SerialName("number") val number: Int? = null,
    @SerialName("released") val released: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("thumbnail") val thumbnail: String? = null,
) {
    fun toSeriesEpisodeItemOrNull(seriesImdbId: String): SeriesEpisodeItem? {
        val idParts = id?.trim()?.split(":")
        val parsedSeason = season ?: idParts?.getOrNull(1)?.toIntOrNull() ?: return null
        val parsedEpisode = episode ?: number ?: idParts?.getOrNull(2)?.toIntOrNull() ?: return null
        if (parsedSeason < 0 || parsedEpisode <= 0) return null

        val resolvedId =
            id?.trim()?.takeIf { it.startsWith("tt") && it.contains(":") }
                ?: "$seriesImdbId:$parsedSeason:$parsedEpisode"
        val resolvedTitle =
            name?.trim()?.takeIf { it.isNotEmpty() }
                ?: title?.trim()?.takeIf { it.isNotEmpty() }
                ?: "Episode $parsedEpisode"
        val resolvedOverview =
            overview?.trim()?.takeIf { it.isNotEmpty() }
                ?: description?.trim()?.takeIf { it.isNotEmpty() }
        val resolvedReleased =
            released?.trim()?.takeIf { it.isNotEmpty() }?.substringBefore("T")

        return SeriesEpisodeItem(
            id = resolvedId,
            season = parsedSeason,
            episode = parsedEpisode,
            title = resolvedTitle,
            released = resolvedReleased,
            overview = resolvedOverview,
            thumbnail = thumbnail?.trim()?.takeIf { it.isNotEmpty() },
        )
    }
}
