package com.github.livingwithhippos.nullseek.core.data.stremio

import com.github.livingwithhippos.nullseek.core.data.ScrapedItem

/**
 * Helper functions to convert between IMDB IDs and other formats
 */
object ImdbUtils {
    /**
     * Extract IMDB ID from various formats
     * Supports: tt1234567, tt1234567:1:2, imdb:tt1234567, https://www.imdb.com/title/tt1234567/
     */
    fun extractImdbId(input: String): String? {
        val trimmed = input.trim()

        // Already in correct format (movie/series tt1234567 or episode tt1234567:1:2)
        if (trimmed.matches(Regex("^tt\\d+(?::\\d+){0,2}$"))) {
            return trimmed
        }

        // IMDB URL format
        val urlMatch = Regex("imdb\\.com/title/(tt\\d+(?::\\d+){0,2})").find(trimmed)
        if (urlMatch != null) {
            return urlMatch.groupValues[1]
        }

        // With prefix
        val prefixMatch = Regex("imdb:(tt\\d+(?::\\d+){0,2})").find(trimmed)
        if (prefixMatch != null) {
            return prefixMatch.groupValues[1]
        }

        return null
    }

    /**
     * Detect the Stremio content type ("movie" or "series") from an IMDB ID if unambiguous.
     * Episode IDs (`tt1234567:1:2`) are always "series"; plain `tt1234567` returns null so both can be queried.
     */
    fun detectContentType(imdbId: String): String? =
        if (imdbId.contains(":")) "series" else null

    /**
     * Check if a string contains or is an IMDB ID
     */
    fun containsImdbId(input: String): Boolean = extractImdbId(input) != null

    /**
     * Format IMDB ID to standard format (tt followed by digits, plus optional :season:episode)
     */
    fun formatImdbId(id: String): String {
        val extracted = extractImdbId(id)
        return extracted ?: id
    }
}

/**
 * Utility class to help with Stremio addon integration in search
 */
class AddonSearchHelper(
    private val addonManager: AddonManager,
) {
    /**
     * Search for content using Stremio addons
     * @param query Search query (can be IMDB ID or title)
     * @param type Optional explicit content type (movie, series). If null, detected from ID or both movie and series are queried.
     * @return List of scraped items from addons
     */
    suspend fun searchWithAddons(
        query: String,
        type: String? = null,
    ): Result<List<ScrapedItem>> =
        runCatching {
            // Try to extract IMDB ID
            val imdbId = ImdbUtils.extractImdbId(query)

            if (imdbId == null) {
                // If no IMDB ID, return empty list
                // Stremio addons work best with IMDB IDs
                return@runCatching emptyList()
            }

            val typesToQuery =
                when {
                    !type.isNullOrBlank() -> listOf(type)
                    else -> ImdbUtils.detectContentType(imdbId)?.let { listOf(it) } ?: listOf("movie", "series")
                }

            // Search across all enabled addons for each applicable type
            val results =
                typesToQuery.flatMap { contentType ->
                    addonManager
                        .searchStreams(contentType, imdbId)
                        .getOrElse { emptyList() }
                }

            // Convert to ScrapedItem format
            results.map { streamResult ->
                addonManager.streamToScrapedItem(
                    streamResult = streamResult,
                    contentName = query,
                )
            }
        }

    /**
     * Get streams for a specific IMDB ID
     */
    suspend fun getStreamsForImdbId(
        imdbId: String,
        type: String? = null,
    ): Result<List<ScrapedItem>> {
        val formattedId = ImdbUtils.formatImdbId(imdbId)
        return searchWithAddons(formattedId, type)
    }

    /**
     * Check if query is suitable for addon search
     */
    fun canSearchWithAddons(query: String): Boolean = ImdbUtils.containsImdbId(query)
}
