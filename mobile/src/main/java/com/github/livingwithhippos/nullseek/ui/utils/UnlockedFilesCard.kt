package com.github.livingwithhippos.nullseek.ui.utils

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
import androidx.compose.ui.draw.alpha
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
import com.github.livingwithhippos.nullseek.core.domain.DebridCapabilities
import com.github.livingwithhippos.nullseek.core.domain.DebridFile
import com.github.livingwithhippos.nullseek.core.domain.DebridTorrent
import com.github.livingwithhippos.nullseek.core.domain.TorrentFullInfo
import com.github.livingwithhippos.nullseek.core.domain.needsFileSelection
import com.github.livingwithhippos.nullseek.core.utils.convertToSize
import com.github.livingwithhippos.nullseek.core.utils.formatSpeed
import com.github.livingwithhippos.nullseek.core.utils.mockTorrents
import com.github.livingwithhippos.nullseek.data.model.ExternalService
import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import com.github.livingwithhippos.nullseek.ui.externalservices.PlayChoiceDialog
import com.github.livingwithhippos.nullseek.ui.lists.UnlockedFile
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
import com.github.livingwithhippos.nullseek.ui.utils.FileSelectionCard
import com.github.livingwithhippos.nullseek.ui.utils.FileSelectionDialog
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
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

// small lists don't need a search field
private const val UNLOCKED_SEARCH_MIN_FILES = 5

/** The files of a multi-link torrent, each with the actions of a generated link. */
@Composable
fun UnlockedFilesCard(
    files: List<UnlockedFile>,
    defaultPlayer: ExternalService?,
    onPlay: (String) -> Unit,
    onCopyAll: () -> Unit,
    context: Context,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    // TorBox links include the API token
    showTokenWarning: Boolean = false,
    // saves an M3U8 playlist with the links
    onSavePlaylist: (() -> Unit)? = null,
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.unlocked_files_title, files.count { !it.failed }),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    val failedCount = files.count { it.failed }
                    if (failedCount > 0) {
                        Text(
                            text = stringResource(id = R.string.unlock_failed_count, failedCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
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

            if (onSavePlaylist != null && files.any { !it.failed }) {
                OutlinedButton(
                    onClick = onSavePlaylist,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_download),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.save_playlist))
                }
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
                val fileError = file.error
                if (fileError != null) {
                    // the provider refused this file, show why
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_warning),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = fileError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    return@forEachIndexed
                }
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

/** Asks a direct link for every media file of a download, shown when there are at least two. */
@Composable
fun AllMediaLinksButton(
    mediaCount: Int,
    progress: Pair<Int, Int>?,
    hasUnlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled && progress == null,
    ) {
        if (progress != null) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_play_arrow),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text =
                when {
                    progress != null -> stringResource(id = R.string.unlocking_progress, progress.first, progress.second)
                    hasUnlocked -> stringResource(id = R.string.unlock_again)
                    else -> stringResource(id = R.string.unlock_all_media, mediaCount)
                },
        )
    }
}
