package com.github.livingwithhippos.nullseek.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.github.livingwithhippos.nullseek.ui.theme.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.data.repository.DEFAULT_PAGING_LOAD_SIZE
import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import org.koin.androidx.compose.koinViewModel

private val PAGING_LOAD_SIZE_PRESETS = listOf(100, 500, 1000, 5000)

@Composable
fun SettingsScreen(
    contentType: NullContentType,
    navigationType: NullNavigationType,
    modifier: Modifier = Modifier,
    onNavigateToExternalServices: () -> Unit = {},
    onNavigateToProviders: () -> Unit = {},
    onNavigateToAddons: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val downloadZipMode by viewModel.downloadZipMode.collectAsStateWithLifecycle()
    val pagingLoadSize by viewModel.pagingLoadSize.collectAsStateWithLifecycle()
    val searchAutoAddDownload by viewModel.searchAutoAddDownload.collectAsStateWithLifecycle()

    SettingsScreen(
        downloadZipMode = downloadZipMode,
        onDownloadZipModeSelected = { viewModel.setDownloadZipMode(it) },
        pagingLoadSize = pagingLoadSize,
        onPagingLoadSizeSelected = { viewModel.setPagingLoadSize(it) },
        searchAutoAddDownload = searchAutoAddDownload,
        onSearchAutoAddDownloadChanged = { viewModel.setSearchAutoAddDownload(it) },
        onNavigateToExternalServices = onNavigateToExternalServices,
        onNavigateToProviders = onNavigateToProviders,
        onNavigateToAddons = onNavigateToAddons,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    downloadZipMode: DownloadZipMode,
    onDownloadZipModeSelected: (DownloadZipMode) -> Unit,
    modifier: Modifier = Modifier,
    pagingLoadSize: Int = DEFAULT_PAGING_LOAD_SIZE,
    onPagingLoadSizeSelected: (Int) -> Unit = {},
    searchAutoAddDownload: Boolean = false,
    onSearchAutoAddDownloadChanged: (Boolean) -> Unit = {},
    onNavigateToExternalServices: () -> Unit = {},
    onNavigateToProviders: () -> Unit = {},
    onNavigateToAddons: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    var pageSizeInput by rememberSaveable { mutableStateOf(pagingLoadSize.toString()) }

    LaunchedEffect(pagingLoadSize) {
        pageSizeInput = pagingLoadSize.toString()
    }

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
                onClick = onNavigateToProviders,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.providers_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(id = R.string.providers_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        painter = painterResource(id = R.drawable.ic_open_in_new),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            OutlinedCard(
                onClick = onNavigateToExternalServices,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.external_services_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(id = R.string.external_services_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        painter = painterResource(id = R.drawable.ic_open_in_new),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            OutlinedCard(
                onClick = onNavigateToAddons,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
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
                    Icon(
                        painter = painterResource(id = R.drawable.ic_open_in_new),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

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
                        title = stringResource(id = R.string.download_zip_preference_zip_non_media),
                        selected = downloadZipMode == DownloadZipMode.ZIP_NON_MEDIA,
                        onClick = { onDownloadZipModeSelected(DownloadZipMode.ZIP_NON_MEDIA) },
                    )

                    DownloadZipModeOption(
                        title = stringResource(id = R.string.download_zip_preference_uncompressed),
                        selected = downloadZipMode == DownloadZipMode.UNCOMPRESSED,
                        onClick = { onDownloadZipModeSelected(DownloadZipMode.UNCOMPRESSED) },
                    )
                }
            }

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.search_auto_add_download_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(id = R.string.search_auto_add_download_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = searchAutoAddDownload,
                        onCheckedChange = onSearchAutoAddDownloadChanged,
                    )
                }
            }

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.paging_load_size_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(id = R.string.paging_load_size_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PAGING_LOAD_SIZE_PRESETS.forEach { preset ->
                            FilterChip(
                                selected = pagingLoadSize == preset,
                                onClick = {
                                    pageSizeInput = preset.toString()
                                    onPagingLoadSizeSelected(preset)
                                },
                                label = { Text(text = preset.toString()) },
                            )
                        }
                    }

                    OutlinedTextField(
                        value = pageSizeInput,
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { it.isDigit() }
                            pageSizeInput = filtered
                            filtered.toIntOrNull()?.takeIf { it > 0 }?.let { validSize ->
                                onPagingLoadSizeSelected(validSize)
                            }
                        },
                        label = { Text(text = stringResource(id = R.string.paging_load_size_label)) },
                        singleLine = true,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                        keyboardActions =
                            KeyboardActions(
                                onDone = {
                                    val parsed = pageSizeInput.toIntOrNull()?.takeIf { it > 0 }
                                    if (parsed != null) {
                                        onPagingLoadSizeSelected(parsed)
                                    } else {
                                        pageSizeInput = pagingLoadSize.toString()
                                    }
                                    focusManager.clearFocus()
                                },
                            ),
                        modifier = Modifier.fillMaxWidth(),
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
                .padding(vertical = 1.dp),
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

@Preview
@Composable
fun SettingsScreenPreview() {
    NullSeekTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            SettingsScreen(
                downloadZipMode = DownloadZipMode.ASK,
                onDownloadZipModeSelected = {},
                pagingLoadSize = DEFAULT_PAGING_LOAD_SIZE,
                onPagingLoadSizeSelected = {},
            )
        }
    }
}