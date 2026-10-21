package com.github.livingwithhippos.nullseek.core.data.stremio

import com.github.livingwithhippos.nullseek.core.data.ScrapedItem

/**
 * Helper functions to convert between IMDB IDs and other formats
 */
object ImdbUtils {
    /**
     * Extract IMDB ID from various formats
     * Supports: tt1234567, imdb:tt1234567, https://www.imdb.com/title/tt1234567/
     */
    fun extractImdbId(input: String): String? {
        val trimmed = input.trim()

        // Already in correct format
        if (trimmed.matches(Regex("^tt\\d+$"))) {
            return trimmed
        }

        // IMDB URL format
        val urlMatch = Regex("imdb\\.com/title/(tt\\d+)").find(trimmed)
        if (urlMatch != null) {
            return urlMatch.groupValues[1]
        }

        // With prefix
        val prefixMatch = Regex("imdb:(tt\\d+)").find(trimmed)
        if (prefixMatch != null) {
            return prefixMatch.groupValues[1]
        }

        return null
    }

    /**
     * Check if a string contains or is an IMDB ID
     */
    fun containsImdbId(input: String): Boolean = extractImdbId(input) != null

    /**
     * Format IMDB ID to standard format (tt followed by digits)
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
     * @param type Content type (movie, series, etc.)
     * @return List of scraped items from addons
     */
    suspend fun searchWithAddons(
        query: String,
        type: String = "movie",
    ): Result<List<ScrapedItem>> =
        runCatching {
            // Try to extract IMDB ID
            val imdbId = ImdbUtils.extractImdbId(query)

            if (imdbId == null) {
                // If no IMDB ID, return empty list
                // Stremio addons work best with IMDB IDs
                return@runCatching emptyList()
            }

            // Search across all enabled addons
            val results =
                addonManager
                    .searchStreams(type, imdbId)
                    .getOrElse { emptyList() }

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
        type: String = "movie",
    ): Result<List<ScrapedItem>> {
        val formattedId = ImdbUtils.formatImdbId(imdbId)
        return searchWithAddons(formattedId, type)
    }

    /**
     * Check if query is suitable for addon search
     */
    fun canSearchWithAddons(query: String): Boolean = ImdbUtils.containsImdbId(query)
}
