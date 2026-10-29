package com.github.livingwithhippos.nullseek.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

val PAGING_LOAD_SIZE_PRESETS = listOf(50, 100, 500, 1000, 5000)
const val DEFAULT_PAGING_LOAD_SIZE = 100
const val MAX_SEARCH_HISTORY = 20

private val historyJson =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

internal fun updateSearchHistoryOnAdd(
    current: List<String>,
    query: String,
    maxEntries: Int = MAX_SEARCH_HISTORY,
): List<String> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return current
    val withoutDuplicate = current.filterNot { it.equals(trimmed, ignoreCase = true) }
    return (listOf(trimmed) + withoutDuplicate).take(maxEntries)
}

internal fun updateSearchHistoryOnRemove(
    current: List<String>,
    query: String,
): List<String> = current.filterNot { it == query }

enum class DownloadZipMode {
    ASK,
    ZIP,
    UNCOMPRESSED,
    ZIP_NON_MEDIA,
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED,
}

enum class ThemeColor {
    SYSTEM,
    PURPLE,
    BLUE,
    TEAL,
    GREEN,
    ORANGE,
    RED,
    TORBOX,
}

class PreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    private val zipModeKey = stringPreferencesKey("download_zip_mode")
    private val pagingLoadSizeKey = intPreferencesKey("paging_load_size")

    private val referralKey = booleanPreferencesKey("include_referral_in_browser")
    private val searchAutoAddDownloadKey = booleanPreferencesKey("search_auto_add_download")
    private val searchCheckCacheKey = booleanPreferencesKey("search_check_cache")
    private val filterFilesFullPathKey = booleanPreferencesKey("filter_files_full_path")

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

    // null means the user has not chosen yet
    val includeReferral: Flow<Boolean?> =
        dataStore.data.map { preferences -> preferences[referralKey] }

    /** Stored as the option name; unknown or missing values fall back to the default. */
    val searchSortOption: Flow<String?> =
        dataStore.data.map { preferences -> preferences[searchSortOptionKey] }

    val searchSortAscending: Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[searchSortAscendingKey] ?: false }

    /** Stored as the SearchMode name; unknown or missing values fall back to INDEXERS. */
    val searchMode: Flow<String?> =
        dataStore.data.map { preferences -> preferences[searchModeKey] }

    val stremioShowFilterBar: Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[stremioShowFilterBarKey] ?: true }

    val stremioSortOption: Flow<String?> =
        dataStore.data.map { preferences -> preferences[stremioSortOptionKey] }

    val stremioSortAscending: Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[stremioSortAscendingKey] ?: false }

    val searchHistory: Flow<List<String>> =
        dataStore.data.map { preferences ->
            val raw = preferences[searchHistoryKey] ?: return@map emptyList()
            runCatching { historyJson.decodeFromString<List<String>>(raw) }
                .getOrDefault(emptyList())
        }

    val searchAutoAddDownload: Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[searchAutoAddDownloadKey] ?: false }

    // on by default
    val searchCheckCache: Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[searchCheckCacheKey] ?: true }

    val filterFilesFullPath: Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[filterFilesFullPathKey] ?: false }

    val downloadZipMode: Flow<DownloadZipMode> =
        dataStore.data.map { preferences ->
            when (preferences[zipModeKey]) {
                DownloadZipMode.ZIP.name -> DownloadZipMode.ZIP
                DownloadZipMode.UNCOMPRESSED.name -> DownloadZipMode.UNCOMPRESSED
                DownloadZipMode.ZIP_NON_MEDIA.name -> DownloadZipMode.ZIP_NON_MEDIA
                else -> DownloadZipMode.ASK
            }
        }

    val pagingLoadSize: Flow<Int> =
        dataStore.data.map { preferences ->
            preferences[pagingLoadSizeKey] ?: DEFAULT_PAGING_LOAD_SIZE
        }

    val themeMode: Flow<ThemeMode> =
        dataStore.data.map { preferences ->
            val raw = preferences[themeModeKey]
            ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM
        }

    val themeColor: Flow<ThemeColor> =
        dataStore.data.map { preferences ->
            val raw = preferences[themeColorKey]
            ThemeColor.entries.firstOrNull { it.name == raw } ?: ThemeColor.SYSTEM
        }

    val useStandardFont: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[useStandardFontKey] ?: false
        }

    suspend fun setUseStandardFont(enabled: Boolean) {
        dataStore.edit { preferences ->
            if (enabled) {
                preferences[useStandardFontKey] = true
            } else {
                preferences.remove(useStandardFontKey)
            }
        }
    }

    val externalDisclaimerAccepted: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[externalDisclaimerAcceptedKey] ?: false
        }

    suspend fun setExternalDisclaimerAccepted(accepted: Boolean) {
        dataStore.edit { preferences ->
            if (accepted) {
                preferences[externalDisclaimerAcceptedKey] = true
            } else {
                preferences.remove(externalDisclaimerAcceptedKey)
            }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            if (mode == ThemeMode.SYSTEM) {
                preferences.remove(themeModeKey)
            } else {
                preferences[themeModeKey] = mode.name
            }
        }
    }

    suspend fun setThemeColor(color: ThemeColor) {
        dataStore.edit { preferences ->
            if (color == ThemeColor.SYSTEM) {
                preferences.remove(themeColorKey)
            } else {
                preferences[themeColorKey] = color.name
            }
        }
    }

    suspend fun getDownloadZipMode(): DownloadZipMode = downloadZipMode.first()

    suspend fun setDownloadZipMode(mode: DownloadZipMode) {
        dataStore.edit { preferences ->
            if (mode == DownloadZipMode.ASK) {
                preferences.remove(zipModeKey)
            } else {
                preferences[zipModeKey] = mode.name
            }
        }
    }

    suspend fun getPagingLoadSize(): Int = pagingLoadSize.first()

    suspend fun setPagingLoadSize(size: Int) {
        val validSize = size.coerceAtLeast(1)
        dataStore.edit { preferences ->
            if (validSize == DEFAULT_PAGING_LOAD_SIZE) {
                preferences.remove(pagingLoadSizeKey)
            } else {
                preferences[pagingLoadSizeKey] = validSize
            }
        }
    }

    suspend fun setIncludeReferral(include: Boolean) {
        dataStore.edit { preferences -> preferences[referralKey] = include }
    }

    suspend fun resetIncludeReferral() {
        dataStore.edit { preferences -> preferences.remove(referralKey) }
    }

    suspend fun setSearchCheckCache(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[searchCheckCacheKey] = enabled }
    }

    suspend fun setSearchAutoAddDownload(enabled: Boolean) {
        dataStore.edit { preferences ->
            if (enabled) {
                preferences[searchAutoAddDownloadKey] = true
            } else {
                preferences.remove(searchAutoAddDownloadKey)
            }
        }
    }

    suspend fun setFilterFilesFullPath(enabled: Boolean) {
        dataStore.edit { preferences ->
            if (enabled) {
                preferences[filterFilesFullPathKey] = true
            } else {
                preferences.remove(filterFilesFullPathKey)
            }
        }
    }

    suspend fun setSearchSortOption(name: String) {
        dataStore.edit { preferences -> preferences[searchSortOptionKey] = name }
    }

    suspend fun setSearchSortAscending(ascending: Boolean) {
        dataStore.edit { preferences -> preferences[searchSortAscendingKey] = ascending }
    }

    suspend fun setSearchMode(name: String) {
        dataStore.edit { preferences -> preferences[searchModeKey] = name }
    }

    suspend fun setStremioShowFilterBar(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[stremioShowFilterBarKey] = enabled }
    }

    suspend fun setStremioSortOption(name: String) {
        dataStore.edit { preferences -> preferences[stremioSortOptionKey] = name }
    }

    suspend fun setStremioSortAscending(ascending: Boolean) {
        dataStore.edit { preferences -> preferences[stremioSortAscendingKey] = ascending }
    }

    suspend fun addSearchHistoryQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        dataStore.edit { preferences ->
            val current =
                preferences[searchHistoryKey]?.let { raw ->
                    runCatching { historyJson.decodeFromString<List<String>>(raw) }
                        .getOrDefault(emptyList())
                } ?: emptyList()
            val updated = updateSearchHistoryOnAdd(current, trimmed)
            preferences[searchHistoryKey] = historyJson.encodeToString(updated)
        }
    }

    suspend fun removeSearchHistoryQuery(query: String) {
        dataStore.edit { preferences ->
            val current =
                preferences[searchHistoryKey]?.let { raw ->
                    runCatching { historyJson.decodeFromString<List<String>>(raw) }
                        .getOrDefault(emptyList())
                } ?: emptyList()
            val updated = updateSearchHistoryOnRemove(current, query)
            if (updated.isEmpty()) {
                preferences.remove(searchHistoryKey)
            } else {
                preferences[searchHistoryKey] = historyJson.encodeToString(updated)
            }
        }
    }

    suspend fun clearSearchHistory() {
        dataStore.edit { preferences -> preferences.remove(searchHistoryKey) }
    }
}
