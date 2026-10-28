package com.github.livingwithhippos.nullseek.tv.ui.lists

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.utils.convertToSize
import com.github.livingwithhippos.nullseek.tv.ui.components.ConfirmDialog
import com.github.livingwithhippos.nullseek.tv.ui.components.LocalMessenger
import com.github.livingwithhippos.nullseek.tv.ui.components.TvActionButton
import com.github.livingwithhippos.nullseek.ui.lists.usenet.UsenetDetailEvent
import com.github.livingwithhippos.nullseek.ui.lists.usenet.UsenetDetailViewModel
import com.github.livingwithhippos.torbox.model.UsenetDownloadDto
import org.koin.androidx.compose.koinViewModel

@Composable
fun TvUsenetDetailScreen(
    item: UsenetDownloadDto,
    onClose: () -> Unit,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleted: () -> Unit = onClose,
    viewModel: UsenetDetailViewModel = koinViewModel(),
) {
    BackHandler(onBack = onClose)
    LaunchedEffect(item.id) { viewModel.init(item) }

    val messenger = LocalMessenger.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val defaultPlayer by viewModel.defaultMediaPlayer.collectAsStateWithLifecycle()
    val mediaPlayers by viewModel.mediaPlayerServices.collectAsStateWithLifecycle()
    val filterFilesFullPath by viewModel.filterFilesFullPath.collectAsStateWithLifecycle()
    val current = uiState.usenet?.takeIf { it.id == item.id } ?: item

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UsenetDetailEvent.ShowMessage -> messenger.show(event.message)
                UsenetDetailEvent.Deleted -> onDeleted()
            }
        }
    }

    val largestVideo = if (current.cached) current.files.map { it.toDetailFile() }.largestVideoForDirectLink() else null

    TvDetailPane(
        modifier = modifier,
        overview =
            DetailOverview(
                name = current.name,
                sizeBytes = current.size,
                state = current.downloadState,
                cached = current.cached,
                active = current.active,
                createdAt = current.createdAt,
                expiresAt = current.expiresAt,
                progress = current.progress,
                downloadSpeed = current.downloadSpeed,
                eta = current.eta,
                hash = current.hash,
                sourceLabel = stringResource(R.string.source_link_title),
                sourceLink = current.originalUrl,
                
            ),
        onClose = onClose,
        isFullScreen = isFullScreen,
        onToggleFullScreen = onToggleFullScreen,
        linkResult = uiState.downloadLinkResult?.let { LinkResult(it.title, it.url, it.isZipped, it.fileType) },
        onDismissLink = viewModel::dismissDownloadLink,
        defaultPlayer = defaultPlayer,
        mediaPlayers = mediaPlayers,
        onPlayMedia = viewModel::playOnMedia,
        files = current.files.map { it.toDetailFile() },
        filterFilesFullPath = filterFilesFullPath,
        onDownloadFile = if (current.cached && !uiState.isDeleting) { file -> viewModel.startDownload(file.id, file.name) } else null,
        actions = {
            TvActionButton(
                text = stringResource(R.string.action_download),
                icon = R.drawable.ic_download,
                primary = true,
                enabled = current.cached && !uiState.isDeleting,
                loading = uiState.isDownloading,
                onClick = { viewModel.startDownload() },
            )
            if (largestVideo != null) {
                TvActionButton(
                    text = stringResource(R.string.download_largest_video),
                    icon = R.drawable.ic_play_arrow,
                    enabled = !uiState.isDeleting,
                    loading = uiState.isDownloading,
                    onClick = { viewModel.downloadDirect(largestVideo.id, largestVideo.name) },
                )
            }
            TvActionButton(
                text = stringResource(R.string.action_refresh),
                icon = R.drawable.ic_refresh,
                loading = uiState.isRefreshing,
                enabled = !uiState.isDeleting,
                onClick = viewModel::refreshUsenet,
            )
            TvActionButton(
                text = stringResource(R.string.action_delete),
                icon = R.drawable.ic_delete,
                loading = uiState.isDeleting,
                onClick = { viewModel.showDeleteConfirmDialog(true) },
            )
        },
        extraSections = {
            if (largestVideo != null) {
                item {
                    Text(
                        text = stringResource(R.string.download_mixed_notice) + "\n" + largestVideo.displayName + " · " + convertToSize(largestVideo.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!current.cached) {
                item { Text(stringResource(R.string.usenet_not_cached_warning), color = MaterialTheme.colorScheme.error) }
            }
        },
    )

    uiState.pendingDownload?.let { pending ->
        DownloadZipDialog(
            itemName = pending.fileName ?: current.name,
            alwaysZipped = pending.fileId == null && current.files.size > 1,
            onConfirm = viewModel::confirmDownloadModal,
            onDismiss = viewModel::dismissDownloadModal,
        )
    }

    if (uiState.showDeleteConfirmDialog) {
        ConfirmDialog(
            title = stringResource(R.string.delete_usenet_confirm_title),
            message = stringResource(R.string.delete_usenet_confirm_message),
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                viewModel.showDeleteConfirmDialog(false)
                viewModel.deleteUsenet()
            },
            onDismiss = { viewModel.showDeleteConfirmDialog(false) },
            dismissText = stringResource(R.string.cancel),
        )
    }
}
