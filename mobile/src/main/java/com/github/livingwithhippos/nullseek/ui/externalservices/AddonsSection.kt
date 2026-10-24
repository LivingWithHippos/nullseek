package com.github.livingwithhippos.nullseek.ui.externalservices

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.github.livingwithhippos.nullseek.ui.theme.Button
import com.github.livingwithhippos.nullseek.ui.theme.TextButton
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
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.core.data.database.model.StremioAddonEntity
import com.github.livingwithhippos.nullseek.utils.setClipboardText
import kotlinx.serialization.json.Json

private val addonTypesJson =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

internal fun StremioAddonEntity.parsedTypes(): List<String> =
    runCatching { addonTypesJson.decodeFromString<List<String>>(types) }
        .getOrDefault(emptyList())

@Composable
fun AddonsSection(
    addons: List<StremioAddonEntity>,
    onAdd: () -> Unit,
    onToggleEnabled: (StremioAddonEntity, Boolean) -> Unit,
    onEdit: (StremioAddonEntity) -> Unit,
    onDelete: (StremioAddonEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.stremio_addons_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(id = R.string.stremio_addons_description),
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

        if (addons.isEmpty()) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.stremio_addons_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(id = R.string.stremio_addons_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            addons.forEach { addon ->
                AddonCard(
                    addon = addon,
                    onToggleEnabled = { enabled -> onToggleEnabled(addon, enabled) },
                    onEdit = { onEdit(addon) },
                    onDelete = { onDelete(addon) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddonCard(
    addon: StremioAddonEntity,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val types = remember(addon.types) { addon.parsedTypes() }

    OutlinedCard(
        onClick = onEdit,
        modifier = modifier.fillMaxWidth(),
    ) {
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
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = addon.displayName,
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
                            text = stringResource(id = R.string.version_format, addon.version),
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

            if (addon.displayName != addon.name) {
                Text(
                    text = "${stringResource(id = R.string.stremio_addon_official_name_label)}: ${addon.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = addon.manifestUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (types.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        types.forEach { type ->
                            Surface(
                                shape = RoundedCornerShape(0.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Text(
                                    text = type,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = addon.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = addon.isEnabled,
                    onCheckedChange = onToggleEnabled,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddonEditDialog(
    initialAddon: StremioAddonEntity?,
    isInstalling: Boolean,
    onDismiss: () -> Unit,
    onInstall: (manifestUrl: String, customName: String?) -> Unit,
    onUpdate: (addon: StremioAddonEntity, customName: String, enabled: Boolean) -> Unit = { _, _, _ -> },
) {
    val context = LocalContext.current
    val linkCopied = stringResource(id = R.string.search_detail_link_copied)

    var manifestUrl by rememberSaveable(initialAddon?.manifestUrl) {
        mutableStateOf(initialAddon?.manifestUrl.orEmpty())
    }
    var customName by rememberSaveable(initialAddon?.manifestUrl) {
        mutableStateOf(initialAddon?.displayName.orEmpty())
    }
    var enabled by rememberSaveable(initialAddon?.manifestUrl) {
        mutableStateOf(initialAddon?.isEnabled ?: true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text =
                    if (initialAddon == null) {
                        stringResource(id = R.string.stremio_addon_add)
                    } else {
                        stringResource(id = R.string.stremio_addon_edit)
                    },
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (initialAddon == null) {
                    OutlinedTextField(
                        value = manifestUrl,
                        onValueChange = { manifestUrl = it },
                        label = { Text(stringResource(id = R.string.manifest_url)) },
                        placeholder = { Text("https://example.com/manifest.json") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isInstalling,
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text(stringResource(id = R.string.stremio_addon_custom_name_optional_label)) },
                        placeholder = { Text(stringResource(id = R.string.stremio_addon_custom_name_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isInstalling,
                        singleLine = true,
                    )
                } else {
                    val types = remember(initialAddon.types) { initialAddon.parsedTypes() }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(0.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = stringResource(id = R.string.version_format, initialAddon.version),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                        types.forEach { type ->
                            Surface(
                                shape = RoundedCornerShape(0.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Text(
                                    text = type,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text(stringResource(id = R.string.stremio_addon_custom_name_label)) },
                        placeholder = { Text(initialAddon.name) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = initialAddon.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(id = R.string.stremio_addon_official_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = initialAddon.manifestUrl,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(id = R.string.manifest_url)) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    setClipboardText(context, initialAddon.manifestUrl)
                                    Toast.makeText(context, linkCopied, Toast.LENGTH_SHORT).show()
                                },
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_copy),
                                    contentDescription = stringResource(id = R.string.search_detail_copy_link),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (initialAddon.description.isNotBlank()) {
                        OutlinedTextField(
                            value = initialAddon.description,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(id = R.string.stremio_addon_description_label)) },
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { enabled = !enabled }
                                .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = enabled, onCheckedChange = { enabled = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.indexers_active_label),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (initialAddon == null) {
                Button(
                    onClick = {
                        onInstall(
                            manifestUrl.trim(),
                            customName.trim().takeIf { it.isNotBlank() },
                        )
                    },
                    enabled = manifestUrl.isNotBlank() && !isInstalling,
                ) {
                    if (isInstalling) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text =
                            if (isInstalling) {
                                stringResource(id = R.string.installing)
                            } else {
                                stringResource(id = R.string.add)
                            },
                    )
                }
            } else {
                Button(
                    onClick = {
                        val resolvedName = customName.trim().ifBlank { initialAddon.name }
                        onUpdate(initialAddon, resolvedName, enabled)
                    },
                ) {
                    Text(text = stringResource(id = R.string.save))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isInstalling,
            ) {
                Text(stringResource(id = R.string.cancel))
            }
        },
    )
}

@Composable
fun AddonDeleteDialog(
    addon: StremioAddonEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.stremio_addon_delete_title)) },
        text = {
            Text(
                stringResource(
                    id = R.string.stremio_addon_delete_message,
                    addon.displayName,
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(id = R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        },
    )
}
