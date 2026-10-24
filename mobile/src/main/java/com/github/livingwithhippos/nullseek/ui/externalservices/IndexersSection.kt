package com.github.livingwithhippos.nullseek.ui.externalservices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.data.model.Indexer
import com.github.livingwithhippos.nullseek.data.model.IndexerType
import com.github.livingwithhippos.nullseek.ui.theme.Button
import com.github.livingwithhippos.nullseek.ui.theme.FilterChip
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.theme.OutlinedButton
import com.github.livingwithhippos.nullseek.ui.theme.TextButton

@Composable
fun IndexersSection(
    indexers: List<Indexer>,
    testingIndexerId: Long?,
    onAdd: () -> Unit,
    onToggleActive: (Indexer, Boolean) -> Unit,
    onTest: (Indexer) -> Unit,
    onEdit: (Indexer) -> Unit,
    onDelete: (Indexer) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.indexers_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(id = R.string.indexers_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onAdd) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(id = R.string.add))
            }
        }

        if (indexers.isEmpty()) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.indexers_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(id = R.string.indexers_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            indexers.forEach { indexer ->
                IndexerCard(
                    indexer = indexer,
                    isTesting = testingIndexerId == indexer.id,
                    onToggleActive = { onToggleActive(indexer, it) },
                    onTest = { onTest(indexer) },
                    onEdit = { onEdit(indexer) },
                    onDelete = { onDelete(indexer) },
                )
            }
        }
    }
}

@Composable
fun IndexerCard(
    indexer: Indexer,
    isTesting: Boolean,
    onToggleActive: (Boolean) -> Unit,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f).clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onEdit,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = indexer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Surface(
                        shape = RoundedCornerShape(0.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = indexer.type.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = stringResource(id = R.string.edit),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = stringResource(id = R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Text(
                text = indexer.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onTest,
                    enabled = !isTesting,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(text = stringResource(id = R.string.external_services_test_connection))
                }
                Switch(checked = indexer.active, onCheckedChange = onToggleActive)
            }
        }
    }
}

@Composable
fun IndexerEditDialog(
    initialIndexer: Indexer?,
    isTesting: Boolean,
    testResult: Result<Unit>?,
    onTest: (Indexer) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Indexer) -> Unit,
) {
    var indexerId by rememberSaveable { mutableStateOf(initialIndexer?.id ?: 0L) }
    var name by rememberSaveable { mutableStateOf(initialIndexer?.name.orEmpty()) }
    var selectedType by rememberSaveable { mutableStateOf(initialIndexer?.type ?: IndexerType.JACKETT) }
    var address by rememberSaveable { mutableStateOf(initialIndexer?.address.orEmpty()) }
    var apiToken by rememberSaveable { mutableStateOf(initialIndexer?.apiToken.orEmpty()) }
    var active by rememberSaveable { mutableStateOf(initialIndexer?.active ?: true) }
    var tokenVisible by rememberSaveable { mutableStateOf(false) }

    fun currentIndexer() =
        Indexer(
            id = indexerId,
            name = name.trim(),
            type = selectedType,
            address = address.trim(),
            apiToken = apiToken.trim(),
            active = active,
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    id = if (indexerId == 0L) R.string.indexers_dialog_add_title else R.string.indexers_dialog_edit_title,
                ),
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IndexerType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                if (address.isBlank()) address = "192.168.1.100:${type.defaultPort}"
                            },
                            label = { Text(type.displayName) },
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = stringResource(id = R.string.external_services_name_label)) },
                    placeholder = { Text(text = stringResource(id = R.string.indexers_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(text = stringResource(id = R.string.external_services_address_label)) },
                    placeholder = { Text(text = "192.168.1.100:${selectedType.defaultPort}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = apiToken,
                    onValueChange = { apiToken = it },
                    label = { Text(text = stringResource(id = R.string.indexers_api_token_label)) },
                    visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { tokenVisible = !tokenVisible }) {
                            Icon(
                                painter = painterResource(
                                    id = if (tokenVisible) R.drawable.ic_close else R.drawable.ic_contact_page,
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { active = !active }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = active, onCheckedChange = { active = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.indexers_active_label),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                OutlinedButton(
                    onClick = { onTest(currentIndexer()) },
                    enabled = !isTesting && address.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = R.string.testing_connection))
                    } else {
                        Text(text = stringResource(id = R.string.external_services_test_connection))
                    }
                }

                if (indexerId != 0L) {
                    OutlinedButton(
                        onClick = {
                            indexerId = 0L
                            name = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_copy),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = R.string.duplicate))
                    }
                }

                testResult?.let { result ->
                    val isSuccess = result.isSuccess
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSuccess) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = result.fold(
                                onSuccess = { "Connection successful!" },
                                onFailure = { "Failed: ${it.message}" },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSuccess) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            },
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(currentIndexer()) },
                enabled = name.isNotBlank() && address.isNotBlank(),
            ) {
                Text(text = stringResource(id = R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}

@Composable
fun IndexerDeleteDialog(
    indexer: Indexer,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.indexers_delete_confirm_title)) },
        text = {
            Text(
                text = stringResource(
                    id = R.string.indexers_delete_confirm_message,
                    indexer.type.displayName,
                    indexer.name,
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(text = stringResource(id = R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}

@Preview
@Composable
fun IndexerCardPreview() {
    NullSeekTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            IndexerCard(
                indexer = Indexer(
                    id = 1L,
                    name = "Home Prowlarr",
                    type = IndexerType.PROWLARR,
                    address = "http://192.168.1.100:9696",
                    apiToken = "token",
                    active = true,
                ),
                isTesting = false,
                onToggleActive = {},
                onTest = {},
                onEdit = {},
                onDelete = {},
            )
        }
    }
}

@Preview
@Composable
fun IndexerEditDialogPreview() {
    NullSeekTheme {
        IndexerEditDialog(
            initialIndexer = null,
            isTesting = false,
            testResult = null,
            onTest = {},
            onDismiss = {},
            onSave = {},
        )
    }
}
