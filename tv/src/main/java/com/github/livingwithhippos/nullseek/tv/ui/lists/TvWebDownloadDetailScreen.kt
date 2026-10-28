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
import com.github.livingwithhippos.nullseek.tv.ui.components.ConfirmDialog
import com.github.livingwithhippos.nullseek.tv.ui.components.LocalMessenger
import com.github.livingwithhippos.nullseek.tv.ui.components.TvActionButton
import com.github.livingwithhippos.nullseek.ui.lists.web.WebDownloadDetailEvent
import com.github.livingwithhippos.nullseek.ui.lists.web.WebDownloadDetailViewModel
import com.github.livingwithhippos.torbox.model.WebDownloadDto
import org.koin.androidx.compose.koinViewModel

@Composable
fun TvWebDownloadDetailScreen(
    item: WebDownloadDto,
    onClose: () -> Unit,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleted: () -> Unit = onClose,
    viewModel: WebDownloadDetailViewModel = koinViewModel(),
) {
    BackHandler(onBack = onClose)
    LaunchedEffect(item.id) { viewModel.init(item) }

    val messenger = LocalMessenger.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val defaultPlayer by viewModel.defaultMediaPlayer.collectAsStateWithLifecycle()
    val mediaPlayers by viewModel.mediaPlayerServices.collectAsStateWithLifecycle()
    val filterFilesFullPath by viewModel.filterFilesFullPath.collectAsStateWithLifecycle()
    val current = uiState.webDownload?.takeIf { it.id == item.id } ?: item

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is WebDownloadDetailEvent.ShowMessage -> messenger.show(event.message)
                WebDownloadDetailEvent.Deleted -> onDeleted()
            }
        }
    }

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
                error = current.error,
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
        onDownloadDirect = if (current.cached && !uiState.isDeleting) { file -> viewModel.downloadDirect(file.id, file.name) } else null,
        isDownloading = uiState.isDownloading,
        actions = {
            TvActionButton(
                text = stringResource(R.string.action_download),
                icon = R.drawable.ic_download,
                primary = true,
                enabled = current.cached && !uiState.isDeleting,
                loading = uiState.isDownloading,
                onClick = { viewModel.startDownload() },
            )
            // nothing to refresh once the item is cached or fully downloaded
            if (!(current.cached || current.downloadFinished || current.progress >= 1.0)) {
                TvActionButton(
                    text = stringResource(R.string.action_refresh),
                    icon = R.drawable.ic_refresh,
                    loading = uiState.isRefreshing,
                    enabled = !uiState.isDeleting,
                    onClick = viewModel::refreshWebDownload,
                )
            }
            TvActionButton(
                text = stringResource(R.string.action_delete),
                icon = R.drawable.ic_delete,
                loading = uiState.isDeleting,
                onClick = { viewModel.showDeleteConfirmDialog(true) },
            )
        },
        extraSections = {
            if (!current.cached) {
                item { Text(stringResource(R.string.web_download_not_cached_warning), color = MaterialTheme.colorScheme.error) }
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
            title = stringResource(R.string.delete_web_download_confirm_title),
            message = stringResource(R.string.delete_web_download_confirm_message),
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                viewModel.showDeleteConfirmDialog(false)
                viewModel.deleteWebDownload()
            },
            onDismiss = { viewModel.showDeleteConfirmDialog(false) },
            dismissText = stringResource(R.string.cancel),
        )
    }
}
