package com.github.livingwithhippos.nullseek.ui.lists.web

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.core.data.repository.NullProviderRepository
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
)

@KoinViewModel
class WebDownloadDetailViewModel(
    private val client: TorboxClient,
    private val providerRepository: NullProviderRepository,
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

    fun requestDownloadLink(
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
            _uiState.value = _uiState.value.copy(isDownloading = true)
            val activeProvider = providerRepository.getActive()
            val token = activeProvider?.token
            if (token.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(isDownloading = false)
                _events.emit(WebDownloadDetailEvent.ShowMessage("No active provider token found"))
                return@launch
            }

            val isZip = fileId == null
            val response =
                client.web.downloadLink(
                    token = token,
                    webId = item.id,
                    fileId = fileId,
                    zipLink = if (isZip) true else null,
                )
            _uiState.value = _uiState.value.copy(isDownloading = false)

            if (response is ApiResult.Success && response.value.data != null) {
                val url = response.value.data!!
                val rawTitle = fileName ?: if (isZip) "${item.name}.zip" else item.name
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
