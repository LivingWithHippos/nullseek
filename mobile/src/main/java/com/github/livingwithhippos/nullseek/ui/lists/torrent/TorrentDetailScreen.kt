package com.github.livingwithhippos.nullseek.ui.lists.torrent

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.utils.convertToSize
import com.github.livingwithhippos.nullseek.core.utils.formatSpeed
import com.github.livingwithhippos.nullseek.core.utils.mockTorrents
import com.github.livingwithhippos.nullseek.data.model.ExternalService
import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import com.github.livingwithhippos.nullseek.ui.externalservices.PlayChoiceDialog
import com.github.livingwithhippos.nullseek.ui.theme.Button
import com.github.livingwithhippos.nullseek.ui.theme.FilledTonalButton
import com.github.livingwithhippos.nullseek.ui.theme.FilterChip
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.theme.OutlinedButton
import com.github.livingwithhippos.nullseek.ui.theme.TextButton
import com.github.livingwithhippos.nullseek.ui.utils.DeletingOverlay
import com.github.livingwithhippos.nullseek.ui.utils.DetailOverviewCard
import com.github.livingwithhippos.nullseek.ui.utils.DownloadLinkActions
import com.github.livingwithhippos.nullseek.ui.utils.DownloadLinkTypeRow
import com.github.livingwithhippos.nullseek.ui.utils.DownloadZipModal
import com.github.livingwithhippos.nullseek.ui.utils.FileTypeBadge
import com.github.livingwithhippos.nullseek.ui.utils.MultipleVideosNotice
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import com.github.livingwithhippos.nullseek.ui.utils.OverviewInfo
import com.github.livingwithhippos.nullseek.ui.utils.SingleVideoShortcut
import com.github.livingwithhippos.nullseek.utils.MultiFileVideos
import com.github.livingwithhippos.nullseek.utils.detectFileTypeCategory
import com.github.livingwithhippos.nullseek.utils.isAudioFile
import com.github.livingwithhippos.nullseek.utils.isStreamableOrSubtitle
import com.github.livingwithhippos.nullseek.utils.isSubtitleFile
import com.github.livingwithhippos.nullseek.utils.isVideoFile
import com.github.livingwithhippos.nullseek.utils.multiFileVideos
import com.github.livingwithhippos.nullseek.utils.openWebPage
import com.github.livingwithhippos.nullseek.utils.openWith
import com.github.livingwithhippos.nullseek.utils.setClipboardText
import com.github.livingwithhippos.nullseek.utils.shareText
import com.github.livingwithhippos.nullseek.utils.wholeDownloadIsZipped
import com.github.livingwithhippos.nullseek.core.domain.DebridCapabilities
import com.github.livingwithhippos.nullseek.core.domain.DebridTorrent
import com.github.livingwithhippos.nullseek.core.domain.needsFileSelection
import com.github.livingwithhippos.nullseek.ui.utils.FileSelectionCard
import com.github.livingwithhippos.nullseek.ui.utils.FileSelectionDialog
import com.github.livingwithhippos.nullseek.core.domain.DebridFile
import com.github.livingwithhippos.nullseek.core.domain.TorrentFullInfo
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun TorrentDetailScreen(
    modifier: Modifier = Modifier,
    contentType: NullContentType,
    navigationType: NullNavigationType,
    item: DebridTorrent,
    closeDetails: () -> Unit,
    // shows another torrent in this pane, e.g. the one added again with other files
    onOpenTorrent: (DebridTorrent) -> Unit = {},
    viewModel: TorrentDetailViewModel = koinViewModel(),
) {
    BackHandler {
        closeDetails()
    }

    LaunchedEffect(item.id) {
        viewModel.init(item)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val defaultPlayer by viewModel.defaultMediaPlayer.collectAsStateWithLifecycle()
    val mediaPlayers by viewModel.mediaPlayerServices.collectAsStateWithLifecycle()
    val filterFilesFullPath by viewModel.filterFilesFullPath.collectAsStateWithLifecycle()
    val downloadZipMode by viewModel.downloadZipMode.collectAsStateWithLifecycle()
    val capabilities by viewModel.capabilities.collectAsStateWithLifecycle()
    val currentItem = uiState.torrent ?: item
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TorrentDetailEvent.ShowMessage -> {
                    // don't wait for the snackbar, or Deleted would be handled only after it hides
                    launch { snackbarHostState.showSnackbar(event.message) }
                }

                TorrentDetailEvent.Deleted -> {
                    closeDetails()
                }

                is TorrentDetailEvent.OpenTorrent -> {
                    onOpenTorrent(event.torrent)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            TorrentDetailScreen(
                modifier = Modifier.fillMaxSize(),
                context = LocalContext.current,
                item = currentItem,
                uiState = uiState,
                capabilities = capabilities,
                onSelectFiles = { viewModel.selectFiles(it) },
                onUnlockAll = { viewModel.unlockAllFiles() },
                onUnlockFiles = { viewModel.unlockFiles(it) },
                onClearUnlocked = { viewModel.clearUnlockedFiles() },
                onCopyUnlockedLinks = { context -> viewModel.copyUnlockedLinks(context) },
                onRequeue = { viewModel.startRequeue() },
                onConfirmRequeue = { viewModel.confirmRequeue(it) },
                onCancelRequeue = { viewModel.cancelRequeue() },
                defaultPlayer = defaultPlayer,
                mediaPlayers = mediaPlayers,
                filterFilesFullPath = filterFilesFullPath,
                downloadZipMode = downloadZipMode,
                onPlayMedia = { service, url, setAsDefault ->
                    viewModel.playOnMedia(service, url, setAsDefault)
                },
                onRefresh = { viewModel.refreshTorrent() },
                onDownloadZip = { viewModel.startDownload() },
                onDownloadDirect = { fileId, fileName -> viewModel.downloadDirect(fileId, fileName) },
                onDownloadFile = { fileId, fileName ->
                    viewModel.startDownload(
                        fileId, fileName
                    )
                },
                onDismissDownloadLink = { viewModel.dismissDownloadLink() },
                onConfirmDownloadModal = { mode, rememberChoice ->
                    viewModel.confirmDownloadModal(mode, rememberChoice)
                },
                onDismissDownloadModal = { viewModel.dismissDownloadModal() },
                onShowExportDialog = { viewModel.showExportDialog(it) },
                onExportMagnet = { context -> viewModel.exportMagnet(context) },
                onExportTorrentFile = { context -> viewModel.exportTorrentFile(context) },
                onToggleFullInfo = { viewModel.loadFullInfo() },
                onShowDeleteDialog = { viewModel.showDeleteConfirmDialog(it) },
                onConfirmDelete = { viewModel.deleteTorrent() },
            )
            DeletingOverlay(visible = uiState.isDeleting)
        }
    }
}

@Composable
private fun TorrentDetailScreen(
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    item: DebridTorrent,
    uiState: TorrentDetailUiState,
    capabilities: DebridCapabilities = DebridCapabilities.Torbox,
    onSelectFiles: (Set<Int>) -> Unit = {},
    onUnlockAll: () -> Unit = {},
    onUnlockFiles: (Set<Int>) -> Unit = {},
    onClearUnlocked: () -> Unit = {},
    onCopyUnlockedLinks: (Context) -> Unit = {},
    onRequeue: () -> Unit = {},
    onConfirmRequeue: (Set<Int>) -> Unit = {},
    onCancelRequeue: () -> Unit = {},
    defaultPlayer: ExternalService?,
    mediaPlayers: List<ExternalService>,
    filterFilesFullPath: Boolean = false,
    downloadZipMode: DownloadZipMode = DownloadZipMode.ASK,
    onPlayMedia: (ExternalService, String, Boolean) -> Unit,
    onRefresh: () -> Unit,
    onDownloadZip: () -> Unit,
    onDownloadFile: (Int, String) -> Unit,
    onDownloadDirect: (Int, String) -> Unit = { _, _ -> },
    onDismissDownloadLink: () -> Unit,
    onConfirmDownloadModal: (DownloadZipMode, Boolean) -> Unit,
    onDismissDownloadModal: () -> Unit,
    onShowExportDialog: (Boolean) -> Unit,
    onExportMagnet: (Context) -> Unit,
    onExportTorrentFile: (Context) -> Unit,
    onToggleFullInfo: () -> Unit,
    onShowDeleteDialog: (Boolean) -> Unit,
    onConfirmDelete: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    // the link waiting for a media player to be picked
    var pendingPlayUrl by remember { mutableStateOf<String?>(null) }
    val playUrl: (String) -> Unit = { url ->
        if (defaultPlayer != null) onPlayMedia(defaultPlayer, url, false) else pendingPlayUrl = url
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                focusManager.clearFocus()
            }
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Card 1: Torrent Overview (includes Magnet link)
            DetailOverviewCard(info = item.toOverviewInfo())

            // Card 2: Transfer & Progress (hidden if cached at 100% progress)
            val isCachedAndComplete = item.cached && (item.progress >= 1.0 || item.downloadFinished)
            if (!isCachedAndComplete) {
                TorrentTransferProgressCard(item = item)
            }

            // Section: Generated Download Link (Options: Open, Copy, Share)
            // a new link appears near the top: scroll to it, the user may be at the bottom of the file list
            val linkRequester = remember { BringIntoViewRequester() }
            LaunchedEffect(uiState.downloadLinkResult) {
                if (uiState.downloadLinkResult != null) {
                    withFrameNanos { }
                    linkRequester.bringIntoView()
                }
            }
            uiState.downloadLinkResult?.let { linkResult ->
                TorrentDownloadLinkCard(
                    result = linkResult,
                    context = context,
                    defaultPlayer = defaultPlayer,
                    onPlay = { playUrl(linkResult.url) },
                    onDismiss = onDismissDownloadLink,
                    modifier = Modifier.bringIntoViewRequester(linkRequester),
                )
            }

            // Real-Debrid torrents with a link for each file
            if (uiState.unlockedFiles.isNotEmpty()) {
                UnlockedFilesCard(
                    files = uiState.unlockedFiles,
                    defaultPlayer = defaultPlayer,
                    onPlay = playUrl,
                    onCopyAll = { onCopyUnlockedLinks(context) },
                    onClear = onClearUnlocked,
                    showTokenWarning = capabilities.linksContainToken,
                    context = context,
                )
            }

            // Card 4: Actions (Refresh, Download, Export, Full Info)
            // the Video filter lives here so the actions card can turn it on
            var filterVideo by rememberSaveable { mutableStateOf(false) }
            val filesRequester = remember { BringIntoViewRequester() }
            val scope = rememberCoroutineScope()
            val showVideos: () -> Unit = {
                filterVideo = true
                scope.launch { filesRequester.bringIntoView() }
            }
            // Real-Debrid waits for the files to be picked before downloading
            if (item.needsFileSelection && item.files.isNotEmpty()) {
                FileSelectionCard(
                    files = item.files,
                    isSelecting = uiState.isSelectingFiles,
                    onConfirm = onSelectFiles,
                )
            }

            TorrentActionsCard(
                item = item,
                uiState = uiState,
                capabilities = capabilities,
                onUnlockAll = onUnlockAll,
                onRequeue = onRequeue,
                onRefresh = onRefresh,
                onDownloadZip = onDownloadZip,
                onDownloadDirect = onDownloadDirect,
                onShowVideos = showVideos,
                downloadZipMode = downloadZipMode,
                onShowExportDialog = { onShowExportDialog(true) },
                onToggleFullInfo = onToggleFullInfo,
                onDelete = { onShowDeleteDialog(true) },
            )

            // Expandable Card: Full BitTorrent Swarm Info
            uiState.fullInfo?.let { fullInfo ->
                TorrentFullInfoCard(
                    fullInfo = fullInfo,
                    isExpanded = uiState.isFullInfoExpanded,
                    onToggleExpand = onToggleFullInfo,
                )
            }

            // Card 5: Files List (with per-file download buttons), only the picked ones on Real-Debrid
            val downloadableFiles = item.files.filter { it.selected }
            if (downloadableFiles.isNotEmpty() && !item.needsFileSelection) {
                TorrentFilesCard(
                    files = downloadableFiles,
                    // on Real-Debrid the files come together, the list is read-only
                    isCached = item.cached && capabilities.perFileLinks,
                    filterVideo = filterVideo,
                    onFilterVideoChange = { filterVideo = it },
                    modifier = Modifier.bringIntoViewRequester(filesRequester),
                    filterFilesFullPath = filterFilesFullPath,
                    onDownloadFile = onDownloadFile,
                    // TorBox can unlock single files, pick several to avoid a zip
                    onUnlockFiles = onUnlockFiles.takeIf { capabilities.perFileLinks && item.cached },
                    unlockProgress = uiState.unlockProgress,
                )
            }

            // Bottom spacer for FAB
            Spacer(modifier = Modifier.height(88.dp))
        }
    }

    (uiState.requeue as? RequeueState.Picking)?.let { picking ->
        FileSelectionDialog(
            name = picking.newTorrent.name,
            files = picking.newTorrent.files,
            remaining = 0,
            onConfirm = onConfirmRequeue,
            onLater = onCancelRequeue,
            onSelectAllRemaining = {},
            onDelete = {},
            initialSelected = picking.preselected,
            showDelete = false,
        )
    }

    uiState.pendingDownload?.let { pending ->
        DownloadZipModal(
            itemName = pending.fileName ?: item.name,
            alwaysZipped = pending.fileId == null && item.files.size > 1,
            onDismiss = onDismissDownloadModal,
            onConfirm = onConfirmDownloadModal,
        )
    }

    pendingPlayUrl?.let { url ->
        PlayChoiceDialog(
            services = mediaPlayers,
            onSelect = { selected, setAsDefault ->
                onPlayMedia(selected, url, setAsDefault)
                pendingPlayUrl = null
            },
            onDismiss = { pendingPlayUrl = null },
        )
    }

    if (uiState.showExportDialog) {
        TorrentExportDialog(
            context = context,
            onDismiss = { onShowExportDialog(false) },
            onExportMagnet = onExportMagnet,
            onExportTorrentFile = onExportTorrentFile,
        )
    }

    if (uiState.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { onShowDeleteDialog(false) },
            title = { Text(text = stringResource(id = R.string.delete_torrent_confirm_title)) },
            text = { Text(text = stringResource(id = R.string.delete_torrent_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDelete,
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Text(text = stringResource(id = R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowDeleteDialog(false) }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            },
        )
    }
}

private fun DebridTorrent.toOverviewInfo() =
    OverviewInfo(
        titleRes = R.string.torrent_overview,
        name = name,
        sizeBytes = size,
        downloadState = downloadState,
        active = active,
        cached = cached,
        createdAt = createdAt,
        expiresAt = expiresAt,
        link = magnet?.takeIf { it.isNotBlank() } ?: "magnet:?xt=urn:btih:$hash",
        linkLabelRes = R.string.magnet_link,
        copyLinkDescriptionRes = R.string.copy_magnet,
        showLinkText = false,
    )

@Composable
private fun TorrentTransferProgressCard(
    item: DebridTorrent,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(id = R.string.torrent_transfer_progress),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            // Progress bar with percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.progress),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "${(item.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            LinearWavyProgressIndicator(
                progress = { item.progress.toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.download_speed),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = formatSpeed(item.downloadSpeed),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.upload_speed),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = formatSpeed(item.uploadSpeed ?: 0),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.torrent_seeds_peers),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.seeds_peers_count, item.seeds ?: 0, item.peers ?: 0),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.ratio),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = item.ratio?.let { String.format(Locale.US, "%.2f", it) } ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.eta),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = if (item.downloadFinished || item.progress >= 1.0) {
                        stringResource(R.string.finished)
                    } else if ((item.eta ?: 0) > 0) {
                        stringResource(R.string.eta_seconds, item.eta ?: 0)
                    } else {
                        "—"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}


@Composable
private fun TorrentDownloadLinkCard(
    result: DownloadLinkResult,
    context: Context,
    modifier: Modifier = Modifier,
    defaultPlayer: ExternalService? = null,
    onPlay: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_download),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(id = R.string.download_link_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            val displayTitle = result.title.substringAfterLast('/').substringAfterLast('\\')
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )

            DownloadLinkTypeRow(isZipped = result.isZipped, fileType = result.fileType)

            // Security Warning Banner, the link includes the API token
            if (result.containsToken) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_warning),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(id = R.string.download_link_api_key_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = result.url,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(8.dp),
                )
            }

            DownloadLinkActions(
                url = result.url,
                title = displayTitle,
                streamingPageUrl = result.streamingPageUrl,
                defaultPlayer = defaultPlayer,
                onPlay = onPlay,
                context = context,
            )
        }
    }
}

// small lists don't need a search field
private const val UNLOCKED_SEARCH_MIN_FILES = 5

/** The files of a multi-link torrent, each with the actions of a generated link. */
@Composable
private fun UnlockedFilesCard(
    files: List<UnlockedFile>,
    defaultPlayer: ExternalService?,
    onPlay: (String) -> Unit,
    onCopyAll: () -> Unit,
    context: Context,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    // TorBox links include the API token
    showTokenWarning: Boolean = false,
) {
    var query by rememberSaveable { mutableStateOf("") }
    // only one file shows its actions, so long lists stay short
    var expandedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    val shown =
        remember(files, query) {
            val clean = query.trim()
            if (clean.isEmpty()) files else files.filter { it.name.contains(clean, ignoreCase = true) }
        }

    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.unlocked_files_title, files.size),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onCopyAll) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_copy),
                        contentDescription = stringResource(id = R.string.copy_all_links),
                        modifier = Modifier.size(20.dp),
                    )
                }
                onClear?.let { clear ->
                    IconButton(onClick = clear) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = stringResource(id = R.string.clear_selection),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            if (showTokenWarning) {
                Text(
                    text = stringResource(id = R.string.download_link_api_key_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (files.size > UNLOCKED_SEARCH_MIN_FILES) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(id = R.string.search_files_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon =
                        if (query.isNotEmpty()) {
                            {
                                IconButton(onClick = { query = "" }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_close),
                                        contentDescription = stringResource(id = R.string.clear_search),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                )
            }

            shown.forEachIndexed { index, file ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val expanded = expandedUrl == file.url
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { expandedUrl = if (expanded) null else file.url },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            file.size?.let { size ->
                                Text(
                                    text = convertToSize(size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Icon(
                            painter =
                                painterResource(
                                    id = if (expanded) R.drawable.ic_arrow_drop_up else R.drawable.ic_arrow_drop_down,
                                ),
                            contentDescription = null,
                        )
                    }
                    AnimatedVisibility(visible = expanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DownloadLinkTypeRow(isZipped = false, fileType = file.fileType)
                            DownloadLinkActions(
                                url = file.url,
                                title = file.name,
                                streamingPageUrl = file.streamingPageUrl,
                                defaultPlayer = defaultPlayer,
                                onPlay = { onPlay(file.url) },
                                context = context,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TorrentActionsCard(
    item: DebridTorrent,
    uiState: TorrentDetailUiState,
    capabilities: DebridCapabilities,
    onUnlockAll: () -> Unit,
    onRequeue: () -> Unit,
    onRefresh: () -> Unit,
    onDownloadZip: () -> Unit,
    modifier: Modifier = Modifier,
    onDownloadDirect: (Int, String) -> Unit = { _, _ -> },
    // several videos: turn on the Video filter and scroll to the file list
    onShowVideos: () -> Unit = {},
    downloadZipMode: DownloadZipMode = DownloadZipMode.ASK,
    onShowExportDialog: () -> Unit,
    onToggleFullInfo: () -> Unit,
    onDelete: () -> Unit,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // nothing to refresh once the item is cached or fully downloaded
            val isRefreshable = !(item.cached || item.downloadFinished || item.progress >= 1.0)

            Text(
                text = stringResource(id = R.string.actions),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            val selectedFiles = item.files.filter { it.selected }
            // without zips a whole download works only with a single link, otherwise each file gets its own
            val wholeAsLinks =
                !capabilities.zipDownloads && selectedFiles.size > 1 && item.hosterLinks.size != 1
            if (wholeAsLinks) {
                val progress = uiState.unlockProgress
                FilledTonalButton(
                    onClick = onUnlockAll,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = item.cached && progress == null && !uiState.isDeleting,
                ) {
                    if (progress != null) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_download),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text =
                            when {
                                progress != null -> stringResource(id = R.string.unlocking_progress, progress.first, progress.second)
                                uiState.unlockedFiles.isNotEmpty() -> stringResource(id = R.string.unlock_again)
                                else -> stringResource(id = R.string.unlock_all_files)
                            },
                    )
                }
            } else FilledTonalButton(
                onClick = onDownloadZip,
                modifier = Modifier.fillMaxWidth(),
                enabled = item.cached && !uiState.isDownloading && !uiState.isDeleting,
            ) {
                if (uiState.isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_download),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                val isZip =
                    capabilities.zipDownloads &&
                        wholeDownloadIsZipped(downloadZipMode, item.files, { it.name }, { it.mimetype })
                Text(text = stringResource(id = if (isZip) R.string.action_generate_zip else R.string.action_download))
            }

            // a full download with several files is zipped: offer the only video directly, or point to the file list
            when (
                val videos =
                    if (item.cached && capabilities.perFileLinks) multiFileVideos(selectedFiles, { it.name }, { it.mimetype }) else null
            ) {
                is MultiFileVideos.Single -> {
                    val video = videos.file
                    SingleVideoShortcut(
                        fileName = video.name,
                        sizeText = convertToSize(video.size),
                        enabled = !uiState.isDownloading && !uiState.isDeleting,
                        onClick = { onDownloadDirect(video.id, video.name) },
                    )
                }

                is MultiFileVideos.Many -> {
                    MultipleVideosNotice(
                        count = videos.count,
                        enabled = !uiState.isDeleting,
                        onShowVideos = onShowVideos,
                    )
                }

                null -> {}
            }

            if (!item.cached && !item.needsFileSelection) {
                Text(
                    text = stringResource(id = R.string.download_not_cached_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Export Button
            if (item.cached.not() && capabilities.exportTorrent) {
                OutlinedButton(
                    onClick = onShowExportDialog,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isExporting && !uiState.isDeleting,
                ) {
                    if (uiState.isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_share),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.action_export))
                }
            }

            if (item.cached.not() && capabilities.fullTorrentInfo) {
                // Full Info Button
                OutlinedButton(
                    onClick = onToggleFullInfo,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoadingFullInfo && !uiState.isDeleting,
                ) {
                    if (uiState.isLoadingFullInfo) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_info),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.fullInfo == null) {
                            stringResource(id = R.string.action_full_info)
                        } else if (uiState.isFullInfoExpanded) {
                            stringResource(R.string.hide_full_info)
                        } else {
                            stringResource(R.string.show_full_info)
                        },
                    )
                }
            }

            // Real-Debrid can't change the files of a torrent, it is added again instead
            if (capabilities.reselectFiles && !item.needsFileSelection) {
                val requeueing = uiState.requeue != null
                OutlinedButton(
                    onClick = onRequeue,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !requeueing && !uiState.isDeleting,
                ) {
                    if (requeueing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.requeue_choose_files))
                }
            }

            if (isRefreshable) {
                // Refresh Button
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isRefreshing && !uiState.isDeleting,
                ) {
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_refresh),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.action_refresh))
                }
            }

            // Delete Button
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isDeleting,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                if (uiState.isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.action_delete))
            }
        }
    }
}

@Composable
private fun TorrentFullInfoCard(
    fullInfo: TorrentFullInfo,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(id = R.string.full_info_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                IconButton(onClick = onToggleExpand) {
                    Icon(
                        painter = painterResource(
                            id = if (isExpanded) {
                                R.drawable.ic_arrow_drop_up
                            } else {
                                R.drawable.ic_arrow_drop_down
                            },
                        ),
                        contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(id = R.string.swarm_seeds),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "${fullInfo.seeds}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(id = R.string.swarm_peers),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "${fullInfo.peers}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    if (fullInfo.trackers.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Text(
                            text = "${stringResource(id = R.string.trackers_label)} (${fullInfo.trackers.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )

                        fullInfo.trackers.forEach { tracker ->
                            Text(
                                text = tracker,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TorrentExportDialog(
    context: Context,
    onDismiss: () -> Unit,
    onExportMagnet: (Context) -> Unit,
    onExportTorrentFile: (Context) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.export_dialog_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onExportMagnet(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_magnet),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.export_as_magnet))
                }

                OutlinedButton(
                    onClick = { onExportTorrentFile(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_download),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.export_as_file))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        },
    )
}

private fun formatFileNameDisplay(fullName: String, shortName: String): String {
    val rawName = shortName.ifBlank { fullName.substringAfterLast('/') }
    return if (rawName.length > 90) "…${rawName.takeLast(85)}" else rawName
}

private fun formatFilePathDisplay(fullName: String, shortName: String): String {
    val normalized = fullName.replace('\\', '/').trim()
    val fileName = shortName.ifBlank { normalized.substringAfterLast('/') }
    val rawPath =
        if (fileName.isNotEmpty() && normalized.endsWith(fileName)) {
            normalized.removeSuffix(fileName).trimEnd('/')
        } else {
            normalized.substringBeforeLast('/', "")
        }
    return rawPath.ifBlank { "/" }
}

private fun DebridFile.isVideo(): Boolean = isVideoFile(name, mimetype)

private fun DebridFile.isAudio(): Boolean = isAudioFile(name, mimetype)

private fun DebridFile.isSubtitle(): Boolean = isSubtitleFile(name)

@Composable
private fun TorrentFilesCard(
    files: List<DebridFile>,
    isCached: Boolean,
    filterVideo: Boolean,
    onFilterVideoChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    filterFilesFullPath: Boolean = false,
    onDownloadFile: (Int, String) -> Unit,
    // set when single files can be unlocked together, enables the selection mode
    onUnlockFiles: ((Set<Int>) -> Unit)? = null,
    unlockProgress: Pair<Int, Int>? = null,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectionMode by rememberSaveable(files) { mutableStateOf(false) }
    var selected by rememberSaveable(files) { mutableStateOf(emptySet<Int>()) }
    val canSelect = isCached && onUnlockFiles != null
    var filterAudio by rememberSaveable { mutableStateOf(false) }
    var filterSubtitles by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val filteredFiles =
        remember(files, searchQuery, filterFilesFullPath, filterVideo, filterAudio, filterSubtitles) {
            files.filter { file ->
                val fileNameOnly =
                    file.shortName.ifBlank {
                        file.name.substringAfterLast('/').substringAfterLast('\\')
                    }
                val matchesSearch =
                    searchQuery.isBlank() ||
                        fileNameOnly.contains(searchQuery, ignoreCase = true) ||
                        (filterFilesFullPath && file.name.contains(searchQuery, ignoreCase = true))

                val anyFilterActive = filterVideo || filterAudio || filterSubtitles
                val matchesType =
                    !anyFilterActive ||
                        (filterVideo && file.isVideo()) ||
                        (filterAudio && file.isAudio()) ||
                        (filterSubtitles && file.isSubtitle())

                matchesSearch && matchesType
            }
        }

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val isFiltered =
                searchQuery.isNotBlank() || filterVideo || filterAudio || filterSubtitles
            val countText =
                if (isFiltered) "${filteredFiles.size}/${files.size}" else "${files.size}"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${stringResource(id = R.string.torrent_files_title)} ($countText)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (canSelect) {
                    TextButton(
                        onClick = {
                            selectionMode = !selectionMode
                            if (!selectionMode) selected = emptySet()
                        },
                    ) {
                        if (!selectionMode) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(text = stringResource(id = if (selectionMode) R.string.cancel else R.string.select_files))
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(text = stringResource(id = R.string.search_files_hint)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_manage_search),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = stringResource(id = R.string.clear_search),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                FilterChip(
                    selected = filterVideo,
                    onClick = { onFilterVideoChange(!filterVideo) },
                    label = { Text(text = stringResource(id = R.string.filter_video)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_movie),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )

                FilterChip(
                    selected = filterAudio,
                    onClick = { filterAudio = !filterAudio },
                    label = { Text(text = stringResource(id = R.string.filter_audio)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_audiotrack),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )

                FilterChip(
                    selected = filterSubtitles,
                    onClick = { filterSubtitles = !filterSubtitles },
                    label = { Text(text = stringResource(id = R.string.filter_subtitles)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_subtitles),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }

            if (canSelect && selectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.selection_count, selected.size),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { selected = selected + filteredFiles.map { it.id } }) {
                        Text(text = stringResource(id = R.string.select_shown))
                    }
                    TextButton(onClick = { selected = emptySet() }, enabled = selected.isNotEmpty()) {
                        Text(text = stringResource(id = R.string.clear_selection))
                    }
                }
                FilledTonalButton(
                    onClick = {
                        onUnlockFiles?.invoke(selected)
                        selected = emptySet()
                        selectionMode = false
                    },
                    enabled = selected.isNotEmpty() && unlockProgress == null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (unlockProgress != null) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text =
                                stringResource(
                                    id = R.string.unlocking_progress,
                                    unlockProgress.first,
                                    unlockProgress.second,
                                ),
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_download),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = R.string.unlock_selected, selected.size))
                    }
                }
            }

            if (filteredFiles.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.no_files_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                filteredFiles.forEachIndexed { index, file ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    val inSelection = canSelect && selectionMode
                    val isChecked = file.id in selected
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .then(
                                    if (inSelection) {
                                        Modifier.clickable {
                                            selected = if (isChecked) selected - file.id else selected + file.id
                                        }
                                    } else {
                                        Modifier
                                    },
                                ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (inSelection) {
                            Checkbox(checked = isChecked, onCheckedChange = null)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = formatFileNameDisplay(file.name, file.shortName),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = convertToSize(file.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                detectFileTypeCategory(file.name, file.mimetype)?.let { category ->
                                    FileTypeBadge(category = category)
                                }
                            }
                            Text(
                                text = formatFilePathDisplay(file.name, file.shortName),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (isCached && !inSelection) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    onDownloadFile(
                                        file.id,
                                        file.shortName.ifBlank {
                                            file.name.substringAfterLast('/')
                                                .substringAfterLast('\\')
                                        },
                                    )
                                },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_download),
                                    contentDescription = "Download ${file.name}",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun TorrentDetailScreenPreview() {
    NullSeekTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            TorrentDetailScreen(
                item = mockTorrents.first(),
                uiState = TorrentDetailUiState(torrent = mockTorrents.first()),
                defaultPlayer = null,
                mediaPlayers = emptyList(),
                onPlayMedia = { _, _, _ -> },
                onRefresh = {},
                onDownloadZip = {},
                onDownloadFile = { _, _ -> },
                onDismissDownloadLink = {},
                onConfirmDownloadModal = { _, _ -> },
                onDismissDownloadModal = {},
                onShowExportDialog = {},
                onExportMagnet = {},
                onExportTorrentFile = {},
                onToggleFullInfo = {},
                onShowDeleteDialog = {},
                onConfirmDelete = {},
            )
        }
    }
}