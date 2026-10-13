package com.github.livingwithhippos.nullseek.ui.lists.web

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.core.data.repository.NullProviderRepository
import com.github.livingwithhippos.nullseek.data.repository.DownloadPreferencesRepository
import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.model.WebDownloadDto
import com.github.livingwithhippos.torbox.network.ApiResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

data class DownloadLinkResult(
    val url: String,
    val title: String,
)

data class PendingDownload(
    val fileId: Int? = null,
    val fileName: String? = null,
)

sealed interface WebDownloadDetailEvent {
    data class ShowMessage(
        val message: String,
    ) : WebDownloadDetailEvent

    data object Deleted : WebDownloadDetailEvent
}

data class WebDownloadDetailUiState(
    val webDownload: WebDownloadDto? = null,
    val isRefreshing: Boolean = false,
    val isDownloading: Boolean = false,
    val isDeleting: Boolean = false,
    val downloadLinkResult: DownloadLinkResult? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val pendingDownload: PendingDownload? = null,
)

@KoinViewModel
class WebDownloadDetailViewModel(
    private val client: TorboxClient,
    private val providerRepository: NullProviderRepository,
    private val downloadPreferencesRepository: DownloadPreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WebDownloadDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WebDownloadDetailEvent>()
    val events = _events.asSharedFlow()

    fun init(item: WebDownloadDto) {
        if (_uiState.value.webDownload?.id != item.id) {
            _uiState.value =
                WebDownloadDetailUiState(
                    webDownload = item,
                )
        }
    }

    fun refreshWebDownload() {
        val current = _uiState.value.webDownload ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            val response = client.web.get(bypassCache = true, id = current.id)
            if (response is ApiResult.Success && response.value.data != null) {
                val updated = response.value.data!!
                _uiState.value =
                    _uiState.value.copy(
                        webDownload = updated,
                        isRefreshing = false,
                    )
                _events.emit(WebDownloadDetailEvent.ShowMessage("Web download info refreshed"))
            } else {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
                _events.emit(WebDownloadDetailEvent.ShowMessage("Failed to refresh web download info"))
            }
        }
    }

    fun startDownload(
        fileId: Int? = null,
        fileName: String? = null,
    ) {
        val item = _uiState.value.webDownload ?: return
        if (!item.cached) {
            viewModelScope.launch {
                _events.emit(WebDownloadDetailEvent.ShowMessage("Downloads are only available for cached web downloads"))
            }
            return
        }

        viewModelScope.launch {
            when (downloadPreferencesRepository.getDownloadZipMode()) {
                DownloadZipMode.ASK -> {
                    _uiState.value =
                        _uiState.value.copy(
                            pendingDownload = PendingDownload(fileId = fileId, fileName = fileName),
                        )
                }
                DownloadZipMode.ZIP -> {
                    requestDownloadLink(fileId = fileId, fileName = fileName, zipLink = true)
                }
                DownloadZipMode.UNCOMPRESSED -> {
                    requestDownloadLink(fileId = fileId, fileName = fileName, zipLink = false)
                }
            }
        }
    }

    fun confirmDownloadModal(zip: Boolean, rememberChoice: Boolean) {
        val pending = _uiState.value.pendingDownload ?: return
        _uiState.value = _uiState.value.copy(pendingDownload = null)
        viewModelScope.launch {
            if (rememberChoice) {
                val mode = if (zip) DownloadZipMode.ZIP else DownloadZipMode.UNCOMPRESSED
                downloadPreferencesRepository.setDownloadZipMode(mode)
            }
            requestDownloadLink(fileId = pending.fileId, fileName = pending.fileName, zipLink = zip)
        }
    }

    fun dismissDownloadModal() {
        _uiState.value = _uiState.value.copy(pendingDownload = null)
    }

    fun requestDownloadLink(
        fileId: Int? = null,
        fileName: String? = null,
        zipLink: Boolean = true,
    ) {
        val item = _uiState.value.webDownload ?: return
        if (!item.cached) {
            viewModelScope.launch {
                _events.emit(WebDownloadDetailEvent.ShowMessage("Downloads are only available for cached web downloads"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true)
            val activeProvider = providerRepository.getActive()
            val token = activeProvider?.token
            if (token.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(isDownloading = false)
                _events.emit(WebDownloadDetailEvent.ShowMessage("No active provider token found"))
                return@launch
            }

            val response =
                client.web.downloadLink(
                    token = token,
                    webId = item.id,
                    fileId = fileId,
                    zipLink = zipLink,
                )
            _uiState.value = _uiState.value.copy(isDownloading = false)

            if (response is ApiResult.Success && response.value.data != null) {
                val url = response.value.data!!
                val baseName = fileName ?: item.name
                val rawTitle =
                    if (zipLink) {
                        if (baseName.endsWith(".zip", ignoreCase = true)) baseName else "$baseName.zip"
                    } else {
                        baseName
                    }
                val title = rawTitle.substringAfterLast('/').substringAfterLast('\\')
                _uiState.value =
                    _uiState.value.copy(
                        downloadLinkResult = DownloadLinkResult(url = url, title = title),
                    )
            } else {
                _events.emit(WebDownloadDetailEvent.ShowMessage("Failed to generate download link"))
            }
        }
    }

    fun dismissDownloadLink() {
        _uiState.value = _uiState.value.copy(downloadLinkResult = null)
    }

    fun showDeleteConfirmDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = show)
    }

    fun deleteWebDownload() {
        val item = _uiState.value.webDownload ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, showDeleteConfirmDialog = false)
            val response = client.web.control(webId = item.id, operation = "Delete")
            _uiState.value = _uiState.value.copy(isDeleting = false)
            if (response is ApiResult.Success) {
                _events.emit(WebDownloadDetailEvent.ShowMessage("Web download deleted"))
                _events.emit(WebDownloadDetailEvent.Deleted)
            } else {
                _events.emit(WebDownloadDetailEvent.ShowMessage("Failed to delete web download"))
            }
        }
    }
}
