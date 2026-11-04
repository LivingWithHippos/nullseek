package com.github.livingwithhippos.nullseek.ui.lists.torrent

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.nullseek.core.domain.ConversionOutcome
import com.github.livingwithhippos.nullseek.core.domain.DebridCapabilities
import com.github.livingwithhippos.nullseek.core.domain.DebridError
import com.github.livingwithhippos.nullseek.core.domain.DebridLink
import com.github.livingwithhippos.nullseek.core.domain.DebridResult
import com.github.livingwithhippos.nullseek.core.domain.DebridServiceProvider
import com.github.livingwithhippos.nullseek.core.domain.DebridTorrent
import com.github.livingwithhippos.nullseek.core.domain.ExportPayload
import com.github.livingwithhippos.nullseek.core.domain.TorrentFullInfo
import com.github.livingwithhippos.nullseek.core.domain.awaitConversion
import com.github.livingwithhippos.nullseek.core.domain.awaitSelectionApplied
import com.github.livingwithhippos.nullseek.core.domain.describe
import com.github.livingwithhippos.nullseek.core.domain.magnetFromHash
import com.github.livingwithhippos.nullseek.core.domain.reason
import com.github.livingwithhippos.nullseek.core.domain.requeuePreselection
import com.github.livingwithhippos.nullseek.core.domain.selectionMismatch
import com.github.livingwithhippos.nullseek.core.domain.selectionToRequest
import com.github.livingwithhippos.nullseek.data.model.ExternalService
import com.github.livingwithhippos.nullseek.data.model.ExternalServiceCategory
import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import com.github.livingwithhippos.nullseek.data.repository.ExternalServicesRepository
import com.github.livingwithhippos.nullseek.data.repository.PreferencesRepository
import com.github.livingwithhippos.nullseek.utils.FileTypeCategory
import com.github.livingwithhippos.nullseek.utils.detectFileTypeCategory
import com.github.livingwithhippos.nullseek.utils.isStreamableOrSubtitle
import com.github.livingwithhippos.nullseek.utils.saveFileToPublicDownloads
import com.github.livingwithhippos.nullseek.utils.setClipboardText
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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
    // a page that streams the file in the browser (Real-Debrid)
    val streamingPageUrl: String? = null,
    // the link includes the API token and must not be shared (TorBox)
    val containsToken: Boolean = true,
)

data class PendingDownload(
    val fileId: Int? = null,
    val fileName: String? = null,
)

sealed interface TorrentDetailEvent {
    data class ShowMessage(
        val message: String,
    ) : TorrentDetailEvent

    data object Deleted : TorrentDetailEvent

    /** a new torrent replaced this one in the details pane */
    data class OpenTorrent(
        val torrent: DebridTorrent,
    ) : TorrentDetailEvent
}

/** Adding a torrent again to pick other files (Real-Debrid). */
sealed interface RequeueState {
    data object Converting : RequeueState

    data class Picking(
        val newTorrent: DebridTorrent,
        val preselected: Set<Int>,
    ) : RequeueState

    data object Selecting : RequeueState
}

/** A file of a multi-link torrent, unlocked with its own link. */
data class UnlockedFile(
    val name: String,
    val size: Long?,
    val url: String,
    val streamingPageUrl: String?,
    val fileType: FileTypeCategory?,
    /** the torrent file it comes from, when the provider unlocks single files (TorBox) */
    val fileId: Int? = null,
    /** why the provider refused to unlock it, the url is empty then */
    val error: String? = null,
) {
    val failed: Boolean get() = error != null
}

data class TorrentDetailUiState(
    val torrent: DebridTorrent? = null,
    /** the account the torrent belongs to, results for other accounts are dropped */
    val accountId: Long? = null,
    val isSelectingFiles: Boolean = false,
    val requeue: RequeueState? = null,
    /** files of a multi-link torrent unlocked so far (Real-Debrid) */
    val unlockedFiles: List<UnlockedFile> = emptyList(),
    /** done and total links while unlocking, null when idle */
    val unlockProgress: Pair<Int, Int>? = null,
    val isRefreshing: Boolean = false,
    val isDownloading: Boolean = false,
    val isDeleting: Boolean = false,
    val downloadLinkResult: DownloadLinkResult? = null,
    val isExporting: Boolean = false,
    val isLoadingFullInfo: Boolean = false,
    val fullInfo: TorrentFullInfo? = null,
    val isFullInfoExpanded: Boolean = false,
    val showExportDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val pendingDownload: PendingDownload? = null,
)

@KoinViewModel
class TorrentDetailViewModel(
    private val debridProvider: DebridServiceProvider,
    private val preferencesRepository: PreferencesRepository,
    private val externalServicesRepository: ExternalServicesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TorrentDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TorrentDetailEvent>()
    val events = _events.asSharedFlow()

    private val service get() = debridProvider.current

    val capabilities: StateFlow<DebridCapabilities> =
        debridProvider.active
            .map { it?.capabilities ?: debridProvider.capabilities }
            .stateIn(viewModelScope, SharingStarted.Eagerly, debridProvider.capabilities)

    // unrestricted links are slow to make on Real-Debrid and count against the rate limit
    private val linkCache = HashMap<Pair<String, Int?>, DebridLink>()

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
            _events.emit(TorrentDetailEvent.ShowMessage("Sending to ${service.name}…"))
            val result = externalServicesRepository.playMedia(service, url)
            result.fold(
                onSuccess = {
                    _events.emit(TorrentDetailEvent.ShowMessage("Playing on ${service.name}"))
                },
                onFailure = { error ->
                    _events.emit(TorrentDetailEvent.ShowMessage("Failed to play on ${service.name}: ${error.message}"))
                },
            )
        }
    }

    fun init(item: DebridTorrent) {
        if (_uiState.value.torrent?.id != item.id || _uiState.value.torrent?.provider != item.provider) {
            linkCache.clear()
            unlockedLinks.clear()
            _uiState.value =
                TorrentDetailUiState(
                    torrent = item,
                    accountId =
                        debridProvider.active.value
                            ?.provider
                            ?.id,
                )
            // the Real-Debrid list has no files
            if (!item.filesLoaded) refreshTorrent(silent = true)
        }
    }

    fun refreshTorrent(silent: Boolean = false) {
        val current = _uiState.value.torrent ?: return
        val accountId = _uiState.value.accountId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            val response = service.torrent(current.id)
            if (!isCurrentAccount(accountId)) return@launch
            if (response is DebridResult.Ok) {
                val updated = response.value
                if (updated.hosterLinks != current.hosterLinks) linkCache.clear()
                _uiState.value =
                    _uiState.value.copy(
                        torrent = updated,
                        isRefreshing = false,
                    )
                if (!silent) _events.emit(TorrentDetailEvent.ShowMessage("Torrent info refreshed"))
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
                _events.emit(TorrentDetailEvent.ShowMessage("Download is only available for ready torrents"))
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
                    _uiState.value.torrent
                        ?.files
                        .orEmpty()
                val target =
                    when {
                        fileId != null -> files.firstOrNull { it.id == fileId }
                        files.size == 1 -> files.first()
                        else -> null
                    }
                target == null ||
                    !isStreamableOrSubtitle(
                        target.name,
                        target.mimetype,
                    )
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
        val torrent = _uiState.value.torrent ?: return
        if (!torrent.cached) {
            viewModelScope.launch {
                _events.emit(TorrentDetailEvent.ShowMessage("Download is only available for ready torrents"))
            }
            return
        }

        val accountId = _uiState.value.accountId
        val zip = zipLink && service.capabilities.zipDownloads
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true)
            val cacheKey = torrent.id to fileId
            val response =
                linkCache[cacheKey]?.takeIf { !zip }?.let { DebridResult.Ok(it) }
                    ?: service.torrentDownloadLink(torrent = torrent, fileId = fileId, zip = zip)
            _uiState.value = _uiState.value.copy(isDownloading = false)
            if (!isCurrentAccount(accountId)) return@launch

            if (response is DebridResult.Ok) {
                val link = response.value
                val url = link.url
                if (!zip) linkCache[cacheKey] = link
                val baseName = fileName ?: torrent.name
                val files = torrent.files.filter { it.selected }
                // without zip support a whole multi file download is a single archive made by the provider
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
                _events.emit(
                    TorrentDetailEvent.ShowMessage(
                        if (response.error == DebridError.Unsupported) {
                            "Pick a single file, this provider can't zip the whole download"
                        } else {
                            response.error.describe("Failed to generate download link")
                        },
                    ),
                )
            }
        }
    }

    /**
     * Picks the files to download on providers that need it (Real-Debrid).
     * @param fileIds the chosen files, every file when null
     */
    fun selectFiles(fileIds: Set<Int>?) {
        val torrent = _uiState.value.torrent ?: return
        val request = fileIds?.let { selectionToRequest(torrent.files.map { it.id }, it) }
        if (fileIds != null && fileIds.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSelectingFiles = true)
            val result = service.selectFiles(torrent.id, request)
            when (result) {
                is DebridResult.Ok -> {
                    // wait for the provider to apply it, or the old selection is shown again
                    val applied = service.awaitSelectionApplied(torrent.id)
                    _uiState.value =
                        _uiState.value.copy(
                            isSelectingFiles = false,
                            torrent = applied?.takeIf { _uiState.value.torrent?.id == it.id } ?: _uiState.value.torrent,
                        )
                    _events.emit(
                        TorrentDetailEvent.ShowMessage(
                            selectionMismatch(request, applied) ?: "Files selected, the download is starting",
                        ),
                    )
                }

                is DebridResult.Err -> {
                    _uiState.value = _uiState.value.copy(isSelectingFiles = false)
                    _events.emit(TorrentDetailEvent.ShowMessage(result.error.describe("Failed to select the files")))
                }
            }
        }
    }

    // unlocked hoster links, so unlocking again doesn't use the rate limit
    private val unlockedLinks = HashMap<String, DebridLink>()

    /** Unlocks every hoster link of the torrent, one at a time, showing each file as soon as it's ready. */
    fun unlockAllFiles() {
        val torrent = _uiState.value.torrent ?: return
        if (!torrent.cached || _uiState.value.unlockProgress != null) return
        val accountId = _uiState.value.accountId
        viewModelScope.launch {
            val loaded =
                if (torrent.filesLoaded && torrent.hosterLinks.isNotEmpty()) {
                    torrent
                } else {
                    (service.torrent(torrent.id) as? DebridResult.Ok)?.value ?: torrent
                }
            val links = loaded.hosterLinks
            if (links.isEmpty()) {
                _events.emit(TorrentDetailEvent.ShowMessage("No links to unlock yet"))
                return@launch
            }
            _uiState.value = _uiState.value.copy(unlockedFiles = emptyList(), unlockProgress = 0 to links.size)
            val failures = mutableListOf<String>()
            // links follow the selected files, used to name the ones that fail
            val linkNames = loaded.files.filter { it.linkIndex != null }.associate { it.linkIndex to it.shortName }
            links.forEachIndexed { index, hosterLink ->
                val result =
                    unlockedLinks[hosterLink]?.let { DebridResult.Ok(it) }
                        ?: service.unlockLink(hosterLink)
                if (!isCurrentAccount(accountId) || _uiState.value.torrent?.id != loaded.id) return@launch
                when (result) {
                    is DebridResult.Ok -> {
                        val link = result.value
                        unlockedLinks[hosterLink] = link
                        val name = link.fileName ?: hosterLink.substringAfterLast('/')
                        _uiState.value =
                            _uiState.value.copy(
                                unlockedFiles =
                                    _uiState.value.unlockedFiles +
                                        UnlockedFile(
                                            name = name,
                                            size = link.size,
                                            url = link.url,
                                            streamingPageUrl = link.streamingPageUrl,
                                            fileType = detectFileTypeCategory(name, link.mimeType),
                                        ),
                            )
                    }

                    is DebridResult.Err -> {
                        val reason = result.error.reason()
                        failures.add(reason)
                        _uiState.value =
                            _uiState.value.copy(
                                unlockedFiles =
                                    _uiState.value.unlockedFiles +
                                        UnlockedFile(
                                            name = linkNames[index]?.takeIf { it.isNotBlank() } ?: "File ${index + 1}",
                                            size = null,
                                            url = "",
                                            streamingPageUrl = null,
                                            fileType = null,
                                            error = reason,
                                        ),
                            )
                    }
                }
                _uiState.value = _uiState.value.copy(unlockProgress = index + 1 to links.size)
            }
            _uiState.value = _uiState.value.copy(unlockProgress = null)
            _events.emit(TorrentDetailEvent.ShowMessage(unlockSummary(links.size - failures.size, failures)))
        }
    }

    /**
     * Unlocks the chosen files one by one, each with its own link instead of a zip (TorBox).
     * New files are added to the ones already unlocked.
     */
    fun unlockFiles(fileIds: Set<Int>) {
        val torrent = _uiState.value.torrent ?: return
        if (!torrent.cached || fileIds.isEmpty() || _uiState.value.unlockProgress != null) return
        val accountId = _uiState.value.accountId
        // files that failed before are tried again
        val alreadyUnlocked =
            _uiState.value.unlockedFiles
                .filter { !it.failed }
                .mapNotNull { it.fileId }
                .toSet()
        val targets = torrent.files.filter { it.id in fileIds && it.id !in alreadyUnlocked }
        if (targets.isEmpty()) {
            viewModelScope.launch { _events.emit(TorrentDetailEvent.ShowMessage("These files are already unlocked")) }
            return
        }
        viewModelScope.launch {
            val retried = targets.map { it.id }.toSet()
            _uiState.value =
                _uiState.value.copy(
                    unlockProgress = 0 to targets.size,
                    unlockedFiles = _uiState.value.unlockedFiles.filterNot { it.failed && it.fileId in retried },
                )
            var unlocked = 0
            val failures = mutableListOf<String>()
            targets.forEachIndexed { index, file ->
                val key = torrent.id to file.id
                val result =
                    linkCache[key]?.let { DebridResult.Ok(it) }
                        ?: service.torrentDownloadLink(torrent, file.id, zip = false)
                if (!isCurrentAccount(accountId) || _uiState.value.torrent?.id != torrent.id) return@launch
                when (result) {
                    is DebridResult.Ok -> {
                        val link = result.value
                        linkCache[key] = link
                        val name = file.shortName.ifBlank { file.name.substringAfterLast('/') }
                        unlocked++
                        _uiState.value =
                            _uiState.value.copy(
                                unlockedFiles =
                                    _uiState.value.unlockedFiles +
                                        UnlockedFile(
                                            name = name,
                                            size = file.size,
                                            url = link.url,
                                            streamingPageUrl = link.streamingPageUrl,
                                            fileType = detectFileTypeCategory(file.name, file.mimetype),
                                            fileId = file.id,
                                        ),
                            )
                    }

                    is DebridResult.Err -> {
                        val reason = result.error.reason()
                        failures.add(reason)
                        _uiState.value =
                            _uiState.value.copy(
                                unlockedFiles =
                                    _uiState.value.unlockedFiles +
                                        UnlockedFile(
                                            name = file.shortName.ifBlank { file.name.substringAfterLast('/') },
                                            size = file.size,
                                            url = "",
                                            streamingPageUrl = null,
                                            fileType = detectFileTypeCategory(file.name, file.mimetype),
                                            fileId = file.id,
                                            error = reason,
                                        ),
                            )
                    }
                }
                _uiState.value = _uiState.value.copy(unlockProgress = index + 1 to targets.size)
            }
            _uiState.value = _uiState.value.copy(unlockProgress = null)
            _events.emit(TorrentDetailEvent.ShowMessage(unlockSummary(unlocked, failures)))
        }
    }

    /** The final message of an unlock, naming the reason when all the failures share it. */
    private fun unlockSummary(
        unlocked: Int,
        failures: List<String>,
    ): String {
        val reason = failures.distinct().singleOrNull()
        return when {
            failures.isEmpty() -> "Unlocked $unlocked files"
            unlocked == 0 && reason != null -> "Failed to unlock the files: $reason"
            unlocked == 0 -> "Failed to unlock the files, see the list for the reasons"
            reason != null -> "Unlocked $unlocked files, ${failures.size} failed: $reason"
            else -> "Unlocked $unlocked files, ${failures.size} failed, see the list for the reasons"
        }
    }

    fun clearUnlockedFiles() {
        if (_uiState.value.unlockProgress != null) return
        _uiState.value = _uiState.value.copy(unlockedFiles = emptyList())
    }

    /** Copies the links of the unlocked files, one per line. */
    fun copyUnlockedLinks(context: Context) {
        val links =
            _uiState.value.unlockedFiles
                .filter { !it.failed }
                .map { it.url }
        if (links.isEmpty()) return
        setClipboardText(context, links.joinToString("\n"))
        viewModelScope.launch { _events.emit(TorrentDetailEvent.ShowMessage("Copied ${links.size} links to the clipboard")) }
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
            val response = service.exportTorrent(torrent.id, asMagnet = true)
            _uiState.value = _uiState.value.copy(isExporting = false)

            if (response is DebridResult.Ok) {
                when (val result = response.value) {
                    is ExportPayload.Magnet -> {
                        setClipboardText(context, result.magnet)
                        _events.emit(TorrentDetailEvent.ShowMessage("Magnet link copied to clipboard"))
                    }

                    is ExportPayload.TorrentFile -> {
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
            val response = service.exportTorrent(torrent.id, asMagnet = false)
            _uiState.value = _uiState.value.copy(isExporting = false)

            if (response is DebridResult.Ok) {
                when (val result = response.value) {
                    is ExportPayload.TorrentFile -> {
                        val fileName =
                            if (torrent.name.endsWith(".torrent", ignoreCase = true)) {
                                torrent.name
                            } else {
                                "${torrent.name}.torrent"
                            }
                        val savedUri =
                            saveFileToPublicDownloads(
                                context = context,
                                fileName = fileName,
                                bytes = result.bytes,
                            )
                        if (savedUri != null) {
                            _events.emit(TorrentDetailEvent.ShowMessage("Torrent saved to Downloads"))
                        } else {
                            _events.emit(TorrentDetailEvent.ShowMessage("Failed to save torrent to Downloads"))
                        }
                    }

                    is ExportPayload.Magnet -> {
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
            val response = service.torrentFullInfo(torrent.hash)
            if (response is DebridResult.Ok) {
                _uiState.value =
                    _uiState.value.copy(
                        isLoadingFullInfo = false,
                        fullInfo = response.value,
                        isFullInfoExpanded = true,
                    )
            } else {
                _uiState.value = _uiState.value.copy(isLoadingFullInfo = false)
                _events.emit(TorrentDetailEvent.ShowMessage("Failed to load full info"))
            }
        }
    }

    fun toggleFullInfoExpanded() {
        _uiState.value =
            _uiState.value.copy(
                isFullInfoExpanded = !_uiState.value.isFullInfoExpanded,
            )
    }

    fun showDeleteConfirmDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = show)
    }

    /** Adds the torrent again from its hash, to pick other files. The original torrent is kept. */
    fun startRequeue() {
        val torrent = _uiState.value.torrent ?: return
        if (_uiState.value.requeue != null) return
        val accountId = _uiState.value.accountId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(requeue = RequeueState.Converting)
            // the previous selection is needed to check the same files in the picker
            val oldTorrent =
                if (torrent.filesLoaded && torrent.files.isNotEmpty()) {
                    torrent
                } else {
                    (service.torrent(torrent.id) as? DebridResult.Ok)?.value ?: torrent
                }
            val added = service.addMagnet(magnetFromHash(torrent.hash))
            if (!isCurrentAccount(accountId)) return@launch
            if (added is DebridResult.Err) {
                _uiState.value = _uiState.value.copy(requeue = null)
                _events.emit(TorrentDetailEvent.ShowMessage(added.error.describe("Could not add the torrent again")))
                return@launch
            }
            val newId = (added as DebridResult.Ok).value.id
            when (val outcome = service.awaitConversion(newId)) {
                is ConversionOutcome.NeedsSelection -> {
                    _uiState.value =
                        _uiState.value.copy(
                            requeue = RequeueState.Picking(outcome.torrent, requeuePreselection(oldTorrent, outcome.torrent)),
                        )
                }

                is ConversionOutcome.Started -> {
                    _uiState.value = _uiState.value.copy(requeue = null)
                    _events.emit(TorrentDetailEvent.OpenTorrent(outcome.torrent))
                }

                is ConversionOutcome.Failed -> {
                    _uiState.value = _uiState.value.copy(requeue = null)
                    _events.emit(TorrentDetailEvent.ShowMessage("Could not add the torrent again: ${outcome.reason}"))
                }

                ConversionOutcome.TimedOut -> {
                    _uiState.value = _uiState.value.copy(requeue = null)
                    _events.emit(
                        TorrentDetailEvent.ShowMessage("The torrent is still being read, pick its files from its page later"),
                    )
                }
            }
        }
    }

    fun confirmRequeue(fileIds: Set<Int>) {
        val picking = _uiState.value.requeue as? RequeueState.Picking ?: return
        if (fileIds.isEmpty()) return
        val newTorrent = picking.newTorrent
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(requeue = RequeueState.Selecting)
            val request = selectionToRequest(newTorrent.files.map { it.id }, fileIds)
            val result = service.selectFiles(newTorrent.id, request)
            when (result) {
                is DebridResult.Ok -> {
                    // the provider applies the selection a moment later, reading it back at once shows every file
                    val applied = service.awaitSelectionApplied(newTorrent.id)
                    _uiState.value = _uiState.value.copy(requeue = null)
                    _events.emit(
                        TorrentDetailEvent.ShowMessage(
                            selectionMismatch(request, applied) ?: "New torrent added, the original one was kept",
                        ),
                    )
                    _events.emit(TorrentDetailEvent.OpenTorrent(applied ?: newTorrent))
                }

                is DebridResult.Err -> {
                    // let the user try again with the picker
                    _uiState.value = _uiState.value.copy(requeue = picking)
                    _events.emit(TorrentDetailEvent.ShowMessage(result.error.describe("Failed to select the files")))
                }
            }
        }
    }

    /** Drops the torrent added for the new selection, so nothing is left waiting. */
    fun cancelRequeue() {
        val picking = _uiState.value.requeue as? RequeueState.Picking
        _uiState.value = _uiState.value.copy(requeue = null)
        if (picking != null) {
            viewModelScope.launch { service.deleteTorrent(picking.newTorrent.id) }
        }
    }

    fun deleteTorrent() {
        val torrent = _uiState.value.torrent ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, showDeleteConfirmDialog = false)
            val response = service.deleteTorrent(torrent.id)
            if (response is DebridResult.Ok) {
                _events.emit(TorrentDetailEvent.ShowMessage("Torrent deleted"))
                _events.emit(TorrentDetailEvent.Deleted)
            } else {
                _uiState.value = _uiState.value.copy(isDeleting = false)
                _events.emit(TorrentDetailEvent.ShowMessage("Failed to delete torrent"))
            }
        }
    }
}
