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

                    json.decodeFromString<AddonManifest>(body)
                }
            }
        }

    /**
     * Fetch streams for a given content type and ID
     * @param addonUrl Base addon URL
     * @param type Content type (movie, series, etc.)
     * @param id Content ID (e.g., tt1254207 for IMDB)
     */
    suspend fun getStreams(
        addonUrl: String,
        type: String,
        id: String,
    ): Result<List<AddonStream>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val baseUrl = normalizeAddonUrl(addonUrl)
                val url = "$baseUrl/stream/$type/$id.json"

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
                            ?: throw AddonException("Empty response body")

                    val addonResponse = json.decodeFromString<AddonResponse<AddonStream>>(body)
                    addonResponse.streams ?: emptyList()
                }
            }
        }

    /**
     * Normalize manifest URL to ensure it ends with /manifest.json
     */
    private fun normalizeManifestUrl(url: String): String {
        val trimmed = url.trim().removeSuffix("/")
        return if (trimmed.endsWith("/manifest.json")) {
            trimmed
        } else {
            "$trimmed/manifest.json"
        }
    }

    /**
     * Normalize addon URL by removing /manifest.json if present
     */
    private fun normalizeAddonUrl(url: String): String =
        url
            .trim()
            .removeSuffix("/")
            .removeSuffix("/manifest.json")

    companion object {
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
