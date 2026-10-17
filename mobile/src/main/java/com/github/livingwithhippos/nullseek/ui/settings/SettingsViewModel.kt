package com.github.livingwithhippos.nullseek.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.data.repository.DEFAULT_PAGING_LOAD_SIZE
import com.github.livingwithhippos.nullseek.data.repository.DownloadPreferencesRepository
import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SettingsViewModel(
    private val downloadPreferencesRepository: DownloadPreferencesRepository,
) : ViewModel() {
    val downloadZipMode: StateFlow<DownloadZipMode> =
        downloadPreferencesRepository.downloadZipMode.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DownloadZipMode.ASK,
        )

    val pagingLoadSize: StateFlow<Int> =
        downloadPreferencesRepository.pagingLoadSize.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DEFAULT_PAGING_LOAD_SIZE,
        )

    fun setDownloadZipMode(mode: DownloadZipMode) {
        viewModelScope.launch {
            downloadPreferencesRepository.setDownloadZipMode(mode)
        }
    }

    fun setPagingLoadSize(size: Int) {
        viewModelScope.launch {
            downloadPreferencesRepository.setPagingLoadSize(size)
        }
    }
}
