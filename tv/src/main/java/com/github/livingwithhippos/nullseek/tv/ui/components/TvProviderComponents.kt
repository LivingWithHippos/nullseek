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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.FilterChip
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.domain.DebridFile
import com.github.livingwithhippos.nullseek.core.domain.ProviderKind
import com.github.livingwithhippos.nullseek.core.utils.convertToSize

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

/** D-pad friendly list of switches to pick the files of a torrent. */
@Composable
fun TvFileSelectionList(
    files: List<DebridFile>,
    selected: Set<Int>,
    onSelectedChange: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
            items(files, key = { it.id }) { file ->
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
) {
    var selected by rememberSaveable(name, files) { mutableStateOf(files.map { it.id }.toSet()) }
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
