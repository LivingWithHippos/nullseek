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

const val DEFAULT_PAGING_LOAD_SIZE = 1000

enum class DownloadZipMode {
    ASK,
    ZIP,
    UNCOMPRESSED,
    ZIP_NON_MEDIA,
}

class PreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    private val zipModeKey = stringPreferencesKey("download_zip_mode")
    private val pagingLoadSizeKey = intPreferencesKey("paging_load_size")

    private val referralKey = booleanPreferencesKey("include_referral_in_browser")
    private val searchAutoAddDownloadKey = booleanPreferencesKey("search_auto_add_download")

    // null means the user has not chosen yet
    val includeReferral: Flow<Boolean?> =
        dataStore.data.map { preferences -> preferences[referralKey] }

    val searchAutoAddDownload: Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[searchAutoAddDownloadKey] ?: false }

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

    suspend fun setSearchAutoAddDownload(enabled: Boolean) {
        dataStore.edit { preferences ->
            if (enabled) {
                preferences[searchAutoAddDownloadKey] = true
            } else {
                preferences.remove(searchAutoAddDownloadKey)
            }
        }
    }
}
