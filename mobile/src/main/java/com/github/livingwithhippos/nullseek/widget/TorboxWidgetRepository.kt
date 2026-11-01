package com.github.livingwithhippos.nullseek.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.updateAll
import com.github.livingwithhippos.nullseek.billing.EntitlementCache
import com.github.livingwithhippos.nullseek.core.data.repository.NullProviderRepository
import com.github.livingwithhippos.nullseek.core.domain.DebridDownload
import com.github.livingwithhippos.nullseek.core.domain.DebridResult
import com.github.livingwithhippos.nullseek.core.domain.DebridServiceProvider
import com.github.livingwithhippos.nullseek.core.domain.DebridTorrent
import com.github.livingwithhippos.nullseek.core.domain.DownloadStatus
import com.github.livingwithhippos.nullseek.core.domain.ProviderKind
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import logcat.asLog
import logcat.logcat

class TorboxWidgetRepository(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val providerRepository: NullProviderRepository,
    private val debridProvider: DebridServiceProvider,
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

        val kind = ProviderKind.fromDbType(activeProvider.type)
        return try {
            // a separate client, the worker must not change the token of the app one
            val service = debridProvider.newTemp(kind, activeProvider.token)
            val capabilities = service.capabilities
            val snapshot =
                coroutineScope {
                    val torrentsDeferred = async { service.listTorrents(offset = 0, limit = FETCH_LIMIT, bypassCache = true) }
                    val webDeferred =
                        async {
                            if (capabilities.webDownloads) service.listWeb(offset = 0, limit = FETCH_LIMIT, bypassCache = true) else null
                        }
                    val usenetDeferred =
                        async {
                            if (capabilities.usenet) service.listUsenet(offset = 0, limit = FETCH_LIMIT, bypassCache = true) else null
                        }

                    val torrentsResult = torrentsDeferred.await()
                    val webResult = webDeferred.await()
                    val usenetResult = usenetDeferred.await()

                    val torrentItems =
                        (torrentsResult as? DebridResult.Ok)
                            ?.value
                            ?.items
                            .orEmpty()
                            .map { it.toWidgetItem() }

                    val webItems =
                        (webResult as? DebridResult.Ok)
                            ?.value
                            ?.items
                            .orEmpty()
                            .map { it.toWidgetItem(WidgetDownloadType.WEB) }

                    val usenetItems =
                        (usenetResult as? DebridResult.Ok)
                            ?.value
                            ?.items
                            .orEmpty()
                            .map { it.toWidgetItem(WidgetDownloadType.USENET) }

                    val combined = torrentItems + webItems + usenetItems
                    val sorted =
                        combined.sortedWith(
                            compareBy<WidgetDownloadItem> { it.isFinished }
                                .thenByDescending { it.updatedAt }
                                .thenByDescending { it.id },
                        )

                    val activeCount = combined.count { !it.isFinished }
                    val anySuccess =
                        torrentsResult is DebridResult.Ok ||
                            webResult is DebridResult.Ok ||
                            usenetResult is DebridResult.Ok

                    DownloadsWidgetSnapshot(
                        items = sorted.take(MAX_WIDGET_ITEMS),
                        activeCount = activeCount,
                        hasProvider = true,
                        lastUpdatedEpochMillis = System.currentTimeMillis(),
                        error = if (anySuccess) null else "Unable to reach ${kind.displayName}",
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
        private val KEY_DOWNLOADS_SNAPSHOT = stringPreferencesKey("widget_downloads_snapshot_v2")

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

private fun DebridTorrent.toWidgetItem(): WidgetDownloadItem {
    val fraction = normalizeProgressFraction(progress)
    val finished = downloadFinished || fraction >= 1.0
    val status =
        when {
            status == DownloadStatus.AWAITING_FILE_SELECTION -> "Pick files"
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
        updatedAt = updatedAt ?: createdAt,
    )
}

private fun DebridDownload.toWidgetItem(type: WidgetDownloadType): WidgetDownloadItem {
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
        type = type,
        progress = fraction,
        downloadSpeedBytes = downloadSpeed,
        sizeBytes = size,
        isFinished = finished,
        isCached = cached,
        statusLabel = status,
        updatedAt = updatedAt ?: createdAt,
    )
}
