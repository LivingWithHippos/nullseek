package com.github.livingwithhippos.nullseek.ui.user

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

const val REFERRAL_CODE = "dba208d7-f53e-4711-96db-97c7051799bc"
const val TORBOX_BASE_SETTINGS_URL = "https://torbox.app/settings"
const val TORBOX_SUBSCRIPTION_URL = "https://torbox.app/subscription"

@KoinViewModel
class UserViewModel(
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {
    private val referralKey = booleanPreferencesKey("include_referral_in_browser")

    val includeReferralPreference: StateFlow<Boolean?> =
        dataStore.data
            .map { preferences -> preferences[referralKey] }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null,
            )

    fun setReferralPreference(include: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[referralKey] = include
            }
        }
    }

    fun resetReferralPreference() {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences.remove(referralKey)
            }
        }
    }

    fun getTorboxSettingsUrl(): String = TORBOX_BASE_SETTINGS_URL

    fun buildTorboxSubscriptionUrl(includeReferral: Boolean): String =
        if (includeReferral) {
            "$TORBOX_SUBSCRIPTION_URL?referral=$REFERRAL_CODE"
        } else {
            TORBOX_SUBSCRIPTION_URL
        }
}
