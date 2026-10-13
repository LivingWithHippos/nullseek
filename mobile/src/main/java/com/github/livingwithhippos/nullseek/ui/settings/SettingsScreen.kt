package com.github.livingwithhippos.nullseek.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    contentType: NullContentType,
    navigationType: NullNavigationType,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val downloadZipMode by viewModel.downloadZipMode.collectAsStateWithLifecycle()

    SettingsScreen(
        downloadZipMode = downloadZipMode,
        onDownloadZipModeSelected = { viewModel.setDownloadZipMode(it) },
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    downloadZipMode: DownloadZipMode,
    onDownloadZipModeSelected: (DownloadZipMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.download_zip_preference_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(id = R.string.download_zip_preference_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    DownloadZipModeOption(
                        title = stringResource(id = R.string.download_zip_preference_ask),
                        selected = downloadZipMode == DownloadZipMode.ASK,
                        onClick = { onDownloadZipModeSelected(DownloadZipMode.ASK) },
                    )

                    DownloadZipModeOption(
                        title = stringResource(id = R.string.download_zip_preference_zip),
                        selected = downloadZipMode == DownloadZipMode.ZIP,
                        onClick = { onDownloadZipModeSelected(DownloadZipMode.ZIP) },
                    )

                    DownloadZipModeOption(
                        title = stringResource(id = R.string.download_zip_preference_uncompressed),
                        selected = downloadZipMode == DownloadZipMode.UNCOMPRESSED,
                        onClick = { onDownloadZipModeSelected(DownloadZipMode.UNCOMPRESSED) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadZipModeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
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
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}