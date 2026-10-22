package com.github.livingwithhippos.nullseek.core.data.stremio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * HTTP client for interacting with Stremio addons
 */
class AddonClient(
    private val httpClient: OkHttpClient = createDefaultHttpClient(),
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

    /**
     * Fetch addon manifest from URL
     */
    suspend fun getManifest(manifestUrl: String): Result<AddonManifest> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = normalizeManifestUrl(manifestUrl)
                val request =
                    Request
                        .Builder()
                        .url(url)
                        .header("Accept", "application/json")
                        .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw AddonException("Failed to fetch manifest: HTTP ${response.code}")
                    }

                    val body =
                        response.body.string()

                    val decoded = json.decodeFromString<AddonManifest>(body)
                    decoded.copy(resources = decoded.normalizedResources())
                }
            }
        }

    /**
     * Fetch streams for a given content type and ID
     * @param addonUrl Base addon URL or manifest URL
     * @param type Content type (movie, series, etc.)
     * @param id Content ID (e.g., tt1254207 for IMDB, or tt1254207:1:1 for series)
     */
    suspend fun getStreams(
        addonUrl: String,
        type: String,
        id: String,
    ): Result<List<AddonStream>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = buildAddonResourceUrl(addonUrl, "stream", type, id)

                val request =
                    Request
                        .Builder()
                        .url(url)
                        .header("Accept", "application/json")
                        .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw AddonException("Failed to fetch streams: HTTP ${response.code}")
                    }

                    val body =
                        response.body.string()

                    val addonResponse = json.decodeFromString<AddonResponse<AddonStream>>(body)
                    addonResponse.streams ?: emptyList()
                }
            }
        }

    /**
     * Search IMDb titles by text query using IMDb's suggestion endpoint with Cinemeta catalog fallback.
     */
    suspend fun searchImdbTitles(query: String): Result<List<ImdbTitleResult>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val trimmed = query.trim()
                if (trimmed.isEmpty()) return@runCatching emptyList()

                val imdbResults =
                    runCatching {
                        val encoded = trimmed.encodeAddonPathSegment()
                        val url = "$IMDB_SUGGESTION_BASE_URL/$encoded.json?includeVideos=0"
                        val request =
                            Request
                                .Builder()
                                .url(url)
                                .header("Accept", "application/json")
                                .build()
                        httpClient.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) {
                                throw AddonException("Failed to search IMDb suggestions: HTTP ${response.code}")
                            }
                            parseImdbSuggestionResponse(response.body.string())
                        }
                    }.getOrNull()

                if (!imdbResults.isNullOrEmpty()) {
                    return@runCatching imdbResults
                }

                // Fallback to Stremio Cinemeta catalog search for movie & series
                val encodedSearch = "search=${trimmed.encodeAddonPathSegment()}"
                val combined = mutableListOf<ImdbTitleResult>()
                for (type in listOf("movie", "series")) {
                    val url =
                        buildAddonResourceUrl(
                            manifestUrl = CINEMETA_MANIFEST_URL,
                            resource = "catalog",
                            type = type,
                            id = "top",
                            extraPathSegment = encodedSearch,
                        )
                    runCatching {
                        val request =
                            Request
                                .Builder()
                                .url(url)
                                .header("Accept", "application/json")
                                .build()
                        httpClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                combined.addAll(parseCinemetaCatalogResponse(response.body.string(), type))
                            }
                        }
                    }
                }
                combined.distinctBy { "${it.type}:${it.imdbId}" }
            }
        }

    /**
     * Fetch episodes for a series from Stremio Cinemeta (/meta/series/{imdbId}.json).
     */
    suspend fun getSeriesEpisodes(imdbId: String): Result<List<SeriesEpisodeItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val baseImdbId = imdbId.trim().substringBefore(":")
                val url =
                    buildAddonResourceUrl(
                        manifestUrl = CINEMETA_MANIFEST_URL,
                        resource = "meta",
                        type = "series",
                        id = baseImdbId,
                    )
                val request =
                    Request
                        .Builder()
                        .url(url)
                        .header("Accept", "application/json")
                        .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw AddonException("Failed to fetch series episodes: HTTP ${response.code}")
                    }
                    parseCinemetaSeriesEpisodes(response.body.string(), baseImdbId)
                }
            }
        }

    internal fun parseImdbSuggestionResponse(body: String): List<ImdbTitleResult> {
        val decoded = json.decodeFromString<ImdbSuggestionResponse>(body)
        return decoded.items
            .mapNotNull { it.toImdbTitleResultOrNull() }
            .distinctBy { it.imdbId }
    }

    internal fun parseCinemetaCatalogResponse(
        body: String,
        defaultType: String,
    ): List<ImdbTitleResult> {
        val decoded = json.decodeFromString<CinemetaCatalogResponse>(body)
        return decoded.metas
            .mapNotNull { it.toImdbTitleResultOrNull(defaultType) }
            .distinctBy { it.imdbId }
    }

    internal fun parseCinemetaSeriesEpisodes(
        body: String,
        seriesImdbId: String,
    ): List<SeriesEpisodeItem> {
        val decoded = json.decodeFromString<CinemetaMetaResponse>(body)
        return decoded.meta
            ?.videos
            .orEmpty()
            .mapNotNull { it.toSeriesEpisodeItemOrNull(seriesImdbId) }
            .distinctBy { "${it.season}:${it.episode}" }
            .sortedWith(
                compareBy<SeriesEpisodeItem> { if (it.season == 0) Int.MAX_VALUE else it.season }
                    .thenBy { it.episode },
            )
    }

    /**
     * Normalize manifest URL to ensure it uses http(s) and ends with /manifest.json (preserving query parameters)
     */
    internal fun normalizeManifestUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        val normalizedScheme =
            when {
                trimmed.startsWith("http://", ignoreCase = true) ||
                    trimmed.startsWith("https://", ignoreCase = true) -> trimmed
                trimmed.startsWith("stremio://", ignoreCase = true) ->
                    "https://${trimmed.substring("stremio://".length)}"
                else -> "https://$trimmed"
            }

        val withoutFragment = normalizedScheme.substringBefore("#")
        val query = withoutFragment.substringAfter("?", "")
        val path = withoutFragment.substringBefore("?").trimEnd('/')
        val manifestPath =
            if (path.endsWith("/manifest.json")) {
                path
            } else {
                "$path/manifest.json"
            }

        return if (query.isEmpty()) manifestPath else "$manifestPath?$query"
    }

    /**
     * Build a Stremio addon resource URL (e.g. /stream/{type}/{id}.json), preserving query strings
     * and encoding path segments as in stremio-addon-client / NuvioMobile.
     */
    internal fun buildAddonResourceUrl(
        manifestUrl: String,
        resource: String,
        type: String,
        id: String,
        extraPathSegment: String? = null,
    ): String {
        val normalized = normalizeManifestUrl(manifestUrl)
        val encodedId = id.encodeAddonPathSegment()
        val baseUrl = normalized.substringBefore("?").removeSuffix("/manifest.json")
        val query =
            normalized.substringAfter("?", "").let { q ->
                if (q.isBlank()) "" else "?$q"
            }
        val resourceUrl =
            if (extraPathSegment.isNullOrEmpty()) {
                "$baseUrl/$resource/$type/$encodedId.json"
            } else {
                "$baseUrl/$resource/$type/$encodedId/$extraPathSegment.json"
            }
        return resourceUrl + query
    }

    companion object {
        private const val ADDON_URL_HEX = "0123456789ABCDEF"
        internal const val IMDB_SUGGESTION_BASE_URL = "https://v3.sg.media-imdb.com/suggestion/x"
        internal const val CINEMETA_MANIFEST_URL = "https://v3-cinemeta.strem.io/manifest.json"

        internal fun String.encodeAddonPathSegment(): String =
            buildString {
                encodeToByteArray().forEach { byte ->
                    val value = byte.toInt() and 0xFF
                    val char = value.toChar()
                    if (
                        char in 'a'..'z' ||
                        char in 'A'..'Z' ||
                        char in '0'..'9' ||
                        char == '-' ||
                        char == '_' ||
                        char == '.' ||
                        char == '~'
                    ) {
                        append(char)
                    } else {
                        append('%')
                        append(ADDON_URL_HEX[value shr 4])
                        append(ADDON_URL_HEX[value and 0x0F])
                    }
                }
            }

        private fun createDefaultHttpClient(): OkHttpClient =
            OkHttpClient
                .Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
    }
}

class AddonException(
    message: String,
) : Exception(message)
