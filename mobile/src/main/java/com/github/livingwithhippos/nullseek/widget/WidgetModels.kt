package com.github.livingwithhippos.nullseek.widget

import kotlinx.serialization.Serializable

const val EXTRA_WIDGET_DESTINATION = "extra_widget_destination"
const val WIDGET_DEST_NEW_DOWNLOAD = "new_download"
const val WIDGET_DEST_SEARCH = "search"
const val WIDGET_DEST_LISTS = "lists"
const val WIDGET_DEST_SETTINGS = "settings"

@Serializable
enum class WidgetDownloadType {
    TORRENT,
    WEB,
    USENET,
}

@Serializable
data class WidgetDownloadItem(
    val id: Int,
    val name: String,
    val type: WidgetDownloadType,
    val progress: Double,
    val downloadSpeedBytes: Long,
    val sizeBytes: Long,
    val isFinished: Boolean,
    val isCached: Boolean,
    val statusLabel: String,
    val updatedAt: String = "",
)

@Serializable
data class DownloadsWidgetSnapshot(
    val items: List<WidgetDownloadItem> = emptyList(),
    val activeCount: Int = 0,
    val hasProvider: Boolean = true,
    val lastUpdatedEpochMillis: Long = 0L,
    val error: String? = null,
)
