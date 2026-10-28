package com.github.livingwithhippos.nullseek.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.livingwithhippos.nullseek.core.data.database.dao.ExternalServiceDao
import com.github.livingwithhippos.nullseek.core.data.database.dao.IndexerDao
import com.github.livingwithhippos.nullseek.core.data.database.dao.NullProviderDao
import com.github.livingwithhippos.nullseek.core.data.database.dao.StremioAddonDao
import com.github.livingwithhippos.nullseek.core.data.database.model.ExternalServiceEntity
import com.github.livingwithhippos.nullseek.core.data.database.model.IndexerEntity
import com.github.livingwithhippos.nullseek.core.data.database.model.NullProvider
import com.github.livingwithhippos.nullseek.core.data.database.model.StremioAddonEntity
import com.github.livingwithhippos.torbox.client.TorboxClient
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val CURRENT_BACKUP_VERSION = 1
private const val MAX_BACKUP_SEARCH_HISTORY = 20

private val backupJson =
    Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

private val historyJson =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

@Serializable
data class AppBackup(
    val version: Int = CURRENT_BACKUP_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val preferences: PreferencesBackup = PreferencesBackup(),
    val providers: List<ProviderBackup> = emptyList(),
    val externalServices: List<ExternalServiceBackup> = emptyList(),
    val indexers: List<IndexerBackup> = emptyList(),
    val stremioAddons: List<StremioAddonBackup> = emptyList(),
)

@Serializable
data class PreferencesBackup(
    val downloadZipMode: String? = null,
    val pagingLoadSize: Int? = null,
    val includeReferral: Boolean? = null,
    val searchAutoAddDownload: Boolean? = null,
    val searchSortOption: String? = null,
    val searchSortAscending: Boolean? = null,
    val searchMode: String? = null,
    val stremioShowFilterBar: Boolean? = null,
    val stremioSortOption: String? = null,
    val stremioSortAscending: Boolean? = null,
    val searchHistory: List<String> = emptyList(),
    val themeMode: String? = null,
    val themeColor: String? = null,
    val useStandardFont: Boolean? = null,
    val externalDisclaimerAccepted: Boolean? = null,
)

@Serializable
data class ProviderBackup(
    val id: Long = 0,
    val type: Int,
    val name: String,
    val token: String,
    val active: Boolean = false,
) {
    fun toEntity(): NullProvider =
        NullProvider(
            id = id,
            type = type,
            name = name,
            token = token,
            active = active,
        )

    companion object {
        fun fromEntity(entity: NullProvider): ProviderBackup =
            ProviderBackup(
                id = entity.id,
                type = entity.type,
                name = entity.name,
                token = entity.token,
                active = entity.active,
            )
    }
}

@Serializable
data class ExternalServiceBackup(
    val id: Long = 0,
    val name: String,
    val type: String,
    val category: String,
    val address: String,
    val username: String = "",
    val password: String = "",
    val isDefault: Boolean = false,
) {
    fun toEntity(): ExternalServiceEntity =
        ExternalServiceEntity(
            id = id,
            name = name,
            type = type,
            category = category,
            address = address,
            username = username,
            password = password,
            isDefault = isDefault,
        )

    companion object {
        fun fromEntity(entity: ExternalServiceEntity): ExternalServiceBackup =
            ExternalServiceBackup(
                id = entity.id,
                name = entity.name,
                type = entity.type,
                category = entity.category,
                address = entity.address,
                username = entity.username,
                password = entity.password,
                isDefault = entity.isDefault,
            )
    }
}

@Serializable
data class IndexerBackup(
    val id: Long = 0,
    val name: String,
    val type: String,
    val address: String,
    val apiToken: String = "",
    val active: Boolean = true,
) {
    fun toEntity(): IndexerEntity =
        IndexerEntity(
            id = id,
            name = name,
            type = type,
            address = address,
            apiToken = apiToken,
            active = active,
        )

    companion object {
        fun fromEntity(entity: IndexerEntity): IndexerBackup =
            IndexerBackup(
                id = entity.id,
                name = entity.name,
                type = entity.type,
                address = entity.address,
                apiToken = entity.apiToken,
                active = entity.active,
            )
    }
}

@Serializable
data class StremioAddonBackup(
    val manifestUrl: String,
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val resources: String,
    val types: String,
    val logo: String? = null,
    val background: String? = null,
    val isEnabled: Boolean = true,
    val addedTimestamp: Long = 0L,
    val customName: String? = null,
) {
    fun toEntity(): StremioAddonEntity =
        StremioAddonEntity(
            manifestUrl = manifestUrl,
            id = id,
            name = name,
            description = description,
            version = version,
            resources = resources,
            types = types,
            logo = logo,
            background = background,
            isEnabled = isEnabled,
            addedTimestamp = addedTimestamp,
            customName = customName,
        )

    companion object {
        fun fromEntity(entity: StremioAddonEntity): StremioAddonBackup =
            StremioAddonBackup(
                manifestUrl = entity.manifestUrl,
                id = entity.id,
                name = entity.name,
                description = entity.description,
                version = entity.version,
                resources = entity.resources,
                types = entity.types,
                logo = entity.logo,
                background = entity.background,
                isEnabled = entity.isEnabled,
                addedTimestamp = entity.addedTimestamp,
                customName = entity.customName,
            )
    }
}

fun encodeBackupToJson(backup: AppBackup): String = backupJson.encodeToString(backup)

fun decodeBackupFromJson(rawJson: String): AppBackup {
    val parsed = backupJson.decodeFromString<AppBackup>(rawJson)
    require(parsed.version in 1..CURRENT_BACKUP_VERSION) {
        "Unsupported backup version: ${parsed.version}"
    }
    return parsed
}

interface BackupRepository {
    suspend fun createBackupJson(): Result<String>

    suspend fun restoreFromBackupJson(rawJson: String): Result<Unit>
}

class BackupRepositoryImpl(
    private val providerDao: NullProviderDao,
    private val externalServiceDao: ExternalServiceDao,
    private val indexerDao: IndexerDao,
    private val stremioAddonDao: StremioAddonDao,
    private val dataStore: DataStore<Preferences>,
    private val torboxClient: TorboxClient,
) : BackupRepository {
    private val zipModeKey = stringPreferencesKey("download_zip_mode")
    private val pagingLoadSizeKey = intPreferencesKey("paging_load_size")
    private val referralKey = booleanPreferencesKey("include_referral_in_browser")
    private val searchAutoAddDownloadKey = booleanPreferencesKey("search_auto_add_download")
    private val searchSortOptionKey = stringPreferencesKey("search_sort_option")
    private val searchSortAscendingKey = booleanPreferencesKey("search_sort_ascending")
    private val searchModeKey = stringPreferencesKey("search_mode")
    private val stremioShowFilterBarKey = booleanPreferencesKey("stremio_show_filter_bar")
    private val stremioSortOptionKey = stringPreferencesKey("stremio_sort_option")
    private val stremioSortAscendingKey = booleanPreferencesKey("stremio_sort_ascending")
    private val searchHistoryKey = stringPreferencesKey("search_history")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val themeColorKey = stringPreferencesKey("theme_color")
    private val useStandardFontKey = booleanPreferencesKey("use_standard_font")
    private val externalDisclaimerAcceptedKey = booleanPreferencesKey("external_disclaimer_accepted")

    override suspend fun createBackupJson(): Result<String> =
        runCatching {
            val preferences = exportPreferences()
            val providers = providerDao.getAll().map { ProviderBackup.fromEntity(it) }
            val externalServices =
                externalServiceDao.getAll().map { ExternalServiceBackup.fromEntity(it) }
            val indexers = indexerDao.getAll().map { IndexerBackup.fromEntity(it) }
            val stremioAddons =
                stremioAddonDao.getAll().map { StremioAddonBackup.fromEntity(it) }

            val backup =
                AppBackup(
                    version = CURRENT_BACKUP_VERSION,
                    createdAt = System.currentTimeMillis(),
                    preferences = preferences,
                    providers = providers,
                    externalServices = externalServices,
                    indexers = indexers,
                    stremioAddons = stremioAddons,
                )
            encodeBackupToJson(backup)
        }

    override suspend fun restoreFromBackupJson(rawJson: String): Result<Unit> =
        runCatching {
            val backup = decodeBackupFromJson(rawJson)

            // Normalize provider active flags so at most one provider is active
            val activeIndex =
                backup.providers
                    .indexOfFirst { it.active }
                    .let { idx ->
                        if (idx >= 0) {
                            idx
                        } else if (backup.providers.isNotEmpty()) {
                            0
                        } else {
                            -1
                        }
                    }

            providerDao.deleteAll()
            backup.providers.forEachIndexed { index, item ->
                providerDao.insert(
                    item.copy(id = 0, active = index == activeIndex).toEntity(),
                )
            }

            externalServiceDao.deleteAll()
            backup.externalServices.forEach { item ->
                externalServiceDao.insert(item.copy(id = 0).toEntity())
            }

            indexerDao.deleteAll()
            backup.indexers.forEach { item ->
                indexerDao.insert(item.copy(id = 0).toEntity())
            }

            stremioAddonDao.deleteAllAddons()
            backup.stremioAddons.forEach { item ->
                stremioAddonDao.insertAddon(item.toEntity())
            }

            restorePreferences(backup.preferences)

            val activeProvider = providerDao.getActive()
            torboxClient.updateBearerToken(activeProvider?.token.orEmpty())
        }

    private suspend fun exportPreferences(): PreferencesBackup {
        val preferences = dataStore.data.first()
        val history =
            preferences[searchHistoryKey]?.let { raw ->
                runCatching { historyJson.decodeFromString<List<String>>(raw) }
                    .getOrDefault(emptyList())
            } ?: emptyList()
        return PreferencesBackup(
            downloadZipMode = preferences[zipModeKey],
            pagingLoadSize = preferences[pagingLoadSizeKey],
            includeReferral = preferences[referralKey],
            searchAutoAddDownload = preferences[searchAutoAddDownloadKey],
            searchSortOption = preferences[searchSortOptionKey],
            searchSortAscending = preferences[searchSortAscendingKey],
            searchMode = preferences[searchModeKey],
            stremioShowFilterBar = preferences[stremioShowFilterBarKey],
            stremioSortOption = preferences[stremioSortOptionKey],
            stremioSortAscending = preferences[stremioSortAscendingKey],
            searchHistory = history,
            themeMode = preferences[themeModeKey],
            themeColor = preferences[themeColorKey],
            useStandardFont = preferences[useStandardFontKey],
            externalDisclaimerAccepted = preferences[externalDisclaimerAcceptedKey],
        )
    }

    private suspend fun restorePreferences(backup: PreferencesBackup) {
        dataStore.edit { preferences ->
            preferences.clear()
            backup.downloadZipMode?.let { preferences[zipModeKey] = it }
            backup.pagingLoadSize?.takeIf { it > 0 }?.let { preferences[pagingLoadSizeKey] = it }
            backup.includeReferral?.let { preferences[referralKey] = it }
            backup.searchAutoAddDownload?.let { preferences[searchAutoAddDownloadKey] = it }
            backup.searchSortOption?.let { preferences[searchSortOptionKey] = it }
            backup.searchSortAscending?.let { preferences[searchSortAscendingKey] = it }
            backup.searchMode?.let { preferences[searchModeKey] = it }
            backup.stremioShowFilterBar?.let { preferences[stremioShowFilterBarKey] = it }
            backup.stremioSortOption?.let { preferences[stremioSortOptionKey] = it }
            backup.stremioSortAscending?.let { preferences[stremioSortAscendingKey] = it }
            if (backup.searchHistory.isNotEmpty()) {
                preferences[searchHistoryKey] =
                    historyJson.encodeToString(backup.searchHistory.take(MAX_BACKUP_SEARCH_HISTORY))
            }
            backup.themeMode?.let { preferences[themeModeKey] = it }
            backup.themeColor?.let { preferences[themeColorKey] = it }
            backup.useStandardFont?.let { preferences[useStandardFontKey] = it }
            backup.externalDisclaimerAccepted?.let { preferences[externalDisclaimerAcceptedKey] = it }
        }
    }
}
