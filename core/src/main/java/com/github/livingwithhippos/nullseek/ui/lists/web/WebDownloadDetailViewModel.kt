package com.github.livingwithhippos.nullseek.ui.lists.web

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.domain.DebridCapabilities
import com.github.livingwithhippos.nullseek.core.domain.DebridDownload
import com.github.livingwithhippos.nullseek.core.domain.DebridResult
import com.github.livingwithhippos.nullseek.core.domain.DebridServiceProvider
import com.github.livingwithhippos.nullseek.core.domain.describe
import com.github.livingwithhippos.nullseek.data.model.ExternalService
import com.github.livingwithhippos.nullseek.data.model.ExternalServiceCategory
import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import com.github.livingwithhippos.nullseek.data.repository.ExternalServicesRepository
import com.github.livingwithhippos.nullseek.data.repository.PreferencesRepository
import com.github.livingwithhippos.nullseek.ui.lists.PLAYLIST_MIME_TYPE
import com.github.livingwithhippos.nullseek.ui.lists.UnlockedFile
import com.github.livingwithhippos.nullseek.ui.lists.buildM3u8Playlist
import com.github.livingwithhippos.nullseek.ui.lists.mediaFiles
import com.github.livingwithhippos.nullseek.ui.lists.notUnlockedIn
import com.github.livingwithhippos.nullseek.ui.lists.playlistFileName
import com.github.livingwithhippos.nullseek.ui.lists.unlockOneByOne
import com.github.livingwithhippos.nullseek.ui.lists.unlockSummary
import com.github.livingwithhippos.nullseek.utils.FileTypeCategory
import com.github.livingwithhippos.nullseek.utils.detectFileTypeCategory
import com.github.livingwithhippos.nullseek.utils.isStreamableOrSubtitle
import com.github.livingwithhippos.nullseek.utils.saveFileToPublicDownloads
import com.github.livingwithhippos.nullseek.utils.setClipboardText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

data class DownloadLinkResult(
    val url: String,
    val title: String,
    // multi-file downloads are always zipped by the server, whatever was requested
    val isZipped: Boolean = false,
    // set only for direct (non-zipped) links
    val fileType: FileTypeCategory? = null,
    // a page that streams the file in the browser (Real-Debrid)
    val streamingPageUrl: String? = null,
    // the link includes the API token and must not be shared (TorBox)
    val containsToken: Boolean = true,
)

data class PendingDownload(
    val fileId: Int? = null,
    val fileName: String? = null,
)

sealed interface WebDownloadDetailEvent {
    data class ShowMessage(
        val message: String,
    ) : WebDownloadDetailEvent

    /** a playlist was saved, [uri] is set when it can be opened by another app */
    data class PlaylistSaved(
        val message: String,
        val uri: Uri?,
    ) : WebDownloadDetailEvent

    data object Deleted : WebDownloadDetailEvent
}

data class WebDownloadDetailUiState(
    val webDownload: DebridDownload? = null,
    val accountId: Long? = null,
    val isRefreshing: Boolean = false,
    val isDownloading: Boolean = false,
    val isDeleting: Boolean = false,
    val downloadLinkResult: DownloadLinkResult? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val pendingDownload: PendingDownload? = null,
    /** files unlocked one by one, each with its own link */
    val unlockedFiles: List<UnlockedFile> = emptyList(),
    /** done and total files while unlocking, null when idle */
    val unlockProgress: Pair<Int, Int>? = null,
)

@KoinViewModel
class WebDownloadDetailViewModel(
    private val debridProvider: DebridServiceProvider,
    private val preferencesRepository: PreferencesRepository,
    private val externalServicesRepository: ExternalServicesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WebDownloadDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WebDownloadDetailEvent>()
    val events = _events.asSharedFlow()

    private val service get() = debridProvider.current

    val capabilities: StateFlow<DebridCapabilities> =
        debridProvider.active
            .map { it?.capabilities ?: debridProvider.capabilities }
            .stateIn(viewModelScope, SharingStarted.Eagerly, debridProvider.capabilities)

    private fun isCurrentAccount(accountId: Long?): Boolean =
        accountId ==
            debridProvider.active.value
                ?.provider
                ?.id

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

    fun init(item: DebridDownload) {
        if (_uiState.value.webDownload?.id != item.id) {
            _uiState.value =
                WebDownloadDetailUiState(
                    webDownload = item,
                    accountId =
                        debridProvider.active.value
                            ?.provider
                            ?.id,
                )
        }
    }

    fun refreshWebDownload() {
        val current = _uiState.value.webDownload ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            val response = service.web(current.id)
            if (response is DebridResult.Ok) {
                val updated = response.value
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
            if (!service.capabilities.zipDownloads) {
                requestDownloadLink(fileId = fileId, fileName = fileName, zipLink = false)
                return@launch
            }
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

        val accountId = _uiState.value.accountId
        val zip = zipLink && service.capabilities.zipDownloads
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true)
            val response = service.webDownloadLink(download = item, fileId = fileId, zip = zip)
            _uiState.value = _uiState.value.copy(isDownloading = false)
            if (!isCurrentAccount(accountId)) return@launch

            if (response is DebridResult.Ok) {
                val link = response.value
                val url = link.url
                val baseName = fileName ?: item.name
                val files = item.files
                val isZipped = zip || (fileId == null && files.size > 1 && service.capabilities.zipDownloads)
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
                            DownloadLinkResult(
                                url = url,
                                title = title,
                                isZipped = isZipped,
                                fileType = fileType,
                                streamingPageUrl = link.streamingPageUrl,
                                containsToken = service.capabilities.linksContainToken,
                            ),
                    )
            } else if (response is DebridResult.Err) {
                _events.emit(WebDownloadDetailEvent.ShowMessage(response.error.describe("Failed to generate download link")))
            }
        }
    }

    /** Asks a direct link for every video and audio file, instead of a zip of the whole download. */
    fun unlockAllMedia() {
        val item = _uiState.value.webDownload ?: return
        if (!item.cached || _uiState.value.unlockProgress != null) return
        val accountId = _uiState.value.accountId
        val targets = item.files.mediaFiles().notUnlockedIn(_uiState.value.unlockedFiles)
        if (targets.isEmpty()) {
            viewModelScope.launch { _events.emit(WebDownloadDetailEvent.ShowMessage("These files are already unlocked")) }
            return
        }
        viewModelScope.launch {
            val outcome =
                unlockOneByOne(
                    targets = targets,
                    existing = _uiState.value.unlockedFiles,
                    fetch = { file -> service.webDownloadLink(download = item, fileId = file.id, zip = false) },
                    onUpdate = { files, progress ->
                        _uiState.value = _uiState.value.copy(unlockedFiles = files, unlockProgress = progress)
                    },
                    isStillCurrent = { isCurrentAccount(accountId) && _uiState.value.webDownload?.id == item.id },
                )
            if (outcome == null) {
                _uiState.value = _uiState.value.copy(unlockProgress = null)
                return@launch
            }
            _events.emit(WebDownloadDetailEvent.ShowMessage(unlockSummary(outcome.unlocked, outcome.failures)))
        }
    }

    fun clearUnlockedFiles() {
        if (_uiState.value.unlockProgress != null) return
        _uiState.value = _uiState.value.copy(unlockedFiles = emptyList())
    }

    /** Saves an M3U8 playlist with the unlocked links in the Downloads folder. */
    fun savePlaylist(context: Context) {
        val item = _uiState.value.webDownload ?: return
        val files = _uiState.value.unlockedFiles
        if (files.none { !it.failed }) return
        viewModelScope.launch {
            val sortByName = preferencesRepository.playlistSortByName.first()
            val playlist = buildM3u8Playlist(files, sortByName)
            val saved =
                withContext(Dispatchers.IO) {
                    saveFileToPublicDownloads(
                        context = context,
                        fileName = playlistFileName(item.name),
                        bytes = playlist.toByteArray(Charsets.UTF_8),
                        mimeType = PLAYLIST_MIME_TYPE,
                        extension = "m3u8",
                    )
                }
            _events.emit(
                if (saved != null) {
                    // only content uris can be shared with other apps
                    WebDownloadDetailEvent.PlaylistSaved(
                        message = context.getString(R.string.playlist_saved),
                        uri = saved.takeIf { it.scheme == "content" },
                    )
                } else {
                    WebDownloadDetailEvent.ShowMessage(context.getString(R.string.playlist_save_failed))
                },
            )
        }
    }

    /** Copies the links of the unlocked files, one per line. */
    fun copyUnlockedLinks(context: Context) {
        val links =
            _uiState.value.unlockedFiles
                .filter { !it.failed }
                .map { it.url }
        if (links.isEmpty()) return
        setClipboardText(context, links.joinToString("\n"))
        viewModelScope.launch { _events.emit(WebDownloadDetailEvent.ShowMessage("Copied ${links.size} links to the clipboard")) }
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
            val response = service.deleteWeb(item.id)
            if (response is DebridResult.Ok) {
                _events.emit(WebDownloadDetailEvent.ShowMessage("Web download deleted"))
                _events.emit(WebDownloadDetailEvent.Deleted)
            } else {
                _uiState.value = _uiState.value.copy(isDeleting = false)
                _events.emit(WebDownloadDetailEvent.ShowMessage("Failed to delete web download"))
            }
        }
    }
}
