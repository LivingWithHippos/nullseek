package com.github.livingwithhippos.nullseek.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.core.data.repository.BackupRepository
import com.github.livingwithhippos.nullseek.core.domain.DebridCapabilities
import com.github.livingwithhippos.nullseek.core.domain.DebridResult
import com.github.livingwithhippos.nullseek.core.domain.DebridServiceProvider
import com.github.livingwithhippos.nullseek.core.domain.describe
import com.github.livingwithhippos.nullseek.data.repository.PreferencesRepository
import com.github.livingwithhippos.torbox.model.UserStatsDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

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
    private val debridProvider: DebridServiceProvider,
) : ViewModel() {
    val capabilities: StateFlow<DebridCapabilities> =
        debridProvider.active
            .map { it?.capabilities ?: debridProvider.capabilities }
            .stateIn(viewModelScope, SharingStarted.Eagerly, debridProvider.capabilities)

    private val _detailedStats = MutableStateFlow<DetailedStatsState>(DetailedStatsState.Idle)
    val detailedStats: StateFlow<DetailedStatsState> = _detailedStats.asStateFlow()

    fun loadDetailedStats() {
        if (_detailedStats.value is DetailedStatsState.Loading) return
        if (!debridProvider.capabilities.detailedStats) return
        _detailedStats.value = DetailedStatsState.Loading
        viewModelScope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    debridProvider.current.stats()
                }
            _detailedStats.value =
                when (result) {
                    is DebridResult.Ok -> DetailedStatsState.Loaded(result.value)
                    is DebridResult.Err -> DetailedStatsState.Error(result.error.describe("Stats"))
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

    fun getAccountSettingsUrl(): String = debridProvider.capabilities.accountUrl

    fun buildSubscriptionUrl(includeReferral: Boolean): String {
        val capabilities = debridProvider.capabilities
        return capabilities.subscriptionReferralUrl?.takeIf { includeReferral } ?: capabilities.subscriptionUrl
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
