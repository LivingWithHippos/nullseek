package com.github.livingwithhippos.nullseek.tv.ui.newdownload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.FilterChip
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.tv.ui.components.LocalMessenger
import com.github.livingwithhippos.nullseek.tv.ui.components.ScreenHeader
import com.github.livingwithhippos.nullseek.tv.ui.components.SectionCard
import com.github.livingwithhippos.nullseek.tv.ui.components.SwitchRow
import com.github.livingwithhippos.nullseek.tv.ui.components.TvActionButton
import com.github.livingwithhippos.nullseek.tv.ui.components.TvDialog
import com.github.livingwithhippos.nullseek.tv.ui.components.TvTextField
import com.github.livingwithhippos.nullseek.tv.ui.components.launchSafely
import com.github.livingwithhippos.nullseek.ui.newdownload.NewDownloadViewModel
import com.github.livingwithhippos.nullseek.ui.newdownload.UploadState
import com.github.livingwithhippos.nullseek.ui.newdownload.appendLinkLine
import com.github.livingwithhippos.nullseek.ui.newdownload.getFileName
import com.github.livingwithhippos.nullseek.ui.newdownload.parseDownloadLinks
import com.github.livingwithhippos.nullseek.ui.newdownload.readPickedFiles
import com.github.livingwithhippos.nullseek.utils.getClipboardText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

private enum class NewDownloadTab(
    @StringRes val label: Int,
    val fileExtension: String?,
) {
    TORRENT(R.string.torrents, ".torrent"),
    WEB(R.string.web_downloads, null),
    USENET(R.string.usenet_downloads, ".nzb"),
}

// api values used by the torbox client
private val SEEDING_OPTIONS =
    listOf(
        R.string.seeding_auto to 1,
        R.string.seeding_seed to 2,
        R.string.seeding_dont_seed to 3,
    )

private val USENET_OPTIONS =
    listOf(
        R.string.usenet_default to -1,
        R.string.usenet_none to 0,
        R.string.usenet_repair to 1,
        R.string.usenet_repair_unpack to 2,
        R.string.usenet_repair_unpack_delete to 3,
    )

private val HASH_REGEX = Regex("^([a-fA-F0-9]{40}|[a-zA-Z2-7]{32})$")

/** Torrent hashes typed on their own are turned into magnet links. */
internal fun normalizeTorrentInput(text: String): String =
    text
        .split("\\s+".toRegex())
        .filter { it.isNotBlank() }
        .joinToString(" ") { token -> if (HASH_REGEX.matches(token)) "magnet:?xt=urn:btih:$token" else token }

@Composable
fun TvNewDownloadScreen(
    initialLinks: String,
    modifier: Modifier = Modifier,
    // called when everything was added
    onQueued: () -> Unit = {},
    viewModel: NewDownloadViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    val scope = rememberCoroutineScope()
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(if (initialLinks.startsWith("http")) 1 else 0) }
    var linkText by rememberSaveable(initialLinks) { mutableStateOf(initialLinks) }
    var password by rememberSaveable { mutableStateOf("") }
    var seedingOption by rememberSaveable { mutableIntStateOf(1) }
    var usenetOption by rememberSaveable { mutableIntStateOf(-1) }
    var allowZipped by rememberSaveable { mutableStateOf(true) }
    var selectedFiles by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val tab = NewDownloadTab.entries[selectedTab]
    val links =
        remember(linkText, tab) {
            parseDownloadLinks(if (tab == NewDownloadTab.TORRENT) normalizeTorrentInput(linkText) else linkText)
        }
    val tabFiles =
        remember(selectedFiles, tab) {
            val ext = tab.fileExtension
            if (ext == null) emptyList() else selectedFiles.filter { context.getFileName(it).endsWith(ext, ignoreCase = true) }
        }
    val total = links.size + tabFiles.size

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) selectedFiles = (selectedFiles + uris).distinct()
        }

    LaunchedEffect(uploadState) {
        val state = uploadState
        if (state is UploadState.Finished && state.failed == 0) {
            // the messenger lives at the app root, so the message outlives this screen
            messenger.show(context.getString(R.string.queued_success, state.successful))
            viewModel.resetState()
            onQueued()
        }
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ScreenHeader(title = stringResource(R.string.new_download))

        TabRow(selectedTabIndex = selectedTab) {
            NewDownloadTab.entries.forEachIndexed { index, entry ->
                Tab(selected = index == selectedTab, onFocus = { selectedTab = index }) {
                    Text(stringResource(entry.label), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                SectionCard(title = stringResource(R.string.link_label), icon = R.drawable.ic_magnet) {
                    TvTextField(
                        value = linkText,
                        onValueChange = { linkText = it },
                        label =
                            when (tab) {
                                NewDownloadTab.TORRENT -> stringResource(R.string.links_label_torrent)
                                NewDownloadTab.WEB -> stringResource(R.string.links_label_web)
                                NewDownloadTab.USENET -> stringResource(R.string.links_label_usenet)
                            },
                        placeholder = stringResource(R.string.link_hint),
                        singleLine = false,
                        minLines = 3,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvActionButton(
                            text = stringResource(R.string.paste_from_clipboard),
                            icon = R.drawable.ic_paste,
                            onClick = { linkText = appendLinkLine(linkText, getClipboardText(context)) },
                        )
                        if (linkText.isNotBlank()) {
                            TvActionButton(
                                text = stringResource(R.string.clear_links),
                                icon = R.drawable.ic_close,
                                onClick = { linkText = "" },
                            )
                        }
                        Text(
                            stringResource(R.string.valid_links_count, links.size),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }
                }

                val ext = tab.fileExtension
                if (ext != null) {
                    SectionCard(title = stringResource(R.string.file_label) + " ($ext)", icon = R.drawable.ic_file) {
                        TvActionButton(
                            text = stringResource(R.string.file_hint_tv),
                            icon = R.drawable.ic_add,
                            onClick = { picker.launchSafely(context, arrayOf("*/*")) },
                        )
                        tabFiles.forEach { uri ->
                            ListItem(
                                selected = false,
                                onClick = { selectedFiles = selectedFiles - uri },
                                leadingContent = { Icon(painterResource(R.drawable.ic_file), null, Modifier.size(24.dp)) },
                                trailingContent = { Icon(painterResource(R.drawable.ic_close), stringResource(R.string.delete)) },
                                headlineContent = { Text(context.getFileName(uri)) },
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                SectionCard(title = stringResource(R.string.advanced_settings), icon = R.drawable.ic_tune) {
                    when (tab) {
                        NewDownloadTab.TORRENT -> {
                            Text(stringResource(R.string.seeding_settings), style = MaterialTheme.typography.titleSmall)
                            OptionChips(SEEDING_OPTIONS, seedingOption) { seedingOption = it }
                        }

                        NewDownloadTab.USENET -> {
                            Text(stringResource(R.string.usenet_post_processing), style = MaterialTheme.typography.titleSmall)
                            OptionChips(USENET_OPTIONS, usenetOption) { usenetOption = it }
                        }

                        NewDownloadTab.WEB -> {}
                    }
                    if (tab != NewDownloadTab.TORRENT) {
                        TvTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = stringResource(R.string.password_label) + " " + stringResource(R.string.optional_label),
                            isPassword = true,
                        )
                    }
                    SwitchRow(
                        title = stringResource(R.string.allow_zipped_downloads),
                        checked = allowZipped,
                        onCheckedChange = { allowZipped = it },
                    )
                }

                TvActionButton(
                    text = stringResource(R.string.action_add_downloads, total),
                    icon = R.drawable.ic_download,
                    primary = true,
                    enabled = total > 0,
                    loading = uploadState is UploadState.Uploading,
                    onClick = {
                        scope.launch {
                            val files = withContext(Dispatchers.IO) { context.readPickedFiles(tabFiles) }
                            viewModel.queueDownloads(
                                links = links,
                                files = files,
                                password = password.takeIf { it.isNotBlank() },
                                seedingOption = seedingOption,
                                usenetOption = usenetOption,
                                allowZipped = allowZipped,
                            )
                        }
                    },
                )
                Spacer(Modifier.size(24.dp))
            }
        }
    }

    val state = uploadState
    if (state is UploadState.Finished && state.failed > 0) {
        TvDialog(
            title = stringResource(R.string.upload_report_title),
            onDismiss = viewModel::resetState,
            confirmText = stringResource(R.string.ok),
            onConfirm = viewModel::resetState,
        ) {
            androidx.compose.material3.Text(stringResource(R.string.upload_report_successful, state.successful))
            androidx.compose.material3.Text(stringResource(R.string.upload_report_failed, state.failed), color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                state.errors.forEach { androidx.compose.material3.Text(it) }
            }
        }
    }
}

@Composable
private fun OptionChips(
    options: List<Pair<Int, Int>>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { (label, value) ->
            FilterChip(selected = selected == value, onClick = { onSelect(value) }) { Text(stringResource(label)) }
        }
    }
}
