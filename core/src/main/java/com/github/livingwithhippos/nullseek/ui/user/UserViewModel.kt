package com.github.livingwithhippos.nullseek.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.core.data.repository.BackupRepository
import com.github.livingwithhippos.nullseek.data.repository.PreferencesRepository
import com.github.livingwithhippos.nullseek.data.repository.describe
import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.model.UserStatsDto
import com.github.livingwithhippos.torbox.network.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

const val REFERRAL_CODE = "dba208d7-f53e-4711-96db-97c7051799bc"
const val TORBOX_BASE_SETTINGS_URL = "https://torbox.app/settings"
const val TORBOX_SUBSCRIPTION_URL = "https://torbox.app/subscription"

// how user/stats groups the bandwidth history (TorBox keeps the last 30 days)
private const val STATS_BANDWIDTH_GROUPING = "month"

/** Detailed stats from user/stats, loaded on demand from the statistics card. */
sealed interface DetailedStatsState {
    data object Idle : DetailedStatsState

    data object Loading : DetailedStatsState

    data class Loaded(
        val stats: UserStatsDto,
    ) : DetailedStatsState

    data class Error(
        val message: String,
    ) : DetailedStatsState
}

@KoinViewModel
class UserViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val backupRepository: BackupRepository,
    private val client: TorboxClient,
) : ViewModel() {
    private val _detailedStats = MutableStateFlow<DetailedStatsState>(DetailedStatsState.Idle)
    val detailedStats: StateFlow<DetailedStatsState> = _detailedStats.asStateFlow()

    fun loadDetailedStats() {
        if (_detailedStats.value is DetailedStatsState.Loading) return
        _detailedStats.value = DetailedStatsState.Loading
        viewModelScope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    client.user.stats(general = true, bandwidth = true, bandwidthGrouping = STATS_BANDWIDTH_GROUPING)
                }
            _detailedStats.value =
                when (result) {
                    is ApiResult.Success -> {
                        result.value.data?.let { DetailedStatsState.Loaded(it) }
                            ?: DetailedStatsState.Error(result.value.detail)
                    }

                    is ApiResult.Failure -> {
                        DetailedStatsState.Error(result.describe("Stats"))
                    }
                }
        }
    }

    val includeReferralPreference: StateFlow<Boolean?> =
        preferencesRepository.includeReferral.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    private val _isRestoringBackup = MutableStateFlow(false)
    val isRestoringBackup: StateFlow<Boolean> = _isRestoringBackup.asStateFlow()

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

    fun restoreBackup(
        rawJson: String,
        onResult: (Boolean) -> Unit,
    ) {
        if (_isRestoringBackup.value) return
        viewModelScope.launch {
            _isRestoringBackup.value = true
            val success = backupRepository.restoreFromBackupJson(rawJson).isSuccess
            _isRestoringBackup.value = false
            onResult(success)
        }
    }
}
