package com.github.livingwithhippos.nullseek.ui.lists.web

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.core.data.repository.NullProviderRepository
import com.github.livingwithhippos.nullseek.data.model.ExternalService
import com.github.livingwithhippos.nullseek.data.model.ExternalServiceCategory
import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import com.github.livingwithhippos.nullseek.data.repository.ExternalServicesRepository
import com.github.livingwithhippos.nullseek.data.repository.PreferencesRepository
import com.github.livingwithhippos.nullseek.utils.FileTypeCategory
import com.github.livingwithhippos.nullseek.utils.detectFileTypeCategory
import com.github.livingwithhippos.nullseek.utils.isStreamableOrSubtitle
import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.model.WebDownloadDto
import com.github.livingwithhippos.torbox.network.ApiResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

data class DownloadLinkResult(
    val url: String,
    val title: String,
    // multi-file downloads are always zipped by the server, whatever was requested
    val isZipped: Boolean = false,
    // set only for direct (non-zipped) links
    val fileType: FileTypeCategory? = null,
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
    private val preferencesRepository: PreferencesRepository,
    private val externalServicesRepository: ExternalServicesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WebDownloadDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WebDownloadDetailEvent>()
    val events = _events.asSharedFlow()

    val defaultMediaPlayer: StateFlow<ExternalService?> =
        externalServicesRepository.defaultMediaPlayer.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    val mediaPlayerServices: StateFlow<List<ExternalService>> =
        externalServicesRepository.mediaPlayerServices.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    // used to label the download button "Generate Zip" when the result will be a zip
    val downloadZipMode: StateFlow<DownloadZipMode> =
        preferencesRepository.downloadZipMode.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DownloadZipMode.ASK,
        )

    val filterFilesFullPath: StateFlow<Boolean> =
        preferencesRepository.filterFilesFullPath.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    fun playOnMedia(
        service: ExternalService,
        url: String,
        setAsDefault: Boolean = false,
    ) {
        viewModelScope.launch {
            if (setAsDefault) {
                externalServicesRepository.setDefault(
                    service.id,
                    ExternalServiceCategory.MEDIA_PLAYER,
                )
            }
            _events.emit(WebDownloadDetailEvent.ShowMessage("Sending to ${service.name}…"))
            val result = externalServicesRepository.playMedia(service, url)
            result.fold(
                onSuccess = {
                    _events.emit(WebDownloadDetailEvent.ShowMessage("Playing on ${service.name}"))
                },
                onFailure = { error ->
                    _events.emit(WebDownloadDetailEvent.ShowMessage("Failed to play on ${service.name}: ${error.message}"))
                },
            )
        }
    }

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
            when (val mode = preferencesRepository.getDownloadZipMode()) {
                DownloadZipMode.ASK -> {
                    _uiState.value =
                        _uiState.value.copy(
                            pendingDownload = PendingDownload(fileId = fileId, fileName = fileName),
                        )
                }

                else -> {
                    requestDownloadLink(
                        fileId = fileId,
                        fileName = fileName,
                        zipLink = resolveZipLink(mode, fileId, fileName),
                    )
                }
            }
        }
    }

    fun confirmDownloadModal(
        mode: DownloadZipMode,
        rememberChoice: Boolean,
    ) {
        val pending = _uiState.value.pendingDownload ?: return
        _uiState.value = _uiState.value.copy(pendingDownload = null)
        viewModelScope.launch {
            if (rememberChoice) {
                preferencesRepository.setDownloadZipMode(mode)
            }
            requestDownloadLink(
                fileId = pending.fileId,
                fileName = pending.fileName,
                zipLink = resolveZipLink(mode, pending.fileId, pending.fileName),
            )
        }
    }

    fun dismissDownloadModal() {
        _uiState.value = _uiState.value.copy(pendingDownload = null)
    }

    /**
     * ZIP_NON_MEDIA keeps video, audio and subtitles uncompressed (streamable / external players).
     * Whole downloads with multiple files are always zipped, since only a zip can bundle them.
     */
    private fun resolveZipLink(
        mode: DownloadZipMode,
        fileId: Int?,
        fileName: String?,
    ): Boolean =
        when (mode) {
            DownloadZipMode.ASK, DownloadZipMode.ZIP -> {
                true
            }

            DownloadZipMode.UNCOMPRESSED -> {
                false
            }

            DownloadZipMode.ZIP_NON_MEDIA -> {
                val files =
                    _uiState.value.webDownload
                        ?.files
                        .orEmpty()
                val target =
                    when {
                        fileId != null -> files.firstOrNull { it.id == fileId }
                        files.size == 1 -> files.first()
                        else -> null
                    }
                if (fileId == null && target == null) {
                    true
                } else {
                    !isStreamableOrSubtitle(target?.name ?: fileName.orEmpty(), target?.mimetype)
                }
            }
        }

    /** Asks for an uncompressed link to one file, ignoring the zip setting, so it can be streamed. */
    fun downloadDirect(
        fileId: Int,
        fileName: String,
    ) {
        requestDownloadLink(fileId = fileId, fileName = fileName, zipLink = false)
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
                val files = item.files.orEmpty()
                val isZipped = zipLink || (fileId == null && files.size > 1)
                val target =
                    when {
                        fileId != null -> files.firstOrNull { it.id == fileId }
                        files.size == 1 -> files.first()
                        else -> null
                    }
                val fileType =
                    if (isZipped) null else detectFileTypeCategory(target?.name ?: baseName, target?.mimetype)
                val rawTitle =
                    if (isZipped) {
                        if (baseName.endsWith(
                                ".zip",
                                ignoreCase = true,
                            )
                        ) {
                            baseName
                        } else {
                            "$baseName.zip"
                        }
                    } else {
                        baseName
                    }
                val title = rawTitle.substringAfterLast('/').substringAfterLast('\\')
                _uiState.value =
                    _uiState.value.copy(
                        downloadLinkResult =
                            DownloadLinkResult(url = url, title = title, isZipped = isZipped, fileType = fileType),
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
            if (response is ApiResult.Success) {
                _events.emit(WebDownloadDetailEvent.ShowMessage("Web download deleted"))
                _events.emit(WebDownloadDetailEvent.Deleted)
            } else {
                _uiState.value = _uiState.value.copy(isDeleting = false)
                _events.emit(WebDownloadDetailEvent.ShowMessage("Failed to delete web download"))
            }
        }
    }
}
