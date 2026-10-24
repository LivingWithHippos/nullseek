package com.github.livingwithhippos.nullseek.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

const val REFERRAL_CODE = "dba208d7-f53e-4711-96db-97c7051799bc"
const val TORBOX_BASE_SETTINGS_URL = "https://torbox.app/settings"
const val TORBOX_SUBSCRIPTION_URL = "https://torbox.app/subscription"

@KoinViewModel
class UserViewModel(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    val includeReferralPreference: StateFlow<Boolean?> =
        preferencesRepository.includeReferral.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    fun setReferralPreference(include: Boolean) {
        viewModelScope.launch { preferencesRepository.setIncludeReferral(include) }
    }

    fun resetReferralPreference() {
        viewModelScope.launch { preferencesRepository.resetIncludeReferral() }
    }

    fun getTorboxSettingsUrl(): String = TORBOX_BASE_SETTINGS_URL

    fun buildTorboxSubscriptionUrl(includeReferral: Boolean): String =
        if (includeReferral) {
            "$TORBOX_SUBSCRIPTION_URL?referral=$REFERRAL_CODE"
        } else {
            TORBOX_SUBSCRIPTION_URL
        }
}
