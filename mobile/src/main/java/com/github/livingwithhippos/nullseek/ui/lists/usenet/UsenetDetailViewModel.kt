package com.github.livingwithhippos.nullseek.ui.lists.usenet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.core.data.repository.NullProviderRepository
import com.github.livingwithhippos.nullseek.data.model.ExternalService
import com.github.livingwithhippos.nullseek.data.model.ExternalServiceCategory
import com.github.livingwithhippos.nullseek.data.repository.DownloadPreferencesRepository
import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import com.github.livingwithhippos.nullseek.data.repository.ExternalServicesRepository
import com.github.livingwithhippos.nullseek.utils.isStreamableOrSubtitle
import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.model.UsenetDownloadDto
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
)

data class PendingDownload(
    val fileId: Int? = null,
    val fileName: String? = null,
)

sealed interface UsenetDetailEvent {
    data class ShowMessage(
        val message: String,
    ) : UsenetDetailEvent

    data object Deleted : UsenetDetailEvent
}

data class UsenetDetailUiState(
    val usenet: UsenetDownloadDto? = null,
    val isRefreshing: Boolean = false,
    val isDownloading: Boolean = false,
    val isDeleting: Boolean = false,
    val downloadLinkResult: DownloadLinkResult? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val pendingDownload: PendingDownload? = null,
)

@KoinViewModel
class UsenetDetailViewModel(
    private val client: TorboxClient,
    private val providerRepository: NullProviderRepository,
    private val downloadPreferencesRepository: DownloadPreferencesRepository,
    private val externalServicesRepository: ExternalServicesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UsenetDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UsenetDetailEvent>()
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

    fun playOnMedia(
        service: ExternalService,
        url: String,
        setAsDefault: Boolean = false,
    ) {
        viewModelScope.launch {
            if (setAsDefault) {
                externalServicesRepository.setDefault(service.id, ExternalServiceCategory.MEDIA_PLAYER)
            }
            _events.emit(UsenetDetailEvent.ShowMessage("Sending to ${service.name}…"))
            val result = externalServicesRepository.playMedia(service, url)
            result.fold(
                onSuccess = {
                    _events.emit(UsenetDetailEvent.ShowMessage("Playing on ${service.name}"))
                },
                onFailure = { error ->
                    _events.emit(UsenetDetailEvent.ShowMessage("Failed to play on ${service.name}: ${error.message}"))
                },
            )
        }
    }

    fun init(item: UsenetDownloadDto) {
        if (_uiState.value.usenet?.id != item.id) {
            _uiState.value =
                UsenetDetailUiState(
                    usenet = item,
                )
        }
    }

    fun refreshUsenet() {
        val current = _uiState.value.usenet ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            val response = client.usenet.get(bypassCache = true, id = current.id)
            if (response is ApiResult.Success && response.value.data != null) {
                val updated = response.value.data!!
                _uiState.value =
                    _uiState.value.copy(
                        usenet = updated,
                        isRefreshing = false,
                    )
                _events.emit(UsenetDetailEvent.ShowMessage("Usenet download info refreshed"))
            } else {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
                _events.emit(UsenetDetailEvent.ShowMessage("Failed to refresh usenet info"))
            }
        }
    }

    fun startDownload(
        fileId: Int? = null,
        fileName: String? = null,
    ) {
        val item = _uiState.value.usenet ?: return
        if (!item.cached) {
            viewModelScope.launch {
                _events.emit(UsenetDetailEvent.ShowMessage("Downloads are only available for cached usenet downloads"))
            }
            return
        }

        viewModelScope.launch {
            when (val mode = downloadPreferencesRepository.getDownloadZipMode()) {
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
                downloadPreferencesRepository.setDownloadZipMode(mode)
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
                    _uiState.value.usenet
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

    fun requestDownloadLink(
        fileId: Int? = null,
        fileName: String? = null,
        zipLink: Boolean = true,
    ) {
        val item = _uiState.value.usenet ?: return
        if (!item.cached) {
            viewModelScope.launch {
                _events.emit(UsenetDetailEvent.ShowMessage("Downloads are only available for cached usenet downloads"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true)
            val activeProvider = providerRepository.getActive()
            val token = activeProvider?.token
            if (token.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(isDownloading = false)
                _events.emit(UsenetDetailEvent.ShowMessage("No active provider token found"))
                return@launch
            }

            val response =
                client.usenet.downloadLink(
                    token = token,
                    usenetId = item.id,
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
                _events.emit(UsenetDetailEvent.ShowMessage("Failed to generate download link"))
            }
        }
    }

    fun dismissDownloadLink() {
        _uiState.value = _uiState.value.copy(downloadLinkResult = null)
    }

    fun showDeleteConfirmDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = show)
    }

    fun deleteUsenet() {
        val item = _uiState.value.usenet ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, showDeleteConfirmDialog = false)
            val response = client.usenet.control(usenetId = item.id, operation = "Delete")
            _uiState.value = _uiState.value.copy(isDeleting = false)
            if (response is ApiResult.Success) {
                _events.emit(UsenetDetailEvent.ShowMessage("Usenet download deleted"))
                _events.emit(UsenetDetailEvent.Deleted)
            } else {
                _events.emit(UsenetDetailEvent.ShowMessage("Failed to delete usenet download"))
            }
        }
    }
}
