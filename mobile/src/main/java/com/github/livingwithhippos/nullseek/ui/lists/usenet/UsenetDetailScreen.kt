package com.github.livingwithhippos.nullseek.ui.lists.usenet

import android.content.Context
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.github.livingwithhippos.nullseek.ui.theme.Button
import com.github.livingwithhippos.nullseek.ui.theme.FilledTonalButton
import com.github.livingwithhippos.nullseek.ui.theme.FilterChip
import com.github.livingwithhippos.nullseek.ui.theme.OutlinedButton
import com.github.livingwithhippos.nullseek.ui.theme.TextButton
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
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.core.utils.mockUsenetDownload
import com.github.livingwithhippos.nullseek.data.model.ExternalService
import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import com.github.livingwithhippos.nullseek.ui.externalservices.PlayChoiceDialog
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.utils.CopyButton
import com.github.livingwithhippos.nullseek.ui.utils.DownloadZipModal
import com.github.livingwithhippos.nullseek.ui.utils.FileTypeBadge
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import com.github.livingwithhippos.nullseek.core.utils.convertToSize
import com.github.livingwithhippos.nullseek.utils.detectFileTypeCategory
import com.github.livingwithhippos.nullseek.core.utils.formatToLocalDate
import com.github.livingwithhippos.nullseek.utils.isAudioFile
import com.github.livingwithhippos.nullseek.utils.isStreamableOrSubtitle
import com.github.livingwithhippos.nullseek.utils.isSubtitleFile
import com.github.livingwithhippos.nullseek.utils.isVideoFile
import com.github.livingwithhippos.nullseek.utils.openWebPage
import com.github.livingwithhippos.nullseek.utils.openWith
import com.github.livingwithhippos.nullseek.utils.setClipboardText
import com.github.livingwithhippos.nullseek.utils.shareText
import com.github.livingwithhippos.torbox.model.UsenetDownloadDto
import com.github.livingwithhippos.torbox.model.UsenetDownloadFileDto
import org.koin.androidx.compose.koinViewModel

@Composable
fun UsenetDetailScreen(
    modifier: Modifier = Modifier,
    contentType: NullContentType,
    navigationType: NullNavigationType,
    item: UsenetDownloadDto,
    closeDetails: () -> Unit,
    viewModel: UsenetDetailViewModel = koinViewModel(),
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
    val currentItem = uiState.usenet ?: item
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UsenetDetailEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is UsenetDetailEvent.Deleted -> {
                    closeDetails()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        UsenetDetailScreen(
            modifier = Modifier.padding(innerPadding),
            context = LocalContext.current,
            item = currentItem,
            uiState = uiState,
            defaultPlayer = defaultPlayer,
            mediaPlayers = mediaPlayers,
            onPlayMedia = { service, url, setAsDefault ->
                viewModel.playOnMedia(service, url, setAsDefault)
            },
            onRefresh = { viewModel.refreshUsenet() },
            onDownloadZip = { viewModel.startDownload() },
            onDownloadFile = { fileId, fileName -> viewModel.startDownload(fileId, fileName) },
            onDismissDownloadLink = { viewModel.dismissDownloadLink() },
            onConfirmDownloadModal = { mode, rememberChoice ->
                viewModel.confirmDownloadModal(mode, rememberChoice)
            },
            onDismissDownloadModal = { viewModel.dismissDownloadModal() },
            onShowDeleteDialog = { viewModel.showDeleteConfirmDialog(it) },
            onConfirmDelete = { viewModel.deleteUsenet() },
        )
    }
}

@Composable
private fun UsenetDetailScreen(
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
    item: UsenetDownloadDto,
    uiState: UsenetDetailUiState,
    defaultPlayer: ExternalService? = null,
    mediaPlayers: List<ExternalService> = emptyList(),
    onPlayMedia: (ExternalService, String, Boolean) -> Unit = { _, _, _ -> },
    onRefresh: () -> Unit,
    onDownloadZip: () -> Unit,
    onDownloadFile: (Int, String) -> Unit,
    onDismissDownloadLink: () -> Unit,
    onConfirmDownloadModal: (DownloadZipMode, Boolean) -> Unit,
    onDismissDownloadModal: () -> Unit,
    onShowDeleteDialog: (Boolean) -> Unit,
    onConfirmDelete: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var showPlayChoiceDialog by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
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
            modifier =
                Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Card 1: Overview (with integrated Source Link)
            UsenetOverviewCard(item = item, context = context)

            // Card 2: Transfer & Progress (hide if cached and completed)
            val isCompleted = item.progress >= 1.0 || item.downloadFinished
            if (!item.cached || !isCompleted) {
                UsenetTransferProgressCard(item = item)
            }

            // Card 3: Download Link Card
            if (uiState.downloadLinkResult != null) {
                UsenetDownloadLinkCard(
                    result = uiState.downloadLinkResult,
                    context = context,
                    defaultPlayer = defaultPlayer,
                    onPlay = {
                        if (defaultPlayer != null) {
                            onPlayMedia(defaultPlayer, uiState.downloadLinkResult.url, false)
                        } else {
                            showPlayChoiceDialog = true
                        }
                    },
                    onDismiss = onDismissDownloadLink,
                )
            }

            // Card 4: Actions
            UsenetActionsCard(
                item = item,
                uiState = uiState,
                onRefresh = onRefresh,
                onDownloadZip = onDownloadZip,
                onDelete = { onShowDeleteDialog(true) },
            )

            // Card 5: Files List
            if (item.files.isNotEmpty()) {
                UsenetFilesCard(
                    files = item.files,
                    isCached = item.cached,
                    onDownloadFile = onDownloadFile,
                )
            }

            // Bottom spacer for FAB
            Spacer(modifier = Modifier.height(88.dp))
        }
    }

    if (showPlayChoiceDialog && uiState.downloadLinkResult != null) {
        PlayChoiceDialog(
            services = mediaPlayers,
            onDismiss = { showPlayChoiceDialog = false },
            onSelect = { service, setAsDefault ->
                showPlayChoiceDialog = false
                onPlayMedia(service, uiState.downloadLinkResult.url, setAsDefault)
            },
        )
    }

    if (uiState.pendingDownload != null) {
        DownloadZipModal(
            itemName = uiState.pendingDownload.fileName ?: item.name,
            onDismiss = onDismissDownloadModal,
            onConfirm = onConfirmDownloadModal,
        )
    }

    if (uiState.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { onShowDeleteDialog(false) },
            title = { Text(text = stringResource(id = R.string.delete_usenet_confirm_title)) },
            text = { Text(text = stringResource(id = R.string.delete_usenet_confirm_message)) },
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
private fun UsenetOverviewCard(
    item: UsenetDownloadDto,
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
                text = stringResource(id = R.string.usenet_overview),
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
                    text = stringResource(id = R.string.hash),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Text(
                        text = item.hash,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    CopyButton(
                        text = item.hash,
                        description = "Copy Hash",
                        context = context,
                    )
                }
            }

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
                    text = "State",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text =
                        buildString {
                            append(if (item.active) "Active" else "Inactive")
                            if (item.cached) append(" • Cached")
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
                        text = "Expires",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = formatToLocalDate(item.expiresAt),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            if (item.originalUrl.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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
                            text = stringResource(id = R.string.source_link_title),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row {
                            IconButton(
                                onClick = { setClipboardText(context, item.originalUrl) },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_copy),
                                    contentDescription = stringResource(id = R.string.copy_source_link),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { shareText(context, item.originalUrl, item.name) },
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
}

@Composable
private fun UsenetTransferProgressCard(
    item: UsenetDownloadDto,
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

            LinearProgressIndicator(
                progress = { item.progress.toFloat().coerceIn(0f, 1f) },
                modifier =
                    Modifier
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
                    text = "${item.downloadSpeed} kB/s",
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
                    text =
                        if (item.downloadFinished || item.progress >= 1.0) {
                            "Finished"
                        } else if (item.eta > 0) {
                            "${item.eta}s"
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
private fun UsenetDownloadLinkCard(
    result: DownloadLinkResult,
    context: Context,
    defaultPlayer: ExternalService? = null,
    onPlay: () -> Unit = {},
    onDismiss: () -> Unit,
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
private fun UsenetActionsCard(
    item: UsenetDownloadDto,
    uiState: UsenetDetailUiState,
    onRefresh: () -> Unit,
    onDownloadZip: () -> Unit,
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
                    text = stringResource(id = R.string.usenet_not_cached_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

private fun formatFileNameDisplay(fullName: String, shortName: String): String {
    val rawName = shortName.ifBlank { fullName.substringAfterLast('/') }
    return if (rawName.length > 90) "…${rawName.takeLast(85)}" else rawName
}

private fun UsenetDownloadFileDto.isVideo(): Boolean = isVideoFile(name, mimetype)

private fun UsenetDownloadFileDto.isAudio(): Boolean = isAudioFile(name, mimetype)

private fun UsenetDownloadFileDto.isSubtitle(): Boolean = isSubtitleFile(name)

@Composable
private fun UsenetFilesCard(
    files: List<UsenetDownloadFileDto>,
    isCached: Boolean,
    onDownloadFile: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filterVideo by rememberSaveable { mutableStateOf(false) }
    var filterAudio by rememberSaveable { mutableStateOf(false) }
    var filterSubtitles by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val filteredFiles = remember(files, searchQuery, filterVideo, filterAudio, filterSubtitles) {
        files.filter { file ->
            val matchesSearch = searchQuery.isBlank() || file.name.contains(
                searchQuery,
                ignoreCase = true
            ) || file.shortName.contains(searchQuery, ignoreCase = true)

            val anyFilterActive = filterVideo || filterAudio || filterSubtitles
            val matchesType = !anyFilterActive ||
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
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )

            // Filter Chips for Video, Audio & Subtitles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                    colors = FilterChipDefaults.filterChipColors(),
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
                    colors = FilterChipDefaults.filterChipColors(),
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
                    colors = FilterChipDefaults.filterChipColors(),
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
fun UsenetDetailScreenPreview() {
    NullSeekTheme {
        Surface(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
        ) {
            UsenetDetailScreen(
                item = mockUsenetDownload,
                uiState = UsenetDetailUiState(usenet = mockUsenetDownload),
                defaultPlayer = null,
                mediaPlayers = emptyList(),
                onPlayMedia = { _, _, _ -> },
                onRefresh = {},
                onDownloadZip = {},
                onDownloadFile = { _, _ -> },
                onDismissDownloadLink = {},
                onConfirmDownloadModal = { _, _ -> },
                onDismissDownloadModal = {},
                onShowDeleteDialog = {},
                onConfirmDelete = {},
            )
        }
    }
}