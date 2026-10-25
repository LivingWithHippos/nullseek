package com.github.livingwithhippos.nullseek.widget

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetModelsTest {
    @Test
    fun `DownloadsWidgetSnapshot round-trips through JSON`() {
        val snapshot =
            DownloadsWidgetSnapshot(
                items =
                    listOf(
                        WidgetDownloadItem(
                            id = 42,
                            name = "Linux ISO",
                            type = WidgetDownloadType.TORRENT,
                            progress = 0.65,
                            downloadSpeedBytes = 1024L,
                            sizeBytes = 4_294_967_296L,
                            isFinished = false,
                            isCached = false,
                            statusLabel = "Downloading",
                            updatedAt = "2026-10-25T12:00:00Z",
                        ),
                    ),
                activeCount = 1,
                hasProvider = true,
                lastUpdatedEpochMillis = 123456789L,
                error = null,
            )

        val encoded = Json.encodeToString(snapshot)
        val decoded = Json.decodeFromString<DownloadsWidgetSnapshot>(encoded)

        assertEquals(snapshot, decoded)
    }
}
