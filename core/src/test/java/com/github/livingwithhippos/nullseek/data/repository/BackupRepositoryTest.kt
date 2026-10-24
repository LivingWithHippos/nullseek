package com.github.livingwithhippos.nullseek.data.repository

import com.github.livingwithhippos.nullseek.core.data.repository.AppBackup
import com.github.livingwithhippos.nullseek.core.data.repository.CURRENT_BACKUP_VERSION
import com.github.livingwithhippos.nullseek.core.data.repository.ExternalServiceBackup
import com.github.livingwithhippos.nullseek.core.data.repository.IndexerBackup
import com.github.livingwithhippos.nullseek.core.data.repository.PreferencesBackup
import com.github.livingwithhippos.nullseek.core.data.repository.ProviderBackup
import com.github.livingwithhippos.nullseek.core.data.repository.StremioAddonBackup
import com.github.livingwithhippos.nullseek.core.data.repository.decodeBackupFromJson
import com.github.livingwithhippos.nullseek.core.data.repository.encodeBackupToJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupRepositoryTest {
    @Test
    fun `encode and decode AppBackup round-trips all preferences and entities`() {
        val original =
            AppBackup(
                version = CURRENT_BACKUP_VERSION,
                createdAt = 1_700_000_000_000L,
                preferences =
                    PreferencesBackup(
                        downloadZipMode = DownloadZipMode.ZIP_NON_MEDIA.name,
                        pagingLoadSize = 500,
                        includeReferral = true,
                        searchAutoAddDownload = true,
                        searchSortOption = "SEEDERS",
                        searchSortAscending = false,
                        searchMode = "STREMIO_ADDONS",
                        stremioShowFilterBar = true,
                        stremioSortOption = "SIZE",
                        stremioSortAscending = false,
                        searchHistory = listOf("ubuntu", "debian"),
                        themeMode = ThemeMode.DARK.name,
                        themeColor = ThemeColor.TEAL.name,
                        externalDisclaimerAccepted = true,
                    ),
                providers =
                    listOf(
                        ProviderBackup(
                            id = 1L,
                            type = 1,
                            name = "Main TorBox",
                            token = "secret-token",
                            active = true,
                        ),
                    ),
                externalServices =
                    listOf(
                        ExternalServiceBackup(
                            id = 2L,
                            name = "Living Room Kodi",
                            type = "KODI",
                            category = "MEDIA_PLAYER",
                            address = "192.168.1.50:8080",
                            username = "kodi",
                            password = "pass",
                            isDefault = true,
                        ),
                    ),
                indexers =
                    listOf(
                        IndexerBackup(
                            id = 3L,
                            name = "Home Prowlarr",
                            type = "PROWLARR",
                            address = "http://192.168.1.60:9696",
                            apiToken = "indexer-key",
                            active = true,
                        ),
                    ),
                stremioAddons =
                    listOf(
                        StremioAddonBackup(
                            manifestUrl = "https://example.com/manifest.json",
                            id = "org.example.addon",
                            name = "Example Addon",
                            description = "Test addon",
                            version = "1.0.0",
                            resources = "[\"stream\"]",
                            types = "[\"movie\",\"series\"]",
                            logo = null,
                            background = null,
                            isEnabled = true,
                            addedTimestamp = 1_700_000_000_000L,
                            customName = "Custom Addon",
                        ),
                    ),
            )

        val json = encodeBackupToJson(original)
        val restored = decodeBackupFromJson(json)

        assertEquals(original, restored)
    }

    @Test
    fun `decodeBackupFromJson rejects unsupported future version`() {
        val invalidVersionJson = """{"version": 999}"""
        assertThrows(IllegalArgumentException::class.java) {
            decodeBackupFromJson(invalidVersionJson)
        }
    }
}
