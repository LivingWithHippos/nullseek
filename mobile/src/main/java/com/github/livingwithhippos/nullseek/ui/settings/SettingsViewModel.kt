package com.github.livingwithhippos.nullseek.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.data.repository.DEFAULT_PAGING_LOAD_SIZE
import com.github.livingwithhippos.nullseek.data.repository.PreferencesRepository
import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
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

    fun setSearchAutoAddDownload(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setSearchAutoAddDownload(enabled) }
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
}
