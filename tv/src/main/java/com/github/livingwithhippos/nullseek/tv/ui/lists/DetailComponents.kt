package com.github.livingwithhippos.nullseek.tv.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.FilterChip
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Text
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.utils.convertToSize
import com.github.livingwithhippos.nullseek.core.utils.formatSpeed
import com.github.livingwithhippos.nullseek.core.utils.formatToLocalDate
import com.github.livingwithhippos.nullseek.data.model.ExternalService
import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import com.github.livingwithhippos.nullseek.tv.ui.components.Badge
import com.github.livingwithhippos.nullseek.tv.ui.components.InfoRow
import com.github.livingwithhippos.nullseek.tv.ui.components.LocalMessenger
import com.github.livingwithhippos.nullseek.tv.ui.components.SectionCard
import com.github.livingwithhippos.nullseek.tv.ui.components.SwitchRow
import com.github.livingwithhippos.nullseek.tv.ui.components.TvActionButton
import com.github.livingwithhippos.nullseek.tv.ui.components.TvDialog
import com.github.livingwithhippos.nullseek.tv.ui.components.TvTextField
import com.github.livingwithhippos.nullseek.tv.ui.components.safeOpenUrl
import com.github.livingwithhippos.nullseek.utils.FileTypeCategory
import com.github.livingwithhippos.nullseek.utils.isAudioFile
import com.github.livingwithhippos.nullseek.utils.isStreamableOrSubtitle
import com.github.livingwithhippos.nullseek.utils.isSubtitleFile
import com.github.livingwithhippos.nullseek.utils.isVideoFile
import com.github.livingwithhippos.nullseek.utils.largestVideoFile
import com.github.livingwithhippos.nullseek.utils.openWith
import com.github.livingwithhippos.nullseek.utils.setClipboardText
import com.github.livingwithhippos.nullseek.utils.shareText
import com.github.livingwithhippos.torbox.model.TorrentFileDto
import com.github.livingwithhippos.torbox.model.UsenetDownloadFileDto
import com.github.livingwithhippos.torbox.model.WebDownloadFileDto

/** A file of a torrent, web or usenet download, so a single file list works for all of them. */
data class DetailFile(
    val id: Int,
    val name: String,
    val shortName: String,
    val size: Long,
    val mimetype: String?,
) {
    val displayName: String
        get() = shortName.ifBlank { name.replace('\\', '/').substringAfterLast('/') }

    val directory: String
        get() {
            val normalized = name.replace('\\', '/').trim()
            val path =
                if (displayName.isNotEmpty() && normalized.endsWith(displayName)) {
                    normalized.removeSuffix(displayName).trimEnd('/')
                } else {
                    normalized.substringBeforeLast('/', "")
                }
            return path.ifBlank { "/" }
        }

    val isVideo: Boolean get() = isVideoFile(name, mimetype)
    val isAudio: Boolean get() = isAudioFile(name, mimetype)
    val isSubtitle: Boolean get() = isSubtitleFile(name)
}

fun TorrentFileDto.toDetailFile() = DetailFile(id, name, shortName, size, mimetype)

fun WebDownloadFileDto.toDetailFile() = DetailFile(id, name, shortName, size, mimetype)

fun UsenetDownloadFileDto.toDetailFile() = DetailFile(id, name, shortName, size, mimetype)

/** Common fields shown at the top of a detail pane. */
data class DetailOverview(
    val name: String,
    val sizeBytes: Long,
    val state: String,
    val cached: Boolean,
    val active: Boolean,
    val createdAt: String?,
    val expiresAt: String?,
    val progress: Double,
    val downloadSpeed: Long,
    val uploadSpeed: Long? = null,
    val eta: Int,
    val seeds: Int? = null,
    val peers: Int? = null,
    val ratio: Double? = null,
    val hash: String? = null,
    val sourceLabel: String? = null,
    val sourceLink: String? = null,
    val error: String? = null,
)

data class LinkResult(
    val title: String,
    val url: String,
    val isZipped: Boolean = false,
    val fileType: FileTypeCategory? = null,
)

private fun FileTypeCategory.labelRes(): Int =
    when (this) {
        FileTypeCategory.VIDEO -> R.string.file_type_video
        FileTypeCategory.AUDIO -> R.string.file_type_audio
        FileTypeCategory.DOCUMENT -> R.string.file_type_document
        FileTypeCategory.BOOK -> R.string.file_type_book
        FileTypeCategory.ARCHIVE -> R.string.file_type_archive
        FileTypeCategory.SUBTITLE -> R.string.file_type_subtitle
        FileTypeCategory.EXECUTABLE -> R.string.file_type_executable
    }

/** The biggest video, offered as a direct link when downloading everything would create a zip. */
fun List<DetailFile>.largestVideoForDirectLink(): DetailFile? =
    if (size > 1) largestVideoFile(this, { it.name }, { it.mimetype }, { it.size }) else null

/**
 * Detail pane skeleton. Everything lives in one lazy list, so long file lists stay fast
 * and D-pad focus moves naturally from the actions down to the files.
 */
@Composable
fun TvDetailPane(
    overview: DetailOverview,
    onClose: () -> Unit,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    linkResult: LinkResult?,
    onDismissLink: () -> Unit,
    defaultPlayer: ExternalService?,
    mediaPlayers: List<ExternalService>,
    onPlayMedia: (ExternalService, String, Boolean) -> Unit,
    files: List<DetailFile>,
    filterFilesFullPath: Boolean,
    onDownloadFile: ((DetailFile) -> Unit)?,
    modifier: Modifier = Modifier,
    // null hides the largest-video shortcut, as onDownloadFile does for file rows
    onDownloadDirect: ((DetailFile) -> Unit)? = null,
    isDownloading: Boolean = false,
    actions: @Composable () -> Unit,
    extraSections: LazyListScope.() -> Unit = {},
) {
    var query by rememberSaveable(overview.name) { mutableStateOf("") }
    var filterVideo by rememberSaveable { mutableStateOf(false) }
    var filterAudio by rememberSaveable { mutableStateOf(false) }
    var filterSubtitles by rememberSaveable { mutableStateOf(false) }

    val filteredFiles =
        remember(files, query, filterFilesFullPath, filterVideo, filterAudio, filterSubtitles) {
            val anyType = filterVideo || filterAudio || filterSubtitles
            files.filter { file ->
                val matchesQuery =
                    query.isBlank() ||
                        file.displayName.contains(query, ignoreCase = true) ||
                        (filterFilesFullPath && file.name.contains(query, ignoreCase = true))
                val matchesType =
                    !anyType ||
                        (filterVideo && file.isVideo) ||
                        (filterAudio && file.isAudio) ||
                        (filterSubtitles && file.isSubtitle)
                matchesQuery && matchesType
            }
        }

    // downloading everything would zip it: offer a direct link to the main video
    val largestVideo = remember(files) { files.largestVideoForDirectLink() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = overview.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TvActionButton(
                    text = if (isFullScreen) stringResource(R.string.detail_split_view) else stringResource(R.string.detail_full_screen),
                    icon = R.drawable.ic_open_in_new,
                    onClick = onToggleFullScreen,
                    modifier = Modifier.padding(start = 12.dp),
                )
                TvActionButton(
                    text = stringResource(R.string.close),
                    icon = R.drawable.ic_close,
                    onClick = onClose,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
        item { OverviewSection(overview) }
        if (linkResult != null) {
            item {
                DownloadLinkSection(
                    result = linkResult,
                    defaultPlayer = defaultPlayer,
                    mediaPlayers = mediaPlayers,
                    onPlayMedia = onPlayMedia,
                    onDismiss = onDismissLink,
                )
            }
        }
        item {
            SectionCard(title = stringResource(R.string.actions), icon = R.drawable.ic_tune) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    actions()
                }
                if (onDownloadDirect != null && largestVideo != null) {
                    LargestVideoShortcut(
                        video = largestVideo,
                        enabled = true,
                        loading = isDownloading,
                        onClick = { onDownloadDirect(largestVideo) },
                    )
                }
            }
        }
        extraSections()
        if (files.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.torrent_files_title) + " (${filteredFiles.size}/${files.size})",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    TvTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = stringResource(R.string.search_files_hint),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(selected = filterVideo, onClick = { filterVideo = !filterVideo }) {
                            Text(stringResource(R.string.filter_video))
                        }
                        FilterChip(selected = filterAudio, onClick = { filterAudio = !filterAudio }) {
                            Text(stringResource(R.string.filter_audio))
                        }
                        FilterChip(selected = filterSubtitles, onClick = { filterSubtitles = !filterSubtitles }) {
                            Text(stringResource(R.string.filter_subtitles))
                        }
                    }
                }
            }
            if (filteredFiles.isEmpty()) {
                item { Text(stringResource(R.string.no_files_found), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(filteredFiles, key = { it.id }) { file ->
                FileRow(file = file, onClick = onDownloadFile?.let { { it(file) } })
            }
        }
    }
}

@Composable
private fun OverviewSection(overview: DetailOverview) {
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    SectionCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Badge(overview.state)
            if (overview.cached) Badge(stringResource(R.string.cached), color = MaterialTheme.colorScheme.tertiary)
            if (overview.active) Badge(stringResource(R.string.active), color = MaterialTheme.colorScheme.secondary)
        }
        InfoRow(stringResource(R.string.size), convertToSize(overview.sizeBytes))

        val complete = overview.progress >= 1.0
        if (!(overview.cached && complete)) {
            InfoRow(stringResource(R.string.progress), "${(overview.progress * 100).toInt()}%")
            LinearProgressIndicator(
                progress = { overview.progress.toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            InfoRow(stringResource(R.string.download_speed), formatSpeed(overview.downloadSpeed))
            overview.uploadSpeed?.let { InfoRow(stringResource(R.string.upload_speed), formatSpeed(it)) }
            if (overview.eta > 0) InfoRow(stringResource(R.string.eta), formatEta(overview.eta))
        }
        if (overview.seeds != null && overview.peers != null) {
            InfoRow(stringResource(R.string.torrent_seeds_peers), "${overview.seeds} / ${overview.peers}")
        }
        overview.ratio?.let { InfoRow(stringResource(R.string.ratio), "%.2f".format(it)) }
        overview.error?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        // secondary rows start collapsed
        var expanded by rememberSaveable(overview.name) { mutableStateOf(false) }
        TvActionButton(
            text = stringResource(if (expanded) R.string.overview_less_details else R.string.overview_more_details),
            icon = if (expanded) R.drawable.ic_arrow_drop_up else R.drawable.ic_arrow_drop_down,
            onClick = { expanded = !expanded },
        )
        if (!expanded) return@SectionCard
        overview.createdAt?.let { InfoRow(stringResource(R.string.added_on), formatToLocalDate(it)) }
        overview.expiresAt?.let { InfoRow(stringResource(R.string.expires), formatToLocalDate(it)) }
        overview.hash?.takeIf { it.isNotBlank() }?.let { InfoRow(stringResource(R.string.hash), it, maxLines = 1) }
        val link = overview.sourceLink
        if (!link.isNullOrBlank() && overview.sourceLabel != null) {
            Text(overview.sourceLabel, style = MaterialTheme.typography.titleSmall)
            Text(link, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val copied = stringResource(R.string.search_detail_link_copied)
                TvActionButton(
                    text = stringResource(R.string.copy_link),
                    icon = R.drawable.ic_copy,
                    onClick = {
                        setClipboardText(context, link)
                        messenger.show(copied)
                    },
                )
                TvActionButton(
                    text = stringResource(R.string.action_share),
                    icon = R.drawable.ic_share,
                    onClick = { runCatching { shareText(context, link) } },
                )
            }
        }
    }
}

@Composable
private fun formatEta(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) stringResource(R.string.eta_hours_minutes, h, m) else stringResource(R.string.eta_minutes_seconds, m, s)
}

@Composable
private fun FileRow(
    file: DetailFile,
    onClick: (() -> Unit)?,
) {
    val icon =
        when {
            file.isVideo -> R.drawable.ic_movie
            file.isAudio -> R.drawable.ic_audiotrack
            file.isSubtitle -> R.drawable.ic_subtitles
            else -> R.drawable.ic_file
        }
    ListItem(
        selected = false,
        onClick = { onClick?.invoke() },
        enabled = true,
        leadingContent = { Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp)) },
        supportingContent = {
            Text(
                text = file.directory + " · " + convertToSize(file.size),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent =
            onClick?.let {
                { Icon(painterResource(R.drawable.ic_download), contentDescription = stringResource(R.string.download)) }
            },
        headlineContent = { Text(file.displayName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
    )
}

/** Offered when downloading a whole item would zip it, but it contains a video that could be streamed. */
@Composable
fun LargestVideoShortcut(
    video: DetailFile,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TvActionButton(
            text = stringResource(R.string.download_largest_video),
            icon = R.drawable.ic_play_arrow,
            enabled = enabled,
            loading = loading,
            onClick = onClick,
        )
        Text(
            text = "${stringResource(R.string.selected_video)} ${video.displayName} · ${convertToSize(video.size)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.download_mixed_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Generated link with the actions: play on a media player, open, copy and share. */
@Composable
fun DownloadLinkSection(
    result: LinkResult,
    defaultPlayer: ExternalService?,
    mediaPlayers: List<ExternalService>,
    onPlayMedia: (ExternalService, String, Boolean) -> Unit,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isMedia: Boolean = isStreamableOrSubtitle(result.title),
) {
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    var showPlayChoice by remember { mutableStateOf(false) }
    val copied = stringResource(R.string.download_link_copied)
    val chooserTitle = stringResource(R.string.action_open_with)

    SectionCard(modifier = modifier, title = stringResource(R.string.download_link_title), icon = R.drawable.ic_download) {
        Text(result.title, fontWeight = FontWeight.SemiBold, maxLines = 4, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Badge(
                text = stringResource(if (result.isZipped) R.string.download_link_zipped else R.string.download_link_direct),
                color = if (result.isZipped) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            )
            result.fileType?.let { Badge(text = stringResource(it.labelRes()), color = MaterialTheme.colorScheme.secondary) }
        }
        if (result.isZipped) {
            Text(
                text = stringResource(R.string.download_link_zipped_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.download_link_api_key_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        Text(result.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isMedia) {
                TvActionButton(
                    text =
                        defaultPlayer?.let { stringResource(R.string.action_play_on, it.name) }
                            ?: stringResource(R.string.action_play_external),
                    icon = R.drawable.ic_play_arrow,
                    primary = true,
                    onClick = {
                        if (defaultPlayer != null) onPlayMedia(defaultPlayer, result.url, false) else showPlayChoice = true
                    },
                )
            }
            TvActionButton(
                text = stringResource(R.string.action_open_with),
                icon = R.drawable.ic_open_in_new,
                onClick = { openWith(context, result.url, result.title, chooserTitle) },
            )
            TvActionButton(
                text = stringResource(R.string.action_open_link),
                icon = R.drawable.ic_open_in_new,
                onClick = { safeOpenUrl(context, result.url) },
            )
            TvActionButton(
                text = stringResource(R.string.copy_link),
                icon = R.drawable.ic_copy,
                onClick = {
                    setClipboardText(context, result.url)
                    messenger.show(copied)
                },
            )
            TvActionButton(
                text = stringResource(R.string.action_share),
                icon = R.drawable.ic_share,
                onClick = { runCatching { shareText(context, result.url, result.title) } },
            )
            if (onDismiss != null) {
                TvActionButton(text = stringResource(R.string.close), icon = R.drawable.ic_close, onClick = onDismiss)
            }
        }
    }

    if (showPlayChoice) {
        PlayChoiceDialog(
            services = mediaPlayers,
            onSelect = { service, setDefault ->
                showPlayChoice = false
                onPlayMedia(service, result.url, setDefault)
            },
            onDismiss = { showPlayChoice = false },
        )
    }
}

@Composable
fun PlayChoiceDialog(
    services: List<ExternalService>,
    onSelect: (ExternalService, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(services) { mutableStateOf(services.firstOrNull()) }
    var setAsDefault by remember { mutableStateOf(false) }
    if (services.isEmpty()) {
        TvDialog(
            title = stringResource(R.string.no_external_players_dialog_title),
            onDismiss = onDismiss,
            confirmText = stringResource(R.string.close),
            onConfirm = onDismiss,
        ) {
            Text(stringResource(R.string.no_external_players_dialog_message))
        }
        return
    }
    TvDialog(
        title = stringResource(R.string.play_choice_dialog_title),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_play),
        onConfirm = { selected?.let { onSelect(it, setAsDefault) } },
        dismissText = stringResource(R.string.cancel),
        confirmEnabled = selected != null,
    ) {
        services.forEach { service ->
            ListItem(
                selected = selected?.id == service.id,
                onClick = { selected = service },
                leadingContent = { RadioButton(selected = selected?.id == service.id, onClick = null) },
                supportingContent = { Text("${service.type.displayName} · ${service.address}") },
                headlineContent = { Text(service.name) },
            )
        }
        SwitchRow(
            title = stringResource(R.string.play_choice_dialog_remember),
            checked = setAsDefault,
            onCheckedChange = { setAsDefault = it },
        )
    }
}

@Composable
fun DownloadZipDialog(
    itemName: String,
    onConfirm: (DownloadZipMode, Boolean) -> Unit,
    onDismiss: () -> Unit,
    // true for a whole download with several files: the server zips it whatever is picked
    alwaysZipped: Boolean = false,
) {
    var mode by remember { mutableStateOf(DownloadZipMode.ZIP_NON_MEDIA) }
    var rememberChoice by remember { mutableStateOf(false) }
    val options =
        listOf(
            DownloadZipMode.ZIP_NON_MEDIA to R.string.download_option_zip_non_media,
            DownloadZipMode.ZIP to R.string.download_option_zip,
            DownloadZipMode.UNCOMPRESSED to R.string.download_option_uncompressed,
        )
    TvDialog(
        title = stringResource(R.string.download_options_title),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.download),
        onConfirm = { onConfirm(mode, rememberChoice) },
        dismissText = stringResource(R.string.cancel),
    ) {
        Text(itemName, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        if (alwaysZipped) {
            Text(
                text = stringResource(R.string.download_zip_notice_multi),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        options.forEach { (option, label) ->
            ListItem(
                selected = mode == option,
                onClick = { mode = option },
                leadingContent = { RadioButton(selected = mode == option, onClick = null) },
                supportingContent =
                    if (option == DownloadZipMode.ZIP_NON_MEDIA) {
                        { Text(stringResource(R.string.download_option_zip_non_media_caption)) }
                    } else {
                        null
                    },
                headlineContent = { Text(stringResource(label)) },
            )
        }
        SwitchRow(
            title = stringResource(R.string.download_remember_choice),
            checked = rememberChoice,
            onCheckedChange = { rememberChoice = it },
        )
    }
}
