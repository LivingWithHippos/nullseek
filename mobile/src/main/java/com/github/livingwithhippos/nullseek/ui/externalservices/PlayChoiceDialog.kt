package com.github.livingwithhippos.nullseek.ui.externalservices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import com.github.livingwithhippos.nullseek.ui.theme.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.data.model.ExternalService
import com.github.livingwithhippos.nullseek.data.model.ExternalServiceType
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme

@Composable
fun PlayChoiceDialog(
    services: List<ExternalService>,
    onSelect: (ExternalService, setAsDefault: Boolean) -> Unit,
    onDismiss: () -> Unit,
    onNavigateToSettings: (() -> Unit)? = null,
) {
    if (services.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = stringResource(id = R.string.no_external_players_dialog_title)) },
            text = { Text(text = stringResource(id = R.string.no_external_players_dialog_message)) },
            confirmButton = {
                if (onNavigateToSettings != null) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onNavigateToSettings()
                        },
                    ) {
                        Text(text = stringResource(id = R.string.action_configure))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
        return
    }

    var selectedPlayer by remember { mutableStateOf(services.first()) }
    var setAsDefault by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.play_choice_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                services.forEach { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPlayer = player }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedPlayer.id == player.id,
                            onClick = { selectedPlayer = player },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = player.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "${player.type.displayName} • ${player.address}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { setAsDefault = !setAsDefault }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = setAsDefault,
                        onCheckedChange = { setAsDefault = it },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.play_choice_dialog_remember),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSelect(selectedPlayer, setAsDefault)
                },
            ) {
                Text(text = stringResource(id = R.string.action_play))
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
fun PlayChoiceDialogPreview() {
    NullSeekTheme {
        PlayChoiceDialog(
            services =
                listOf(
                    ExternalService(
                        id = 1L,
                        name = "Living Room Kodi",
                        type = ExternalServiceType.KODI,
                        address = "http://192.168.1.100:8080",
                    ),
                    ExternalService(
                        id = 2L,
                        name = "Bedroom VLC",
                        type = ExternalServiceType.VLC,
                        address = "http://192.168.1.101:9090",
                    ),
                ),
            onSelect = { _, _ -> },
            onDismiss = {},
        )
    }
}
