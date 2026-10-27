package com.github.livingwithhippos.nullseek.ui.lists.torrent

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.github.livingwithhippos.nullseek.core.utils.formatToLocalDate
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
import com.github.livingwithhippos.nullseek.ui.utils.DownloadZipModal
import com.github.livingwithhippos.nullseek.ui.utils.FileTypeBadge
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import com.github.livingwithhippos.nullseek.utils.detectFileTypeCategory
import com.github.livingwithhippos.nullseek.utils.isAudioFile
import com.github.livingwithhippos.nullseek.utils.isStreamableOrSubtitle
import com.github.livingwithhippos.nullseek.utils.isSubtitleFile
import com.github.livingwithhippos.nullseek.utils.isVideoFile
import com.github.livingwithhippos.nullseek.utils.openWebPage
import com.github.livingwithhippos.nullseek.utils.openWith
import com.github.livingwithhippos.nullseek.utils.setClipboardText
import com.github.livingwithhippos.nullseek.utils.shareText
import com.github.livingwithhippos.torbox.model.TorrentDto
import com.github.livingwithhippos.torbox.model.TorrentFileDto
import com.github.livingwithhippos.torbox.model.TorrentInfoDto
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun TorrentDetailScreen(
    modifier: Modifier = Modifier,
    contentType: NullContentType,
    navigationType: NullNavigationType,
    item: TorrentDto,
    closeDetails: () -> Unit,
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
    val currentItem = uiState.torrent ?: item
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TorrentDetailEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                TorrentDetailEvent.Deleted -> {
                    closeDetails()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        TorrentDetailScreen(
            modifier = Modifier.padding(innerPadding),
            context = LocalContext.current,
            item = currentItem,
            uiState = uiState,
            defaultPlayer = defaultPlayer,
            mediaPlayers = mediaPlayers,
            filterFilesFullPath = filterFilesFullPath,
            onPlayMedia = { service, url, setAsDefault ->
                viewModel.playOnMedia(service, url, setAsDefault)
            },
            onRefresh = { viewModel.refreshTorrent() },
            onDownloadZip = { viewModel.startDownload() },
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
    }
}

@Composable
private fun TorrentDetailScreen(
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    item: TorrentDto,
    uiState: TorrentDetailUiState,
    defaultPlayer: ExternalService?,
    mediaPlayers: List<ExternalService>,
    filterFilesFullPath: Boolean = false,
    onPlayMedia: (ExternalService, String, Boolean) -> Unit,
    onRefresh: () -> Unit,
    onDownloadZip: () -> Unit,
    onDownloadFile: (Int, String) -> Unit,
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
    var showPlayChoiceDialog by remember { mutableStateOf(false) }

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
            TorrentOverviewCard(item = item, context = context)

            // Card 2: Transfer & Progress (hidden if cached at 100% progress)
            val isCachedAndComplete = item.cached && (item.progress >= 1.0 || item.downloadFinished)
            if (!isCachedAndComplete) {
                TorrentTransferProgressCard(item = item)
            }

            // Section: Generated Download Link (Options: Open, Copy, Share)
            uiState.downloadLinkResult?.let { linkResult ->
                TorrentDownloadLinkCard(
                    result = linkResult,
                    context = context,
                    defaultPlayer = defaultPlayer,
                    onPlay = {
                        if (defaultPlayer != null) {
                            onPlayMedia(defaultPlayer, linkResult.url, false)
                        } else {
                            showPlayChoiceDialog = true
                        }
                    },
                    onDismiss = onDismissDownloadLink,
                )
            }

            // Card 4: Actions (Refresh, Download, Export, Full Info)
            TorrentActionsCard(
                item = item,
                uiState = uiState,
                onRefresh = onRefresh,
                onDownloadZip = onDownloadZip,
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

            // Card 5: Files List (with per-file download buttons)
            if (item.files.isNotEmpty()) {
                TorrentFilesCard(
                    files = item.files,
                    isCached = item.cached,
                    filterFilesFullPath = filterFilesFullPath,
                    onDownloadFile = onDownloadFile,
                )
            }

            // Bottom spacer for FAB
            Spacer(modifier = Modifier.height(88.dp))
        }
    }

    uiState.pendingDownload?.let { pending ->
        DownloadZipModal(
            itemName = pending.fileName ?: item.name,
            onDismiss = onDismissDownloadModal,
            onConfirm = onConfirmDownloadModal,
        )
    }

    val playLink = uiState.downloadLinkResult
    if (showPlayChoiceDialog && playLink != null) {
        PlayChoiceDialog(
            services = mediaPlayers,
            onSelect = { selected, setAsDefault ->
                onPlayMedia(selected, playLink.url, setAsDefault)
                showPlayChoiceDialog = false
            },
            onDismiss = { showPlayChoiceDialog = false },
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

@Composable
private fun TorrentOverviewCard(
    item: TorrentDto,
    context: Context,
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
                text = stringResource(id = R.string.torrent_overview),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.size),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = convertToSize(item.size),
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
                    text = stringResource(id = R.string.status),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = item.downloadState.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.state),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = buildString {
                        append(if (item.active) stringResource(R.string.active) else stringResource(R.string.inactive))
                        if (item.cached) append(" • " + stringResource(R.string.cached))
                    },
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
                    text = stringResource(id = R.string.added_on),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = formatToLocalDate(item.createdAt),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (item.expiresAt != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.expires),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = formatToLocalDate(item.expiresAt),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            val effectiveMagnet =
                item.magnet?.takeIf { it.isNotBlank() } ?: "magnet:?xt=urn:btih:${item.hash}"

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.magnet_link),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row {
                        IconButton(
                            onClick = { setClipboardText(context, effectiveMagnet) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_copy),
                                contentDescription = stringResource(id = R.string.copy_magnet),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { shareText(context, effectiveMagnet, item.name) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_share),
                                contentDescription = stringResource(id = R.string.action_share),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TorrentTransferProgressCard(
    item: TorrentDto,
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
                    text = stringResource(R.string.speed_kbps, item.downloadSpeed),
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
                    text = stringResource(R.string.speed_kbps, item.uploadSpeed),
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
                    text = stringResource(R.string.seeds_peers_count, item.seeds, item.peers),
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
                    text = String.format(Locale.US, "%.2f", item.ratio),
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
                    } else if (item.eta > 0) {
                        stringResource(R.string.eta_seconds, item.eta)
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

            // Security Warning Banner
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

            val isMedia = isStreamableOrSubtitle(displayTitle)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isMedia) {
                    Button(
                        onClick = onPlay,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_play_arrow),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val playText = defaultPlayer?.let {
                            stringResource(id = R.string.action_play_on, it.name)
                        } ?: stringResource(id = R.string.action_play_external)
                        Text(text = playText)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isMedia) {
                        FilledTonalButton(
                            onClick = { openWebPage(context, result.url) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_open_in_new),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(id = R.string.action_open_link))
                        }
                    } else {
                        Button(
                            onClick = { openWebPage(context, result.url) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_open_in_new),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(id = R.string.action_open_link))
                        }
                    }

                    val chooserTitle = stringResource(id = R.string.action_open_with)
                    FilledTonalButton(
                        onClick = {
                            openWith(
                                context = context,
                                url = result.url,
                                fileName = displayTitle,
                                chooserTitle = chooserTitle,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_apps),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(id = R.string.action_open_with))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            setClipboardText(context, result.url)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_copy),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(id = R.string.copy_link))
                    }

                    OutlinedButton(
                        onClick = {
                            shareText(context, result.url, title = displayTitle)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_share),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(id = R.string.action_share))
                    }
                }
            }
        }
    }
}

@Composable
private fun TorrentActionsCard(
    item: TorrentDto,
    uiState: TorrentDetailUiState,
    onRefresh: () -> Unit,
    onDownloadZip: () -> Unit,
    onShowExportDialog: () -> Unit,
    onToggleFullInfo: () -> Unit,
    onDelete: () -> Unit,
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
                text = stringResource(id = R.string.actions),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            // Refresh Button
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isRefreshing,
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

            // Download as Zip Button (Checks item.cached)
            FilledTonalButton(
                onClick = onDownloadZip,
                modifier = Modifier.fillMaxWidth(),
                enabled = item.cached && !uiState.isDownloading,
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
                Text(text = stringResource(id = R.string.action_download))
            }

            if (!item.cached) {
                Text(
                    text = stringResource(id = R.string.download_not_cached_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Export Button
            if (item.cached.not()) {
                OutlinedButton(
                    onClick = onShowExportDialog,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isExporting,
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

            if (item.cached.not()) {
                // Full Info Button
                OutlinedButton(
                    onClick = onToggleFullInfo,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoadingFullInfo,
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
    fullInfo: TorrentInfoDto,
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

private fun TorrentFileDto.isVideo(): Boolean = isVideoFile(name, mimetype)

private fun TorrentFileDto.isAudio(): Boolean = isAudioFile(name, mimetype)

private fun TorrentFileDto.isSubtitle(): Boolean = isSubtitleFile(name)

@Composable
private fun TorrentFilesCard(
    files: List<TorrentFileDto>,
    isCached: Boolean,
    modifier: Modifier = Modifier,
    filterFilesFullPath: Boolean = false,
    onDownloadFile: (Int, String) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filterVideo by rememberSaveable { mutableStateOf(false) }
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
            Text(
                text = "${stringResource(id = R.string.torrent_files_title)} ($countText)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

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
                    onClick = { filterVideo = !filterVideo },
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
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

                        if (isCached) {
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