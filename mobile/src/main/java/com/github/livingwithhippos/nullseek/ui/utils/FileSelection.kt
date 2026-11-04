package com.github.livingwithhippos.nullseek.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import com.github.livingwithhippos.nullseek.core.domain.FileTypeFilter
import com.github.livingwithhippos.nullseek.core.domain.availableTypes
import com.github.livingwithhippos.nullseek.core.domain.filterByQuery
import com.github.livingwithhippos.nullseek.core.domain.filterByType
import com.github.livingwithhippos.nullseek.core.domain.labelRes
import com.github.livingwithhippos.nullseek.ui.theme.FilterChip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.domain.DebridFile
import com.github.livingwithhippos.nullseek.core.utils.convertToSize
import com.github.livingwithhippos.nullseek.ui.theme.TextButton

// small torrents don't need a search field
private const val SEARCH_MIN_FILES = 5

/** Checkboxes for the files of a torrent, with search, type filters and select all / none / videos. */
@Composable
fun FileSelectionList(
    files: List<DebridFile>,
    selected: Set<Int>,
    onSelectedChange: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filters by rememberSaveable { mutableStateOf(emptySet<FileTypeFilter>()) }
    val availableTypes = remember(files) { files.availableTypes() }
    val shownFiles = remember(files, query, filters) { files.filterByQuery(query).filterByType(filters) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (files.size > SEARCH_MIN_FILES) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_files_hint)) },
                singleLine = true,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon =
                    if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = stringResource(R.string.clear_search),
                                )
                            }
                        }
                    } else {
                        null
                    },
            )
        }
        if (availableTypes.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                availableTypes.forEach { type ->
                    FilterChip(
                        selected = type in filters,
                        onClick = { filters = if (type in filters) filters - type else filters + type },
                        label = { Text(stringResource(type.labelRes)) },
                        enabled = enabled,
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.file_selection_selected_count, selected.size, files.size),
            style = MaterialTheme.typography.labelMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton(onClick = { onSelectedChange(files.map { it.id }.toSet()) }, enabled = enabled) {
                Text(stringResource(R.string.file_selection_select_all))
            }
            TextButton(onClick = { onSelectedChange(emptySet()) }, enabled = enabled) {
                Text(stringResource(R.string.file_selection_select_none))
            }
        }
        if (shownFiles.isEmpty()) {
            Text(
                text = stringResource(R.string.file_selection_no_match),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
            items(shownFiles, key = { it.id }) { file ->
                val checked = file.id in selected
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) {
                                onSelectedChange(if (checked) selected - file.id else selected + file.id)
                            }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = null,
                        enabled = enabled,
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(
                            text = file.shortName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.MiddleEllipsis,
                        )
                        val folder = file.name.removeSuffix(file.shortName).trimEnd('/')
                        if (folder.isNotBlank()) {
                            Text(
                                text = folder,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.MiddleEllipsis,
                            )
                        }
                    }
                    Text(
                        text = convertToSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

/** Shown in the torrent page while the provider waits for the files to be picked. */
@Composable
fun FileSelectionCard(
    files: List<DebridFile>,
    isSelecting: Boolean,
    onConfirm: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by rememberSaveable(files) { mutableStateOf(files.map { it.id }.toSet()) }
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.file_selection_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.file_selection_needed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FileSelectionList(
                files = files,
                selected = selected,
                onSelectedChange = { selected = it },
                enabled = !isSelecting,
            )
            FilledTonalButton(
                onClick = { onConfirm(selected) },
                enabled = selected.isNotEmpty() && !isSelecting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSelecting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.file_selection_confirm))
            }
        }
    }
}

/** Asks for the files of a torrent just added, one torrent at a time. */
@Composable
fun FileSelectionDialog(
    name: String,
    files: List<DebridFile>,
    remaining: Int,
    onConfirm: (Set<Int>) -> Unit,
    onLater: () -> Unit,
    onSelectAllRemaining: () -> Unit,
    onDelete: () -> Unit,
    // the files checked at the start, every file when null
    initialSelected: Set<Int>? = null,
    showDelete: Boolean = true,
) {
    var selected by rememberSaveable(name, files) { mutableStateOf(initialSelected ?: files.map { it.id }.toSet()) }
    AlertDialog(
        // the torrent waits on the provider until a choice is made
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.file_selection_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (remaining > 0) {
                    Text(
                        text = stringResource(R.string.file_selection_remaining, remaining),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FileSelectionList(files = files, selected = selected, onSelectedChange = { selected = it })
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (remaining > 0) {
                        TextButton(onClick = onSelectAllRemaining) {
                            Text(stringResource(R.string.file_selection_select_all_rest))
                        }
                    }
                    if (showDelete) {
                        TextButton(onClick = onDelete) {
                            Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }, enabled = selected.isNotEmpty()) {
                Text(stringResource(R.string.file_selection_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text(stringResource(R.string.file_selection_later))
            }
        },
    )
}
