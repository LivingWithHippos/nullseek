package com.github.livingwithhippos.nullseek.tv.ui.lists

import com.github.livingwithhippos.nullseek.core.domain.needsFileSelection
import com.github.livingwithhippos.nullseek.tv.ui.components.TvFileSelectionSection
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import com.github.livingwithhippos.nullseek.ui.lists.mediaFiles
import androidx.compose.runtime.remember
import com.github.livingwithhippos.nullseek.tv.ui.components.TvTextField
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.tv.ui.components.ConfirmDialog
import com.github.livingwithhippos.nullseek.tv.ui.components.InfoRow
import com.github.livingwithhippos.nullseek.tv.ui.components.LocalMessenger
import com.github.livingwithhippos.nullseek.tv.ui.components.SectionCard
import com.github.livingwithhippos.nullseek.tv.ui.components.TvActionButton
import com.github.livingwithhippos.nullseek.tv.ui.components.TvDialog
import com.github.livingwithhippos.nullseek.tv.ui.components.TvFileSelectionDialog
import com.github.livingwithhippos.nullseek.ui.lists.torrent.RequeueState
import com.github.livingwithhippos.nullseek.ui.lists.torrent.TorrentDetailEvent
import com.github.livingwithhippos.nullseek.ui.lists.torrent.TorrentDetailViewModel
import com.github.livingwithhippos.nullseek.utils.wholeDownloadIsZipped
import com.github.livingwithhippos.nullseek.core.domain.DebridTorrent
import org.koin.androidx.compose.koinViewModel

@Composable
fun TvTorrentDetailScreen(
    item: DebridTorrent,
    onClose: () -> Unit,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleted: () -> Unit = onClose,
    // shows another torrent in this pane, e.g. the one added again with other files
    onOpenTorrent: (DebridTorrent) -> Unit = {},
    viewModel: TorrentDetailViewModel = koinViewModel(),
) {
    BackHandler(onBack = onClose)
    LaunchedEffect(item.id) { viewModel.init(item) }

    val context = LocalContext.current
    val messenger = LocalMessenger.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val defaultPlayer by viewModel.defaultMediaPlayer.collectAsStateWithLifecycle()
    var unlockedQuery by rememberSaveable(item.id) { mutableStateOf("") }
    var selectionMode by rememberSaveable(item.id) { mutableStateOf(false) }
    var selectedFiles by rememberSaveable(item.id) { mutableStateOf(emptySet<Int>()) }
    val mediaPlayers by viewModel.mediaPlayerServices.collectAsStateWithLifecycle()
    val filterFilesFullPath by viewModel.filterFilesFullPath.collectAsStateWithLifecycle()
    val downloadZipMode by viewModel.downloadZipMode.collectAsStateWithLifecycle()
    val capabilities by viewModel.capabilities.collectAsStateWithLifecycle()
    val torrent = uiState.torrent?.takeIf { it.id == item.id } ?: item

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TorrentDetailEvent.ShowMessage -> messenger.show(event.message)
                TorrentDetailEvent.Deleted -> onDeleted()
                is TorrentDetailEvent.OpenTorrent -> onOpenTorrent(event.torrent)
            }
        }
    }

    TvDetailPane(
        modifier = modifier,
        overview =
            DetailOverview(
                name = torrent.name,
                itemId = torrent.id,
                sizeBytes = torrent.size,
                state = torrent.downloadState,
                cached = torrent.cached,
                active = torrent.active,
                createdAt = torrent.createdAt,
                expiresAt = torrent.expiresAt,
                progress = torrent.progress,
                downloadSpeed = torrent.downloadSpeed,
                uploadSpeed = torrent.uploadSpeed,
                eta = torrent.eta,
                seeds = torrent.seeds,
                peers = torrent.peers,
                ratio = torrent.ratio,
                hash = torrent.hash,
                sourceLabel = stringResource(R.string.magnet_link),
                sourceLink = torrent.magnet,
            ),
        onClose = onClose,
        isFullScreen = isFullScreen,
        onToggleFullScreen = onToggleFullScreen,
        linkResult = uiState.downloadLinkResult?.let { LinkResult(it.title, it.url, it.isZipped, it.fileType, it.streamingPageUrl, it.containsToken) },
        onDismissLink = viewModel::dismissDownloadLink,
        defaultPlayer = defaultPlayer,
        mediaPlayers = mediaPlayers,
        onPlayMedia = viewModel::playOnMedia,
        // only the picked files can be downloaded on Real-Debrid
        // every file, the ones not picked on Real-Debrid are marked as such
        files = if (torrent.needsFileSelection) emptyList() else torrent.files.map { it.toDetailFile() },
        filterFilesFullPath = filterFilesFullPath,
        // on Real-Debrid the files come together, the list is read-only
        onDownloadFile = if (torrent.cached && capabilities.perFileLinks && !uiState.isDeleting) { file -> viewModel.startDownload(file.id, file.name) } else null,
        onDownloadDirect = if (torrent.cached && capabilities.perFileLinks && !uiState.isDeleting) { file -> viewModel.downloadDirect(file.id, file.name) } else null,
        isDownloading = uiState.isDownloading,
        actions = {
            val selectedFiles = torrent.files.filter { it.selected }
            // without zips a whole download works only with a single link, otherwise each file gets its own
            val wholeAsLinks = !capabilities.zipDownloads && selectedFiles.size > 1 && torrent.hosterLinks.size != 1
            if (wholeAsLinks) {
                val progress = uiState.unlockProgress
                TvActionButton(
                    text =
                        when {
                            progress != null -> stringResource(R.string.unlocking_progress, progress.first, progress.second)
                            uiState.unlockedFiles.isNotEmpty() -> stringResource(R.string.unlock_again)
                            else -> stringResource(R.string.unlock_all_files)
                        },
                    icon = R.drawable.ic_download,
                    primary = true,
                    enabled = torrent.cached && progress == null && !uiState.isDeleting,
                    loading = progress != null,
                    onClick = viewModel::unlockAllFiles,
                )
            } else {
                val isZip =
                    capabilities.zipDownloads &&
                        wholeDownloadIsZipped(downloadZipMode, torrent.files, { it.name }, { it.mimetype })
                TvActionButton(
                    text = stringResource(if (isZip) R.string.action_generate_zip else R.string.action_download),
                    icon = R.drawable.ic_download,
                    primary = true,
                    enabled = torrent.cached && !uiState.isDeleting,
                    loading = uiState.isDownloading,
                    onClick = { viewModel.startDownload() },
                )
            }
            // a direct link for each media file instead of a zip (TorBox)
            val mediaCount = remember(torrent.files) { torrent.files.mediaFiles().size }
            if (capabilities.perFileLinks && torrent.cached && mediaCount >= 2) {
                AllMediaLinksAction(
                    mediaCount = mediaCount,
                    progress = uiState.unlockProgress,
                    hasUnlocked = uiState.unlockedFiles.isNotEmpty(),
                    enabled = !uiState.isDeleting,
                    onClick = viewModel::unlockAllMedia,
                )
            }
            if (capabilities.exportTorrent) {
                TvActionButton(
                    text = stringResource(R.string.action_export),
                    icon = R.drawable.ic_magnet,
                    loading = uiState.isExporting,
                    enabled = !uiState.isDeleting,
                    onClick = { viewModel.showExportDialog(true) },
                )
            }
            if (capabilities.fullTorrentInfo) {
                TvActionButton(
                    text = stringResource(R.string.action_full_info),
                    icon = R.drawable.ic_info,
                    loading = uiState.isLoadingFullInfo,
                    enabled = !uiState.isDeleting,
                    onClick = viewModel::loadFullInfo,
                )
            }
            // nothing to refresh once the item is cached or fully downloaded
            // Real-Debrid can't change the files of a torrent, it is added again instead
            if (capabilities.reselectFiles && !torrent.needsFileSelection) {
                TvActionButton(
                    text = stringResource(R.string.requeue_choose_files),
                    icon = R.drawable.ic_edit,
                    loading = uiState.requeue != null,
                    enabled = uiState.requeue == null && !uiState.isDeleting,
                    onClick = viewModel::startRequeue,
                )
            }
            if (!(torrent.cached || torrent.downloadFinished || torrent.progress >= 1.0)) {
                TvActionButton(
                    text = stringResource(R.string.action_refresh),
                    icon = R.drawable.ic_refresh,
                    loading = uiState.isRefreshing,
                    enabled = !uiState.isDeleting,
                    onClick = { viewModel.refreshTorrent() },
                )
            }
            TvActionButton(
                text = stringResource(R.string.action_delete),
                icon = R.drawable.ic_delete,
                loading = uiState.isDeleting,
                onClick = { viewModel.showDeleteConfirmDialog(true) },
            )
        },
        // TorBox can unlock single files, pick several to avoid a zip
        fileSelection =
            if (capabilities.perFileLinks && torrent.cached && !uiState.isDeleting) {
                TvFileSelection(
                    active = selectionMode,
                    selected = selectedFiles,
                    unlockProgress = uiState.unlockProgress,
                    onToggleMode = {
                        selectionMode = !selectionMode
                        selectedFiles = emptySet()
                    },
                    onToggle = { id -> selectedFiles = if (id in selectedFiles) selectedFiles - id else selectedFiles + id },
                    onSelect = { ids -> selectedFiles = selectedFiles + ids },
                    onClear = { selectedFiles = emptySet() },
                    onUnlock = {
                        viewModel.unlockFiles(selectedFiles)
                        selectedFiles = emptySet()
                        selectionMode = false
                    },
                )
            } else {
                null
            },
        extraSections = {
            // the files unlocked one by one, each with its own link
            unlockedFilesSection(
                files = uiState.unlockedFiles,
                query = unlockedQuery,
                onQueryChange = { unlockedQuery = it },
                unlocking = uiState.unlockProgress != null,
                containsToken = capabilities.linksContainToken,
                defaultPlayer = defaultPlayer,
                mediaPlayers = mediaPlayers,
                onPlayMedia = viewModel::playOnMedia,
                onCopyAll = { viewModel.copyUnlockedLinks(context) },
                onClear = viewModel::clearUnlockedFiles,
            )
            if (torrent.needsFileSelection && torrent.files.isNotEmpty()) {
                item {
                    TvFileSelectionSection(
                        files = torrent.files,
                        isSelecting = uiState.isSelectingFiles,
                        onConfirm = { viewModel.selectFiles(it) },
                    )
                }
            } else if (!torrent.cached) {
                item {
                    Text(
                        stringResource(R.string.download_not_cached_warning),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            val fullInfo = uiState.fullInfo
            if (fullInfo != null && uiState.isFullInfoExpanded) {
                item {
                    SectionCard(title = stringResource(R.string.full_info_title), icon = R.drawable.ic_info) {
                        InfoRow(stringResource(R.string.swarm_seeds), fullInfo.seeds.toString())
                        InfoRow(stringResource(R.string.swarm_peers), fullInfo.peers.toString())
                        Text(stringResource(R.string.trackers_label), style = MaterialTheme.typography.titleSmall)
                        fullInfo.trackers.forEach { tracker ->
                            Text(tracker, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
    )

    (uiState.requeue as? RequeueState.Picking)?.let { picking ->
        TvFileSelectionDialog(
            name = picking.newTorrent.name,
            files = picking.newTorrent.files,
            remaining = 0,
            onConfirm = viewModel::confirmRequeue,
            onLater = viewModel::cancelRequeue,
            onSelectAllRemaining = {},
            initialSelected = picking.preselected,
        )
    }

    uiState.pendingDownload?.let { pending ->
        DownloadZipDialog(
            itemName = pending.fileName ?: torrent.name,
            alwaysZipped = pending.fileId == null && torrent.files.size > 1,
            onConfirm = viewModel::confirmDownloadModal,
            onDismiss = viewModel::dismissDownloadModal,
        )
    }

    if (uiState.showExportDialog) {
        TvDialog(
            title = stringResource(R.string.export_dialog_title),
            onDismiss = { viewModel.showExportDialog(false) },
            confirmText = stringResource(R.string.cancel),
            onConfirm = { viewModel.showExportDialog(false) },
        ) {
            TvActionButton(
                text = stringResource(R.string.export_as_magnet),
                icon = R.drawable.ic_magnet,
                onClick = { viewModel.exportMagnet(context) },
            )
            TvActionButton(
                text = stringResource(R.string.export_as_file),
                icon = R.drawable.ic_download,
                onClick = { viewModel.exportTorrentFile(context) },
            )
        }
    }

    if (uiState.showDeleteConfirmDialog) {
        ConfirmDialog(
            title = stringResource(R.string.delete_torrent_confirm_title),
            message = stringResource(R.string.delete_torrent_confirm_message),
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                viewModel.showDeleteConfirmDialog(false)
                viewModel.deleteTorrent()
            },
            onDismiss = { viewModel.showDeleteConfirmDialog(false) },
            dismissText = stringResource(R.string.cancel),
        )
    }
}
