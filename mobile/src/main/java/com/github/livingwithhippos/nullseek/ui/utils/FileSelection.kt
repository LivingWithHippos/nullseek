package com.github.livingwithhippos.nullseek.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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

/** Checkboxes for the files of a torrent, with select all / none. */
@Composable
fun FileSelectionList(
    files: List<DebridFile>,
    selected: Set<Int>,
    onSelectedChange: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.file_selection_selected_count, selected.size, files.size),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onSelectedChange(files.map { it.id }.toSet()) }, enabled = enabled) {
                Text(stringResource(R.string.file_selection_select_all))
            }
            TextButton(onClick = { onSelectedChange(emptySet()) }, enabled = enabled) {
                Text(stringResource(R.string.file_selection_select_none))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
            items(files, key = { it.id }) { file ->
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
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp).heightIn(max = 18.dp), strokeWidth = 2.dp)
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
) {
    var selected by rememberSaveable(name, files) { mutableStateOf(files.map { it.id }.toSet()) }
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
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
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
