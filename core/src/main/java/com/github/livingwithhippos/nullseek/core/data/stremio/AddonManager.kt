package com.github.livingwithhippos.nullseek.core.data.stremio

import com.github.livingwithhippos.nullseek.core.data.database.model.StremioAddonEntity
import com.github.livingwithhippos.nullseek.core.data.repository.StremioAddonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/**
 * Manager for Stremio addon operations
 * Coordinates between the HTTP client and local storage
 */
class AddonManager(
    private val addonClient: AddonClient,
    private val repository: StremioAddonRepository,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    /**
     * Install a new addon from its manifest URL
     */
    suspend fun installAddon(
        manifestUrl: String,
        customName: String? = null,
    ): Result<StremioAddonEntity> =
        runCatching {
            val trimmedUrl = manifestUrl.trim()
            // Fetch manifest from URL
            val manifest = addonClient.getManifest(trimmedUrl).getOrThrow()
            val existing = repository.getAddonByUrl(trimmedUrl)
            val resolvedCustomName =
                customName?.trim()?.takeIf { it.isNotBlank() }
                    ?: existing?.customName?.takeIf { it.isNotBlank() }
                    ?: manifest.name

            // Convert to entity
            val entity =
                StremioAddonEntity(
                    manifestUrl = trimmedUrl,
                    id = manifest.id,
                    name = manifest.name,
                    description = manifest.description,
                    version = manifest.version,
                    resources = json.encodeToString(manifest.normalizedResources()),
                    types = json.encodeToString(manifest.types),
                    logo = manifest.logo,
                    background = manifest.background,
                    isEnabled = existing?.isEnabled ?: true,
                    addedTimestamp = existing?.addedTimestamp ?: System.currentTimeMillis(),
                    customName = resolvedCustomName,
                )

            // Save to database
            repository.insertAddon(entity)
            entity
        }

    /**
     * Update an addon's custom name and enabled state
     */
    suspend fun updateAddonDetails(
        manifestUrl: String,
        customName: String?,
        enabled: Boolean,
    ): Result<Unit> =
        runCatching {
            val normalizedName = customName?.trim()?.takeIf { it.isNotBlank() }
            repository.updateAddonDetails(manifestUrl, normalizedName, enabled)
        }

    /**
     * Uninstall an addon
     */
    suspend fun uninstallAddon(manifestUrl: String): Result<Unit> =
        runCatching {
            repository.deleteAddon(manifestUrl)
        }

    /**
     * Enable or disable an addon
     */
    suspend fun setAddonEnabled(
        manifestUrl: String,
        enabled: Boolean,
    ): Result<Unit> =
        runCatching {
            repository.setAddonEnabled(manifestUrl, enabled)
        }

    /**
     * Get all installed addons
     */
    fun getAllAddons(): Flow<List<StremioAddonEntity>> = repository.getAllAddons()

    /**
     * Get only enabled addons
     */
    fun getEnabledAddons(): Flow<List<StremioAddonEntity>> = repository.getEnabledAddons()

    /**
     * Search IMDb titles by text query
     */
    suspend fun searchImdbTitles(query: String): Result<List<ImdbTitleResult>> = addonClient.searchImdbTitles(query)

    /**
     * Fetch episodes for a series by its IMDb ID
     */
    suspend fun getSeriesEpisodes(imdbId: String): Result<List<SeriesEpisodeItem>> = addonClient.getSeriesEpisodes(imdbId)

    /**
     * Fetch rich title metadata (and episodes if series) by its IMDb ID
     */
    suspend fun getTitleMeta(
        imdbId: String,
        type: String,
        existing: ImdbTitleResult? = null,
    ): Result<TitleMetaDetails> = addonClient.getTitleMeta(imdbId, type, existing)

    /**
     * Search for streams across all enabled addons
     * @param type Content type (movie, series, etc.)
     * @param id Content ID (e.g., tt1254207 for IMDB)
     * @param query Optional search query string
     */
    suspend fun searchStreams(
        type: String,
        id: String,
        query: String? = null,
    ): Result<List<AddonStreamResult>> =
        runCatching {
            val enabledAddons = repository.getEnabledAddons().first()
            val results = mutableListOf<AddonStreamResult>()

            enabledAddons.forEach { addon ->
                try {
                    // Parse addon resources
                    val addonResources = json.decodeFromString<List<AddonResource>>(addon.resources)
                    val defaultTypes =
                        runCatching { json.decodeFromString<List<String>>(addon.types) }
                            .getOrDefault(emptyList())

                    // Find stream resources that match the content type
                    val matchingStreamResources =
                        addonResources.filter { resource ->
                            resource.name == "stream" &&
                                resource.types.ifEmpty { defaultTypes }.contains(type)
                        }

                    if (matchingStreamResources.isEmpty()) {
                        return@forEach
                    }

                    // Check ID prefix compatibility
                    val compatibleResource =
                        matchingStreamResources.firstOrNull { resource ->
                            matchesIdPrefix(id, resource.idPrefixes)
                        }

                    if (compatibleResource == null) {
                        return@forEach
                    }

                    // Fetch streams from this addon
                    addonClient
                        .getStreams(addon.manifestUrl, type, id)
                        .onSuccess { streams ->
                            streams.forEach { stream ->
                                results.add(
                                    AddonStreamResult(
                                        stream = stream,
                                        addonName = addon.displayName,
                                        addonId = addon.id,
                                    ),
                                )
                            }
                        }.onFailure { e ->
                            // Log but continue with other addons
                            println("Failed to fetch streams from ${addon.name}: ${e.message}")
                        }
                } catch (e: Exception) {
                    // Handle parsing errors gracefully
                    println("Failed to parse addon resources for ${addon.name}: ${e.message}")
                }
            }

            results
        }

    /**
     * Check whether [id] matches the allowed [idPrefixes] for a Stremio addon resource.
     * Supports prefix matching via `startsWith` (per stremio-addon-client / NuvioMobile) as well as extracted prefix.
     */
    internal fun matchesIdPrefix(
        id: String,
        idPrefixes: List<String>?,
    ): Boolean {
        if (idPrefixes.isNullOrEmpty()) return true
        val extracted = extractIdPrefix(id)
        return idPrefixes.any { prefix ->
            id.startsWith(prefix) || (extracted.isNotEmpty() && prefix == extracted)
        }
    }

    /**
     * Extract ID prefix from content ID
     * Examples: tt1254207 -> tt, kitsu:12345 -> kitsu, imdb:tt123 -> imdb
     */
    internal fun extractIdPrefix(id: String): String {
        // Check for colon-separated prefix (e.g., "kitsu:12345", "imdb:tt123")
        val firstColon = id.indexOf(':')
        if (firstColon > 0) {
            val beforeColon = id.substring(0, firstColon)
            if (beforeColon.all { it.isLetter() }) {
                return beforeColon
            }
        }

        // Check for letter prefix (e.g., "tt1254207" or "tt1254207:1:2")
        val prefix = id.takeWhile { it.isLetter() }
        if (prefix.isNotEmpty()) {
            return prefix
        }

        // No recognizable prefix
        return ""
    }

    /**
     * Get streams from a specific addon
     */
    suspend fun getStreamsFromAddon(
        manifestUrl: String,
        type: String,
        id: String,
    ): Result<List<AddonStream>> =
        runCatching {
            val addon =
                repository.getAddonByUrl(manifestUrl)
                    ?: throw AddonException("Addon not found: $manifestUrl")

            if (!addon.isEnabled) {
                throw AddonException("Addon is disabled: ${addon.name}")
            }

            addonClient.getStreams(manifestUrl, type, id).getOrThrow()
        }

    /**
     * Convert addon stream to scraped item for use in the app
     */
    fun streamToScrapedItem(
        streamResult: AddonStreamResult,
        contentName: String,
    ): com.github.livingwithhippos.nullseek.core.data.ScrapedItem {
        val stream = streamResult.stream
        val magnets = mutableListOf<String>()
        val torrents = mutableListOf<String>()
        val hosting = mutableListOf<String>()

        // Handle different stream types
        when {
            !stream.url.isNullOrBlank() -> {
                if (stream.url.startsWith("magnet:", ignoreCase = true)) {
                    magnets.add(stream.url)
                } else {
                    // Direct URL stream
                    hosting.add(stream.url)
                }
            }

            !stream.infoHash.isNullOrBlank() -> {
                // Torrent stream
                val magnetLink = buildMagnetLink(stream.infoHash, stream.sources)
                magnets.add(magnetLink)
            }

            !stream.ytId.isNullOrBlank() -> {
                // YouTube stream
                hosting.add("https://www.youtube.com/watch?v=${stream.ytId}")
            }
        }

        val rawDetails =
            stream.description?.trim()?.takeIf { it.isNotEmpty() }
                ?: stream.title?.trim()?.takeIf { it.isNotEmpty() }
        val fileName =
            stream.behaviorHints
                ?.filename
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

        val resolvedName =
            when {
                rawDetails != null && fileName != null && !rawDetails.contains(fileName, ignoreCase = true) -> {
                    "$fileName\n$rawDetails"
                }

                rawDetails != null -> {
                    rawDetails
                }

                fileName != null -> {
                    fileName
                }

                !stream.name.isNullOrBlank() -> {
                    stream.name.trim()
                }

                else -> {
                    contentName
                }
            }

        val combinedText = listOfNotNull(stream.title, stream.description, stream.name).joinToString("\n")
        val resolvedSizeBytes =
            stream.behaviorHints?.videoSize?.takeIf { it > 0L }
                ?: parseSizeBytesFromText(combinedText)
        val parsedSeeders = parseSeedersFromText(combinedText)
        val parsedLeechers = parseLeechersFromText(combinedText)

        return com.github.livingwithhippos.nullseek.core.data.ScrapedItem(
            name = resolvedName,
            link = hosting.firstOrNull(),
            seeders = parsedSeeders?.toString(),
            leechers = parsedLeechers?.toString(),
            size = resolvedSizeBytes?.let { formatBytes(it) },
            addedDate = null,
            parsedSize = resolvedSizeBytes?.toDouble(),
            magnets = magnets,
            torrents = torrents,
            hosting = hosting,
        )
    }

    /**
     * Extract concise metadata tags (quality, debrid status, source indexer, codec) from an AddonStream
     * without making additional network requests.
     */
    fun extractStreamCategories(streamResult: AddonStreamResult): List<String> {
        val stream = streamResult.stream
        val tags = mutableListOf<String>()

        stream.name
            ?.lineSequence()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() && !it.equals(streamResult.addonName, ignoreCase = true) }
            ?.forEach { line ->
                val cleaned =
                    if (line.startsWith(streamResult.addonName, ignoreCase = true)) {
                        line.substring(streamResult.addonName.length).trim(' ', '-', ':', '|')
                    } else {
                        line
                    }
                if (cleaned.isNotEmpty() && cleaned.length <= 32) {
                    tags.add(cleaned)
                }
            }

        val combinedText = listOfNotNull(stream.title, stream.description).joinToString("\n")
        SOURCE_INDEXER_REGEX.find(combinedText)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { source ->
            val cleanSource = source.trimEnd('.', ',', '|', '-', ' ')
            if (cleanSource.isNotEmpty() && cleanSource.length <= 28) {
                tags.add(cleanSource)
            }
        }

        return tags.distinct()
    }

    internal fun parseSeedersFromText(text: String): Int? =
        SEEDERS_REGEX
            .find(text)
            ?.groupValues
            ?.drop(1)
            ?.firstOrNull { it.isNotEmpty() }
            ?.toIntOrNull()

    internal fun parseLeechersFromText(text: String): Int? =
        LEECHERS_REGEX
            .find(text)
            ?.groupValues
            ?.drop(1)
            ?.firstOrNull { it.isNotEmpty() }
            ?.toIntOrNull()

    internal fun parseSizeBytesFromText(text: String): Long? {
        val match = SIZE_REGEX.find(text) ?: return null
        val amount = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val unit = match.groupValues[2].uppercase()
        val multiplier =
            when (unit) {
                "TB", "TIB" -> 1024.0 * 1024.0 * 1024.0 * 1024.0
                "GB", "GIB" -> 1024.0 * 1024.0 * 1024.0
                "MB", "MIB" -> 1024.0 * 1024.0
                "KB", "KIB" -> 1024.0
                else -> return null
            }
        return (amount * multiplier).toLong().takeIf { it > 0L }
    }

    internal fun buildMagnetLink(
        infoHash: String,
        trackers: List<String>?,
    ): String {
        val magnetUri = StringBuilder("magnet:?xt=urn:btih:${infoHash.trim()}")
        trackers
            ?.mapNotNull { source ->
                val trimmed = source.trim()
                when {
                    trimmed.startsWith("dht:", ignoreCase = true) -> {
                        null
                    }

                    trimmed.startsWith("tracker:", ignoreCase = true) -> {
                        trimmed.substring("tracker:".length).takeIf { it.isNotBlank() }
                    }

                    trimmed.isNotBlank() -> {
                        trimmed
                    }

                    else -> {
                        null
                    }
                }
            }?.distinct()
            ?.forEach { tracker ->
                magnetUri.append("&tr=").append(tracker)
            }
        return magnetUri.toString()
    }

    private fun formatBytes(bytes: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            bytes >= gb -> String.format("%.2f GB", bytes.toDouble() / gb)
            bytes >= mb -> String.format("%.2f MB", bytes.toDouble() / mb)
            bytes >= kb -> String.format("%.2f KB", bytes.toDouble() / kb)
            else -> "$bytes B"
        }
    }

    private companion object {
        val SEEDERS_REGEX =
            Regex("""(?:👤|🌱)\s*(\d+)|\b(?:seeds?|seeders?)\s*[:=]\s*(\d+)|\bS\s*:\s*(\d+)\b""", RegexOption.IGNORE_CASE)
        val LEECHERS_REGEX =
            Regex("""\b(?:peers?|leechers?)\s*[:=]\s*(\d+)|\bL\s*:\s*(\d+)\b""", RegexOption.IGNORE_CASE)
        val SIZE_REGEX =
            Regex("""(?:💾\s*)?(\d+(?:[.,]\d+)?)\s*(TB|TiB|GB|GiB|MB|MiB|KB|KiB)\b""", RegexOption.IGNORE_CASE)
        val SOURCE_INDEXER_REGEX =
            Regex("""(?:⚙️|📡)\s*([^\n\r👤💾⚙️📡]+)""")
    }
}

/**
 * Stream result with addon metadata
 */
data class AddonStreamResult(
    val stream: AddonStream,
    val addonName: String,
    val addonId: String,
)
