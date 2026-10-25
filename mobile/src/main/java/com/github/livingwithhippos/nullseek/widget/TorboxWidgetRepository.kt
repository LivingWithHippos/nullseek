package com.github.livingwithhippos.nullseek.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.updateAll
import com.github.livingwithhippos.nullseek.billing.EntitlementCache
import com.github.livingwithhippos.nullseek.core.data.repository.NullProviderRepository
import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.model.TorrentDto
import com.github.livingwithhippos.torbox.model.UsenetDownloadDto
import com.github.livingwithhippos.torbox.model.WebDownloadDto
import com.github.livingwithhippos.torbox.network.ApiResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import logcat.asLog
import logcat.logcat

class TorboxWidgetRepository(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val providerRepository: NullProviderRepository,
    private val torboxClient: TorboxClient,
    private val entitlementCache: EntitlementCache,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    suspend fun getSnapshot(): DownloadsWidgetSnapshot {
        val raw =
            dataStore.data
                .map { prefs -> prefs[KEY_DOWNLOADS_SNAPSHOT] }
                .first()
        if (raw.isNullOrBlank()) {
            return DownloadsWidgetSnapshot()
        }
        return runCatching {
            json.decodeFromString<DownloadsWidgetSnapshot>(raw)
        }.getOrElse {
            DownloadsWidgetSnapshot()
        }
    }

    suspend fun refreshWidgetData(): DownloadsWidgetSnapshot {
        if (!entitlementCache.isUnlocked()) {
            updateAllWidgets(context)
            return getSnapshot()
        }

        val activeProvider = providerRepository.getActive()
        if (activeProvider == null) {
            val noProviderSnapshot =
                DownloadsWidgetSnapshot(
                    items = emptyList(),
                    activeCount = 0,
                    hasProvider = false,
                    lastUpdatedEpochMillis = System.currentTimeMillis(),
                )
            saveSnapshot(noProviderSnapshot)
            updateAllWidgets(context)
            return noProviderSnapshot
        }

        return try {
            torboxClient.updateBearerToken(activeProvider.token)
            val snapshot =
                coroutineScope {
                    val torrentsDeferred = async { torboxClient.torrent.list(offset = 0, limit = FETCH_LIMIT) }
                    val webDeferred = async { torboxClient.web.list(offset = 0, limit = FETCH_LIMIT) }
                    val usenetDeferred = async { torboxClient.usenet.list(offset = 0, limit = FETCH_LIMIT) }

                    val torrentsResult = torrentsDeferred.await()
                    val webResult = webDeferred.await()
                    val usenetResult = usenetDeferred.await()

                    val torrentItems =
                        (torrentsResult as? ApiResult.Success)
                            ?.value
                            ?.data
                            .orEmpty()
                            .map { it.toWidgetItem() }

                    val webItems =
                        (webResult as? ApiResult.Success)
                            ?.value
                            ?.data
                            .orEmpty()
                            .map { it.toWidgetItem() }

                    val usenetItems =
                        (usenetResult as? ApiResult.Success)
                            ?.value
                            ?.data
                            .orEmpty()
                            .map { it.toWidgetItem() }

                    val combined = torrentItems + webItems + usenetItems
                    val sorted =
                        combined.sortedWith(
                            compareBy<WidgetDownloadItem> { it.isFinished }
                                .thenByDescending { it.updatedAt }
                                .thenByDescending { it.id },
                        )

                    val activeCount = combined.count { !it.isFinished }
                    val anySuccess =
                        torrentsResult is ApiResult.Success ||
                            webResult is ApiResult.Success ||
                            usenetResult is ApiResult.Success

                    DownloadsWidgetSnapshot(
                        items = sorted.take(MAX_WIDGET_ITEMS),
                        activeCount = activeCount,
                        hasProvider = true,
                        lastUpdatedEpochMillis = System.currentTimeMillis(),
                        error = if (anySuccess) null else "Unable to reach TorBox",
                    )
                }

            saveSnapshot(snapshot)
            updateAllWidgets(context)
            snapshot
        } catch (e: Exception) {
            logcat("TorboxWidgetRepository") { "Error refreshing widget data: ${e.asLog()}" }
            val fallback =
                getSnapshot().copy(
                    hasProvider = true,
                    error = e.localizedMessage ?: "Failed to refresh",
                )
            saveSnapshot(fallback)
            updateAllWidgets(context)
            fallback
        }
    }

    suspend fun saveSnapshot(snapshot: DownloadsWidgetSnapshot) {
        val serialized = json.encodeToString(snapshot)
        dataStore.edit { prefs ->
            prefs[KEY_DOWNLOADS_SNAPSHOT] = serialized
        }
    }

    companion object {
        private const val FETCH_LIMIT = 10
        private const val MAX_WIDGET_ITEMS = 8
        private val KEY_DOWNLOADS_SNAPSHOT = stringPreferencesKey("widget_downloads_snapshot_v1")

        suspend fun updateAllWidgets(context: Context) {
            runCatching {
                TorboxDownloadsWidget().updateAll(context)
                TorboxQuickActionsWidget().updateAll(context)
            }.onFailure {
                logcat("TorboxWidgetRepository") { "Failed to update widgets: ${it.asLog()}" }
            }
        }
    }
}

private fun normalizeProgressFraction(rawProgress: Double): Double =
    when {
        rawProgress > 1.0 -> (rawProgress / 100.0).coerceIn(0.0, 1.0)
        else -> rawProgress.coerceIn(0.0, 1.0)
    }

private fun TorrentDto.toWidgetItem(): WidgetDownloadItem {
    val fraction = normalizeProgressFraction(progress)
    val finished = downloadFinished || fraction >= 1.0
    val status =
        when {
            finished -> "Ready"
            cached -> "Cached"
            downloadState.isNotBlank() -> downloadState.replaceFirstChar { it.uppercase() }
            else -> "${(fraction * 100).toInt()}%"
        }
    return WidgetDownloadItem(
        id = id,
        name = name,
        type = WidgetDownloadType.TORRENT,
        progress = fraction,
        downloadSpeedBytes = downloadSpeed,
        sizeBytes = size,
        isFinished = finished,
        isCached = cached,
        statusLabel = status,
        updatedAt = updatedAt,
    )
}

private fun WebDownloadDto.toWidgetItem(): WidgetDownloadItem {
    val fraction = normalizeProgressFraction(progress)
    val finished = downloadFinished || fraction >= 1.0
    val status =
        when {
            error != null -> "Error"
            finished -> "Ready"
            cached -> "Cached"
            downloadState.isNotBlank() -> downloadState.replaceFirstChar { it.uppercase() }
            else -> "${(fraction * 100).toInt()}%"
        }
    return WidgetDownloadItem(
        id = id,
        name = name,
        type = WidgetDownloadType.WEB,
        progress = fraction,
        downloadSpeedBytes = downloadSpeed,
        sizeBytes = size,
        isFinished = finished,
        isCached = cached,
        statusLabel = status,
        updatedAt = updatedAt,
    )
}

private fun UsenetDownloadDto.toWidgetItem(): WidgetDownloadItem {
    val fraction = normalizeProgressFraction(progress)
    val finished = downloadFinished || fraction >= 1.0
    val status =
        when {
            finished -> "Ready"
            cached -> "Cached"
            downloadState.isNotBlank() -> downloadState.replaceFirstChar { it.uppercase() }
            else -> "${(fraction * 100).toInt()}%"
        }
    return WidgetDownloadItem(
        id = id,
        name = name,
        type = WidgetDownloadType.USENET,
        progress = fraction,
        downloadSpeedBytes = downloadSpeed,
        sizeBytes = size,
        isFinished = finished,
        isCached = cached,
        statusLabel = status,
        updatedAt = updatedAt,
    )
}
