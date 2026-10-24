package com.github.livingwithhippos.nullseek.ui.utils

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import com.github.livingwithhippos.nullseek.ui.theme.TextButton

@Composable
fun DownloadZipModal(
    itemName: String?,
    onDismiss: () -> Unit,
    onConfirm: (mode: DownloadZipMode, rememberChoice: Boolean) -> Unit,
) {
    var selectedMode by remember { mutableStateOf(DownloadZipMode.ZIP_NON_MEDIA) }
    var rememberChoice by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.download_options_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!itemName.isNullOrBlank()) {
                    Text(
                        text = itemName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                ModeRadioRow(
                    title = stringResource(id = R.string.download_option_zip_non_media),
                    caption = stringResource(id = R.string.download_option_zip_non_media_caption),
                    selected = selectedMode == DownloadZipMode.ZIP_NON_MEDIA,
                    onClick = { selectedMode = DownloadZipMode.ZIP_NON_MEDIA },
                )

                ModeRadioRow(
                    title = stringResource(id = R.string.download_option_zip),
                    selected = selectedMode == DownloadZipMode.ZIP,
                    onClick = { selectedMode = DownloadZipMode.ZIP },
                )

                ModeRadioRow(
                    title = stringResource(id = R.string.download_option_uncompressed),
                    selected = selectedMode == DownloadZipMode.UNCOMPRESSED,
                    onClick = { selectedMode = DownloadZipMode.UNCOMPRESSED },
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                // Checkbox: Remember choice
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { rememberChoice = !rememberChoice }
                            .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = rememberChoice,
                        onCheckedChange = { rememberChoice = it },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.download_remember_choice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedMode, rememberChoice) },
            ) {
                Text(text = stringResource(id = R.string.action_download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun ModeRadioRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    caption: String? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
