package com.github.livingwithhippos.nullseek.ui.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.livingwithhippos.nullseek.BuildConfig
import com.github.livingwithhippos.nullseek.billing.BillingManager
import com.github.livingwithhippos.nullseek.billing.EntitlementState
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.data.repository.DEFAULT_PAGING_LOAD_SIZE
import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import com.github.livingwithhippos.nullseek.data.repository.ThemeColor
import com.github.livingwithhippos.nullseek.data.repository.ThemeMode
import com.github.livingwithhippos.nullseek.ui.theme.Button
import com.github.livingwithhippos.nullseek.ui.theme.FilterChip
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.theme.OutlinedButton
import com.github.livingwithhippos.nullseek.ui.theme.TextButton
import com.github.livingwithhippos.nullseek.ui.theme.shouldUseDarkTheme
import com.github.livingwithhippos.nullseek.ui.theme.swatchColor
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import com.github.livingwithhippos.nullseek.widget.TorboxDownloadsWidgetReceiver
import com.github.livingwithhippos.nullseek.widget.TorboxQuickActionsWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PAGING_LOAD_SIZE_PRESETS = listOf(100, 500, 1000, 5000)

private fun defaultBackupFileName(): String {
    val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    return "nullseek_backup_${formatter.format(Date())}.json"
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun SettingsScreen(
    contentType: NullContentType,
    navigationType: NullNavigationType,
    modifier: Modifier = Modifier,
    onNavigateToExternalServices: () -> Unit = {},
    onNavigateToProviders: () -> Unit = {},
    onProvidersChanged: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
    billingManager: BillingManager = koinInject(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentOnProvidersChanged by rememberUpdatedState(onProvidersChanged)

    val downloadZipMode by viewModel.downloadZipMode.collectAsStateWithLifecycle()
    val pagingLoadSize by viewModel.pagingLoadSize.collectAsStateWithLifecycle()
    val searchAutoAddDownload by viewModel.searchAutoAddDownload.collectAsStateWithLifecycle()
    val filterFilesFullPath by viewModel.filterFilesFullPath.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val themeColor by viewModel.themeColor.collectAsStateWithLifecycle()
    val useStandardFont by viewModel.useStandardFont.collectAsStateWithLifecycle()
    val externalDisclaimerAccepted by viewModel.externalDisclaimerAccepted.collectAsStateWithLifecycle()
    val isBackupBusy by viewModel.isBackupBusy.collectAsStateWithLifecycle()
    val entitlementState by billingManager.entitlementState.collectAsStateWithLifecycle()

    var pendingBackupJson by remember { mutableStateOf<String?>(null) }

    val createBackupLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            val json = pendingBackupJson
            pendingBackupJson = null
            if (uri != null && json != null) {
                coroutineScope.launch {
                    val success =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                context.contentResolver.openOutputStream(uri)?.use { stream ->
                                    stream.write(json.toByteArray(Charsets.UTF_8))
                                } ?: error("Unable to open output stream")
                            }.isSuccess
                        }
                    viewModel.onBackupWriteCompleted(success)
                }
            }
        }

    val restoreBackupLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    val rawJson =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                context.contentResolver
                                    .openInputStream(uri)
                                    ?.bufferedReader(Charsets.UTF_8)
                                    ?.use { it.readText() }
                            }.getOrNull()
                        }
                    if (rawJson != null) {
                        viewModel.importBackup(rawJson)
                    } else {
                        viewModel.onBackupReadFailed()
                    }
                }
            }
        }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(context.getString(event.resId))
                }

                is SettingsEvent.WriteBackupFile -> {
                    pendingBackupJson = event.json
                    createBackupLauncher.launch(defaultBackupFileName())
                }

                SettingsEvent.BackupRestored -> {
                    currentOnProvidersChanged()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        SettingsScreen(
            downloadZipMode = downloadZipMode,
            onDownloadZipModeSelected = { viewModel.setDownloadZipMode(it) },
            pagingLoadSize = pagingLoadSize,
            onPagingLoadSizeSelected = { viewModel.setPagingLoadSize(it) },
            searchAutoAddDownload = searchAutoAddDownload,
            onSearchAutoAddDownloadChanged = { viewModel.setSearchAutoAddDownload(it) },
            filterFilesFullPath = filterFilesFullPath,
            onFilterFilesFullPathChanged = { viewModel.setFilterFilesFullPath(it) },
            themeMode = themeMode,
            onThemeModeSelected = { viewModel.setThemeMode(it) },
            themeColor = themeColor,
            onThemeColorSelected = { viewModel.setThemeColor(it) },
            useStandardFont = useStandardFont,
            onUseStandardFontChanged = { viewModel.setUseStandardFont(it) },
            externalDisclaimerAccepted = externalDisclaimerAccepted,
            onAcceptExternalDisclaimer = { viewModel.acceptExternalDisclaimer() },
            isBackupBusy = isBackupBusy,
            onCreateBackup = { viewModel.exportBackup() },
            onRestoreBackup = {
                restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
            },
            onNavigateToExternalServices = onNavigateToExternalServices,
            onNavigateToProviders = onNavigateToProviders,
            entitlementState = entitlementState,
            onSubscribeClick = {
                context.findActivity()?.let { activity ->
                    billingManager.launchSubscriptionFlow(activity)
                }
            },
            onRestorePurchasesClick = {
                billingManager.refreshPurchases()
            },
            onPinDownloadsWidgetClick = {
                if (!requestPinWidget(context, TorboxDownloadsWidgetReceiver::class.java)) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.widgets_pin_unsupported))
                    }
                }
            },
            onPinQuickActionsWidgetClick = {
                if (!requestPinWidget(context, TorboxQuickActionsWidgetReceiver::class.java)) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.widgets_pin_unsupported))
                    }
                }
            },
            modifier = Modifier.padding(innerPadding),
        )
    }
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
    filterFilesFullPath: Boolean = false,
    onFilterFilesFullPathChanged: (Boolean) -> Unit = {},
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeSelected: (ThemeMode) -> Unit = {},
    themeColor: ThemeColor = ThemeColor.SYSTEM,
    onThemeColorSelected: (ThemeColor) -> Unit = {},
    useStandardFont: Boolean = false,
    onUseStandardFontChanged: (Boolean) -> Unit = {},
    externalDisclaimerAccepted: Boolean = false,
    onAcceptExternalDisclaimer: () -> Unit = {},
    isBackupBusy: Boolean = false,
    onCreateBackup: () -> Unit = {},
    onRestoreBackup: () -> Unit = {},
    onNavigateToExternalServices: () -> Unit = {},
    onNavigateToProviders: () -> Unit = {},
    entitlementState: EntitlementState =
        EntitlementState(
            isFossBuild = BuildConfig.IS_FOSS,
            isPremiumUnlocked = BuildConfig.IS_FOSS,
        ),
    onSubscribeClick: () -> Unit = {},
    onRestorePurchasesClick: () -> Unit = {},
    onPinDownloadsWidgetClick: () -> Unit = {},
    onPinQuickActionsWidgetClick: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    var pageSizeInput by rememberSaveable { mutableStateOf(pagingLoadSize.toString()) }
    var showExternalDisclaimerDialog by rememberSaveable { mutableStateOf(false) }
    var showRestoreConfirmDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(pagingLoadSize) {
        pageSizeInput = pagingLoadSize.toString()
    }

    if (showExternalDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showExternalDisclaimerDialog = false },
            title = { Text(text = stringResource(id = R.string.external_content_disclaimer_title)) },
            text = { Text(text = stringResource(id = R.string.external_content_disclaimer_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onAcceptExternalDisclaimer()
                        showExternalDisclaimerDialog = false
                        onNavigateToExternalServices()
                    },
                ) {
                    Text(text = stringResource(id = R.string.external_content_disclaimer_accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExternalDisclaimerDialog = false }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }

    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text(text = stringResource(id = R.string.backup_restore_confirm_title)) },
            text = { Text(text = stringResource(id = R.string.backup_restore_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        onRestoreBackup()
                    },
                ) {
                    Text(text = stringResource(id = R.string.backup_restore_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
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
                onClick = {
                    if (externalDisclaimerAccepted) {
                        onNavigateToExternalServices()
                    } else {
                        showExternalDisclaimerDialog = true
                    }
                },
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

            WidgetsPremiumSettingsCard(
                entitlementState = entitlementState,
                onSubscribeClick = onSubscribeClick,
                onRestorePurchasesClick = onRestorePurchasesClick,
                onPinDownloadsWidgetClick = onPinDownloadsWidgetClick,
                onPinQuickActionsWidgetClick = onPinQuickActionsWidgetClick,
            )

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val resolvedDarkTheme = shouldUseDarkTheme(themeMode)
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.theme_preference_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(id = R.string.theme_preference_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = stringResource(id = R.string.theme_dark_mode_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onThemeModeSelected(mode) },
                                label = { Text(text = stringResource(id = mode.labelRes())) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = stringResource(id = R.string.theme_color_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ThemeColor.entries.forEach { colorOption ->
                            val swatch = colorOption.swatchColor(resolvedDarkTheme)
                            FilterChip(
                                selected = themeColor == colorOption,
                                onClick = { onThemeColorSelected(colorOption) },
                                leadingIcon =
                                    if (swatch != null) {
                                        {
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .size(12.dp)
                                                        .background(swatch)
                                                        .border(
                                                            width = 1.dp,
                                                            color = MaterialTheme.colorScheme.outline,
                                                        ),
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                label = { Text(text = stringResource(id = colorOption.labelRes())) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(id = R.string.theme_standard_font_title),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(id = R.string.theme_standard_font_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = useStandardFont,
                            onCheckedChange = onUseStandardFontChanged,
                        )
                    }
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

                    DownloadZipModeDropdown(
                        selected = downloadZipMode,
                        onSelected = onDownloadZipModeSelected,
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
                            text = stringResource(id = R.string.filter_files_full_path_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(id = R.string.filter_files_full_path_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = filterFilesFullPath,
                        onCheckedChange = onFilterFilesFullPathChanged,
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

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.backup_preference_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(id = R.string.backup_preference_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onCreateBackup,
                            enabled = !isBackupBusy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = stringResource(id = R.string.backup_create_button))
                        }

                        OutlinedButton(
                            onClick = { showRestoreConfirmDialog = true },
                            enabled = !isBackupBusy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = stringResource(id = R.string.backup_restore_button))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetsPremiumSettingsCard(
    entitlementState: EntitlementState,
    onSubscribeClick: () -> Unit,
    onRestorePurchasesClick: () -> Unit,
    onPinDownloadsWidgetClick: () -> Unit,
    onPinQuickActionsWidgetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summaryText =
        when {
            entitlementState.isFossBuild -> stringResource(id = R.string.widgets_foss_summary)
            entitlementState.isPremiumUnlocked -> stringResource(id = R.string.widgets_playstore_unlocked_summary)
            else -> stringResource(id = R.string.widgets_playstore_locked_summary)
        }

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.widgets_premium_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (entitlementState.errorMessage != null) {
                Text(
                    text = entitlementState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (entitlementState.isPremiumUnlocked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onPinDownloadsWidgetClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(id = R.string.widgets_pin_downloads))
                    }

                    OutlinedButton(
                        onClick = onPinQuickActionsWidgetClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(id = R.string.widgets_pin_quick_actions))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onSubscribeClick,
                        enabled = !entitlementState.isLoading,
                        modifier = Modifier.weight(1f),
                    ) {
                        val buttonText =
                            if (!entitlementState.formattedPrice.isNullOrBlank()) {
                                stringResource(
                                    id = R.string.widgets_subscribe_with_price,
                                    entitlementState.formattedPrice,
                                )
                            } else {
                                stringResource(id = R.string.widgets_subscribe_button)
                            }
                        Text(text = buttonText)
                    }

                    OutlinedButton(
                        onClick = onRestorePurchasesClick,
                        enabled = !entitlementState.isLoading,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(id = R.string.widgets_restore_purchases))
                    }
                }
            }
        }
    }
}

private fun requestPinWidget(
    context: Context,
    receiverClass: Class<*>,
): Boolean {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    if (!appWidgetManager.isRequestPinAppWidgetSupported) {
        return false
    }
    val provider = ComponentName(context, receiverClass)
    return appWidgetManager.requestPinAppWidget(provider, null, null)
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Composable
private fun DownloadZipModeDropdown(
    selected: DownloadZipMode,
    onSelected: (DownloadZipMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = stringResource(id = selected.labelRes()),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DOWNLOAD_ZIP_MODE_ORDER.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = mode.labelRes())) },
                    onClick = {
                        expanded = false
                        onSelected(mode)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

private val DOWNLOAD_ZIP_MODE_ORDER =
    listOf(
        DownloadZipMode.ASK,
        DownloadZipMode.ZIP,
        DownloadZipMode.ZIP_NON_MEDIA,
        DownloadZipMode.UNCOMPRESSED,
    )

private fun DownloadZipMode.labelRes(): Int =
    when (this) {
        DownloadZipMode.ASK -> R.string.download_zip_preference_ask
        DownloadZipMode.ZIP -> R.string.download_zip_preference_zip
        DownloadZipMode.ZIP_NON_MEDIA -> R.string.download_zip_preference_zip_non_media
        DownloadZipMode.UNCOMPRESSED -> R.string.download_zip_preference_uncompressed
    }

private fun ThemeMode.labelRes(): Int =
    when (this) {
        ThemeMode.SYSTEM -> R.string.theme_mode_system
        ThemeMode.LIGHT -> R.string.theme_mode_light
        ThemeMode.DARK -> R.string.theme_mode_dark
        ThemeMode.AMOLED -> R.string.theme_mode_amoled
    }

private fun ThemeColor.labelRes(): Int =
    when (this) {
        ThemeColor.SYSTEM -> R.string.theme_color_system
        ThemeColor.PURPLE -> R.string.theme_color_purple
        ThemeColor.BLUE -> R.string.theme_color_blue
        ThemeColor.TEAL -> R.string.theme_color_teal
        ThemeColor.GREEN -> R.string.theme_color_green
        ThemeColor.ORANGE -> R.string.theme_color_orange
        ThemeColor.RED -> R.string.theme_color_red
        ThemeColor.TORBOX -> R.string.theme_color_torbox
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