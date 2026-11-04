package com.github.livingwithhippos.nullseek.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.FilterChip
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.data.NetProvider
import com.github.livingwithhippos.nullseek.core.domain.DebridFile
import com.github.livingwithhippos.nullseek.core.domain.FileTypeFilter
import com.github.livingwithhippos.nullseek.core.domain.availableTypes
import com.github.livingwithhippos.nullseek.core.domain.filterByQuery
import com.github.livingwithhippos.nullseek.core.domain.filterByType
import com.github.livingwithhippos.nullseek.core.domain.labelRes
import com.github.livingwithhippos.nullseek.core.domain.ProviderKind
import com.github.livingwithhippos.nullseek.core.utils.convertToSize
import com.github.livingwithhippos.nullseek.utils.getClipboardText

/** Chips to pick the debrid service of a new account. */
@Composable
fun TvProviderKindChips(
    selected: ProviderKind,
    onSelect: (ProviderKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.provider_service), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProviderKind.entries.forEach { kind ->
                FilterChip(selected = kind == selected, onClick = { onSelect(kind) }) { Text(kind.displayName) }
            }
        }
    }
}

/** Buttons under a token field: open the provider token page and paste the clipboard. */
@Composable
fun TvTokenHelpers(
    kind: ProviderKind,
    onPaste: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val provider = remember(kind) { NetProvider.of(kind) }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TvActionButton(
            text = stringResource(R.string.setup_get_key_button_provider, provider.name),
            icon = R.drawable.ic_open_in_new,
            onClick = { safeOpenUrl(context, provider.tokenLink) },
        )
        TvActionButton(
            text = stringResource(R.string.paste_from_clipboard),
            icon = R.drawable.ic_paste,
            onClick = { getClipboardText(context)?.let { onPaste(it.trim()) } },
        )
    }
}

// small torrents don't need a search field
private const val SEARCH_MIN_FILES = 5

/** D-pad friendly list of switches to pick the files of a torrent. */
@Composable
fun TvFileSelectionList(
    files: List<DebridFile>,
    selected: Set<Int>,
    onSelectedChange: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filters by rememberSaveable { mutableStateOf(emptySet<FileTypeFilter>()) }
    val availableTypes = remember(files) { files.availableTypes() }
    val shownFiles = remember(files, query, filters) { files.filterByQuery(query).filterByType(filters) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (files.size > SEARCH_MIN_FILES) {
            TvTextField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(R.string.search_files_hint),
            )
        }
        if (availableTypes.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                availableTypes.forEach { type ->
                    FilterChip(
                        selected = type in filters,
                        onClick = { filters = if (type in filters) filters - type else filters + type },
                    ) { Text(stringResource(type.labelRes)) }
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onSelectedChange(files.map { it.id }.toSet()) }) {
                Text(stringResource(R.string.file_selection_select_all))
            }
            OutlinedButton(onClick = { onSelectedChange(emptySet()) }) {
                Text(stringResource(R.string.file_selection_select_none))
            }
        }
        Text(
            stringResource(R.string.file_selection_selected_count, selected.size, files.size),
            style = MaterialTheme.typography.labelMedium,
        )
        if (shownFiles.isEmpty()) {
            Text(stringResource(R.string.file_selection_no_match), style = MaterialTheme.typography.bodySmall)
        }
        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
            items(shownFiles, key = { it.id }) { file ->
                val checked = file.id in selected
                ListItem(
                    selected = false,
                    onClick = { onSelectedChange(if (checked) selected - file.id else selected + file.id) },
                    headlineContent = { Text(file.shortName, maxLines = 2) },
                    supportingContent = { Text(convertToSize(file.size)) },
                    trailingContent = { Switch(checked = checked, onCheckedChange = null) },
                )
            }
        }
    }
}

/** Shown in the torrent pane while Real-Debrid waits for the files to be picked. */
@Composable
fun TvFileSelectionSection(
    files: List<DebridFile>,
    isSelecting: Boolean,
    onConfirm: (Set<Int>) -> Unit,
) {
    var selected by rememberSaveable(files) { mutableStateOf(files.map { it.id }.toSet()) }
    SectionCard(title = stringResource(R.string.file_selection_title)) {
        Text(stringResource(R.string.file_selection_needed), style = MaterialTheme.typography.bodySmall)
        TvFileSelectionList(files = files, selected = selected, onSelectedChange = { selected = it })
        TvActionButton(
            text = stringResource(R.string.file_selection_confirm),
            onClick = { onConfirm(selected) },
            enabled = selected.isNotEmpty() && !isSelecting,
            loading = isSelecting,
            primary = true,
        )
    }
}

/** Asks for the files of a torrent just added. */
@Composable
fun TvFileSelectionDialog(
    name: String,
    files: List<DebridFile>,
    remaining: Int,
    onConfirm: (Set<Int>) -> Unit,
    onLater: () -> Unit,
    onSelectAllRemaining: () -> Unit,
    // the files checked at the start, every file when null
    initialSelected: Set<Int>? = null,
) {
    var selected by rememberSaveable(name, files) { mutableStateOf(initialSelected ?: files.map { it.id }.toSet()) }
    TvDialog(
        title = stringResource(R.string.file_selection_title),
        onDismiss = onLater,
        confirmText = stringResource(R.string.file_selection_confirm),
        onConfirm = { onConfirm(selected) },
        confirmEnabled = selected.isNotEmpty(),
        dismissText = stringResource(R.string.file_selection_later),
    ) {
        Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 2)
        if (remaining > 0) {
            Text(stringResource(R.string.file_selection_remaining, remaining), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onSelectAllRemaining) {
                Text(stringResource(R.string.file_selection_select_all_rest))
            }
        }
        TvFileSelectionList(files = files, selected = selected, onSelectedChange = { selected = it })
    }
}
