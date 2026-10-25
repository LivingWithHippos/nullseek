package com.github.livingwithhippos.nullseek.billing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.github.livingwithhippos.nullseek.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class EntitlementCache(
    private val dataStore: DataStore<Preferences>,
) {
    val isUnlockedFlow: Flow<Boolean> =
        if (BuildConfig.IS_FOSS) {
            flowOf(true)
        } else {
            dataStore.data.map { prefs ->
                prefs[KEY_PREMIUM_WIDGETS_UNLOCKED] ?: false
            }
        }

    suspend fun isUnlocked(): Boolean =
        if (BuildConfig.IS_FOSS) {
            true
        } else {
            isUnlockedFlow.first()
        }

    suspend fun setUnlocked(unlocked: Boolean) {
        if (BuildConfig.IS_FOSS) return
        dataStore.edit { prefs ->
            prefs[KEY_PREMIUM_WIDGETS_UNLOCKED] = unlocked
        }
    }

    companion object {
        private val KEY_PREMIUM_WIDGETS_UNLOCKED =
            booleanPreferencesKey("premium_widgets_unlocked")
    }
}
