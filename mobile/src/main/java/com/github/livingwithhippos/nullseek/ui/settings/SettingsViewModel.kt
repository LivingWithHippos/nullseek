package com.github.livingwithhippos.nullseek.ui.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.core.data.repository.BackupRepository
import com.github.livingwithhippos.nullseek.data.repository.DEFAULT_PAGING_LOAD_SIZE
import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import com.github.livingwithhippos.nullseek.data.repository.PreferencesRepository
import com.github.livingwithhippos.nullseek.data.repository.ThemeColor
import com.github.livingwithhippos.nullseek.data.repository.ThemeMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

sealed interface SettingsEvent {
    data class ShowMessage(
        @StringRes val resId: Int,
    ) : SettingsEvent

    data class WriteBackupFile(
        val json: String,
    ) : SettingsEvent

    data object BackupRestored : SettingsEvent
}

@KoinViewModel
class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {
    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    private val _isBackupBusy = MutableStateFlow(false)
    val isBackupBusy: StateFlow<Boolean> = _isBackupBusy.asStateFlow()

    val downloadZipMode: StateFlow<DownloadZipMode> =
        preferencesRepository.downloadZipMode.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DownloadZipMode.ASK,
        )

    val pagingLoadSize: StateFlow<Int> =
        preferencesRepository.pagingLoadSize.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DEFAULT_PAGING_LOAD_SIZE,
        )

    val searchAutoAddDownload: StateFlow<Boolean> =
        preferencesRepository.searchAutoAddDownload.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    val filterFilesFullPath: StateFlow<Boolean> =
        preferencesRepository.filterFilesFullPath.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    val themeMode: StateFlow<ThemeMode> =
        preferencesRepository.themeMode.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM,
        )

    val themeColor: StateFlow<ThemeColor> =
        preferencesRepository.themeColor.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeColor.SYSTEM,
        )

    val externalDisclaimerAccepted: StateFlow<Boolean> =
        preferencesRepository.externalDisclaimerAccepted.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    fun acceptExternalDisclaimer() {
        viewModelScope.launch {
            preferencesRepository.setExternalDisclaimerAccepted(true)
        }
    }

    fun setSearchAutoAddDownload(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setSearchAutoAddDownload(enabled) }
    }

    fun setFilterFilesFullPath(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setFilterFilesFullPath(enabled) }
    }

    fun setDownloadZipMode(mode: DownloadZipMode) {
        viewModelScope.launch {
            preferencesRepository.setDownloadZipMode(mode)
        }
    }

    fun setPagingLoadSize(size: Int) {
        viewModelScope.launch {
            preferencesRepository.setPagingLoadSize(size)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setThemeColor(color: ThemeColor) {
        viewModelScope.launch {
            preferencesRepository.setThemeColor(color)
        }
    }

    fun exportBackup() {
        if (_isBackupBusy.value) return
        viewModelScope.launch {
            _isBackupBusy.value = true
            backupRepository
                .createBackupJson()
                .onSuccess { json ->
                    _events.emit(SettingsEvent.WriteBackupFile(json))
                }.onFailure {
                    _events.emit(SettingsEvent.ShowMessage(R.string.backup_create_failed))
                }
            _isBackupBusy.value = false
        }
    }

    fun onBackupWriteCompleted(success: Boolean) {
        viewModelScope.launch {
            _events.emit(
                SettingsEvent.ShowMessage(
                    if (success) R.string.backup_created_success else R.string.backup_create_failed,
                ),
            )
        }
    }

    fun importBackup(rawJson: String) {
        if (_isBackupBusy.value) return
        viewModelScope.launch {
            _isBackupBusy.value = true
            backupRepository
                .restoreFromBackupJson(rawJson)
                .onSuccess {
                    _events.emit(SettingsEvent.BackupRestored)
                    _events.emit(SettingsEvent.ShowMessage(R.string.backup_restored_success))
                }.onFailure {
                    _events.emit(SettingsEvent.ShowMessage(R.string.backup_restore_failed))
                }
            _isBackupBusy.value = false
        }
    }

    fun onBackupReadFailed() {
        viewModelScope.launch {
            _events.emit(SettingsEvent.ShowMessage(R.string.backup_restore_failed))
        }
    }
}
