package com.github.livingwithhippos.nullseek.ui.lists.torrent

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.core.data.repository.NullProviderRepository
import com.github.livingwithhippos.nullseek.data.repository.DownloadPreferencesRepository
import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import com.github.livingwithhippos.nullseek.utils.saveFileToPublicDownloads
import com.github.livingwithhippos.nullseek.utils.setClipboardText
import com.github.livingwithhippos.torbox.client.TorboxClient
import com.github.livingwithhippos.torbox.model.ExportResultDto
import com.github.livingwithhippos.torbox.model.TorrentDto
import com.github.livingwithhippos.torbox.model.TorrentInfoDto
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

sealed interface TorrentDetailEvent {
    data class ShowMessage(
        val message: String,
    ) : TorrentDetailEvent
}

data class TorrentDetailUiState(
    val torrent: TorrentDto? = null,
    val isRefreshing: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadLinkResult: DownloadLinkResult? = null,
    val isExporting: Boolean = false,
    val isLoadingFullInfo: Boolean = false,
    val fullInfo: TorrentInfoDto? = null,
    val isFullInfoExpanded: Boolean = false,
    val showExportDialog: Boolean = false,
    val pendingDownload: PendingDownload? = null,
)

@KoinViewModel
class TorrentDetailViewModel(
    private val client: TorboxClient,
    private val providerRepository: NullProviderRepository,
    private val downloadPreferencesRepository: DownloadPreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TorrentDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TorrentDetailEvent>()
    val events = _events.asSharedFlow()

    fun init(item: TorrentDto) {
        if (_uiState.value.torrent?.id != item.id) {
            _uiState.value =
                TorrentDetailUiState(
                    torrent = item,
                )
        }
    }

    fun refreshTorrent() {
        val current = _uiState.value.torrent ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            val response = client.torrent.get(bypassCache = true, id = current.id)
            if (response is ApiResult.Success && response.value.data != null) {
                val updated = response.value.data!!
                _uiState.value =
                    _uiState.value.copy(
                        torrent = updated,
                        isRefreshing = false,
                    )
                _events.emit(TorrentDetailEvent.ShowMessage("Torrent info refreshed"))
            } else {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
                _events.emit(TorrentDetailEvent.ShowMessage("Failed to refresh torrent info"))
            }
        }
    }

    fun startDownload(
        fileId: Int? = null,
        fileName: String? = null,
    ) {
        val torrent = _uiState.value.torrent ?: return
        if (!torrent.cached) {
            viewModelScope.launch {
                _events.emit(TorrentDetailEvent.ShowMessage("Download is only available for cached torrents"))
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
        val torrent = _uiState.value.torrent ?: return
        if (!torrent.cached) {
            viewModelScope.launch {
                _events.emit(TorrentDetailEvent.ShowMessage("Download is only available for cached torrents"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true)
            val activeProvider = providerRepository.getActive()
            val token = activeProvider?.token
            if (token.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(isDownloading = false)
                _events.emit(TorrentDetailEvent.ShowMessage("No active provider token found"))
                return@launch
            }

            val response =
                client.torrent.downloadLink(
                    token = token,
                    torrentId = torrent.id,
                    fileId = fileId,
                    zipLink = zipLink,
                )
            _uiState.value = _uiState.value.copy(isDownloading = false)

            if (response is ApiResult.Success && response.value.data != null) {
                val url = response.value.data!!
                val baseName = fileName ?: torrent.name
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
                _events.emit(TorrentDetailEvent.ShowMessage("Failed to generate download link"))
            }
        }
    }

    fun dismissDownloadLink() {
        _uiState.value = _uiState.value.copy(downloadLinkResult = null)
    }

    fun showExportDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showExportDialog = show)
    }

    fun exportMagnet(context: Context) {
        val torrent = _uiState.value.torrent ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, showExportDialog = false)
            val response = client.torrent.export(torrentId = torrent.id, type = "magnet")
            _uiState.value = _uiState.value.copy(isExporting = false)

            if (response is ApiResult.Success) {
                when (val result = response.value) {
                    is ExportResultDto.Magnet -> {
                        setClipboardText(context, result.magnet)
                        _events.emit(TorrentDetailEvent.ShowMessage("Magnet link copied to clipboard"))
                    }

                    is ExportResultDto.Torrent -> {
                        _events.emit(TorrentDetailEvent.ShowMessage("Unexpected export response"))
                    }
                }
            } else {
                _events.emit(TorrentDetailEvent.ShowMessage("Export failed"))
            }
        }
    }

    fun exportTorrentFile(context: Context) {
        val torrent = _uiState.value.torrent ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, showExportDialog = false)
            val response = client.torrent.export(torrentId = torrent.id, type = "file")
            _uiState.value = _uiState.value.copy(isExporting = false)

            if (response is ApiResult.Success) {
                when (val result = response.value) {
                    is ExportResultDto.Torrent -> {
                        val fileName =
                            if (torrent.name.endsWith(".torrent", ignoreCase = true)) {
                                torrent.name
                            } else {
                                "${torrent.name}.torrent"
                            }
                        val savedUri = saveFileToPublicDownloads(context = context, fileName = fileName, bytes = result.bytes)
                        if (savedUri != null) {
                            _events.emit(TorrentDetailEvent.ShowMessage("Torrent saved to Downloads"))
                        } else {
                            _events.emit(TorrentDetailEvent.ShowMessage("Failed to save torrent to Downloads"))
                        }
                    }

                    is ExportResultDto.Magnet -> {
                        _events.emit(TorrentDetailEvent.ShowMessage("Unexpected export response"))
                    }
                }
            } else {
                _events.emit(TorrentDetailEvent.ShowMessage("Export failed"))
            }
        }
    }

    fun loadFullInfo() {
        val torrent = _uiState.value.torrent ?: return
        if (_uiState.value.fullInfo != null) {
            _uiState.value =
                _uiState.value.copy(
                    isFullInfoExpanded = !_uiState.value.isFullInfoExpanded,
                )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingFullInfo = true)
            val response = client.torrent.fullInfo(hash = torrent.hash, cacheLookup = true)
            if (response is ApiResult.Success && response.value.data != null) {
                _uiState.value =
                    _uiState.value.copy(
                        isLoadingFullInfo = false,
                        fullInfo = response.value.data,
                        isFullInfoExpanded = true,
                    )
            } else {
                // Fallback to info(hash)
                val fallbackResponse = client.torrent.info(hash = torrent.hash, cacheLookup = true)
                if (fallbackResponse is ApiResult.Success && fallbackResponse.value.data != null) {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoadingFullInfo = false,
                            fullInfo = fallbackResponse.value.data,
                            isFullInfoExpanded = true,
                        )
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingFullInfo = false)
                    _events.emit(TorrentDetailEvent.ShowMessage("Failed to load full info"))
                }
            }
        }
    }

    fun toggleFullInfoExpanded() {
        _uiState.value =
            _uiState.value.copy(
                isFullInfoExpanded = !_uiState.value.isFullInfoExpanded,
            )
    }
}
