package com.github.livingwithhippos.nullseek.tv.ui.lists

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.tv.ui.components.InfoRow
import com.github.livingwithhippos.nullseek.tv.ui.components.LocalMessenger
import com.github.livingwithhippos.nullseek.tv.ui.components.SectionCard
import com.github.livingwithhippos.nullseek.tv.ui.components.TvActionButton
import com.github.livingwithhippos.nullseek.tv.ui.components.TvDialog
import com.github.livingwithhippos.nullseek.ui.lists.torrent.TorrentDetailEvent
import com.github.livingwithhippos.nullseek.ui.lists.torrent.TorrentDetailViewModel
import com.github.livingwithhippos.torbox.model.TorrentDto
import org.koin.androidx.compose.koinViewModel

@Composable
fun TvTorrentDetailScreen(
    item: TorrentDto,
    onClose: () -> Unit,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TorrentDetailViewModel = koinViewModel(),
) {
    BackHandler(onBack = onClose)
    LaunchedEffect(item.id) { viewModel.init(item) }

    val context = LocalContext.current
    val messenger = LocalMessenger.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val defaultPlayer by viewModel.defaultMediaPlayer.collectAsStateWithLifecycle()
    val mediaPlayers by viewModel.mediaPlayerServices.collectAsStateWithLifecycle()
    val filterFilesFullPath by viewModel.filterFilesFullPath.collectAsStateWithLifecycle()
    val torrent = uiState.torrent?.takeIf { it.id == item.id } ?: item

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TorrentDetailEvent.ShowMessage -> messenger.show(event.message)
            }
        }
    }

    TvDetailPane(
        modifier = modifier,
        overview =
            DetailOverview(
                name = torrent.name,
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
        linkResult = uiState.downloadLinkResult?.let { LinkResult(it.title, it.url) },
        onDismissLink = viewModel::dismissDownloadLink,
        defaultPlayer = defaultPlayer,
        mediaPlayers = mediaPlayers,
        onPlayMedia = viewModel::playOnMedia,
        files = torrent.files.orEmpty().map { it.toDetailFile() },
        filterFilesFullPath = filterFilesFullPath,
        onDownloadFile = if (torrent.cached) { file -> viewModel.startDownload(file.id, file.name) } else null,
        actions = {
            TvActionButton(
                text = stringResource(R.string.action_download),
                icon = R.drawable.ic_download,
                primary = true,
                enabled = torrent.cached,
                loading = uiState.isDownloading,
                onClick = { viewModel.startDownload() },
            )
            TvActionButton(
                text = stringResource(R.string.action_refresh),
                icon = R.drawable.ic_refresh,
                loading = uiState.isRefreshing,
                onClick = viewModel::refreshTorrent,
            )
            TvActionButton(
                text = stringResource(R.string.action_export),
                icon = R.drawable.ic_magnet,
                loading = uiState.isExporting,
                onClick = { viewModel.showExportDialog(true) },
            )
            TvActionButton(
                text = stringResource(R.string.action_full_info),
                icon = R.drawable.ic_info,
                loading = uiState.isLoadingFullInfo,
                onClick = viewModel::loadFullInfo,
            )
        },
        extraSections = {
            if (!torrent.cached) {
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

    uiState.pendingDownload?.let { pending ->
        DownloadZipDialog(
            itemName = pending.fileName ?: torrent.name,
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
}
