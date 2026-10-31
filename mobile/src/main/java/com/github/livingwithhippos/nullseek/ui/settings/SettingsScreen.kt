package com.github.livingwithhippos.nullseek.ui.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.livingwithhippos.nullseek.BuildConfig
import com.github.livingwithhippos.nullseek.billing.BillingManager
import com.github.livingwithhippos.nullseek.billing.EntitlementState
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.data.repository.DEFAULT_PAGING_LOAD_SIZE
import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import com.github.livingwithhippos.nullseek.data.repository.SeedingOption
import com.github.livingwithhippos.nullseek.data.repository.ThemeColor
import com.github.livingwithhippos.nullseek.data.repository.ThemeMode
import com.github.livingwithhippos.nullseek.ui.theme.Button
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.theme.TextButton
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import com.github.livingwithhippos.nullseek.widget.TorboxDownloadsWidgetReceiver
import com.github.livingwithhippos.nullseek.widget.TorboxQuickActionsWidgetReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

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
    val defaultSeedingOption by viewModel.defaultSeedingOption.collectAsStateWithLifecycle()
    val pagingLoadSize by viewModel.pagingLoadSize.collectAsStateWithLifecycle()
    val searchAutoAddDownload by viewModel.searchAutoAddDownload.collectAsStateWithLifecycle()
    val searchCheckCache by viewModel.searchCheckCache.collectAsStateWithLifecycle()
    val imdbDetailsExpanded by viewModel.imdbDetailsExpanded.collectAsStateWithLifecycle()
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
            defaultSeedingOption = defaultSeedingOption,
            onDefaultSeedingOptionSelected = { viewModel.setDefaultSeedingOption(it) },
            pagingLoadSize = pagingLoadSize,
            onPagingLoadSizeSelected = { viewModel.setPagingLoadSize(it) },
            searchAutoAddDownload = searchAutoAddDownload,
            onSearchAutoAddDownloadChanged = { viewModel.setSearchAutoAddDownload(it) },
            searchCheckCache = searchCheckCache,
            onSearchCheckCacheChanged = { viewModel.setSearchCheckCache(it) },
            imdbDetailsExpanded = imdbDetailsExpanded,
            onImdbDetailsExpandedChanged = { viewModel.setImdbDetailsExpanded(it) },
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
    defaultSeedingOption: SeedingOption = SeedingOption.AUTO,
    onDefaultSeedingOptionSelected: (SeedingOption) -> Unit = {},
    pagingLoadSize: Int = DEFAULT_PAGING_LOAD_SIZE,
    onPagingLoadSizeSelected: (Int) -> Unit = {},
    searchAutoAddDownload: Boolean = false,
    onSearchAutoAddDownloadChanged: (Boolean) -> Unit = {},
    searchCheckCache: Boolean = true,
    onSearchCheckCacheChanged: (Boolean) -> Unit = {},
    imdbDetailsExpanded: Boolean = false,
    onImdbDetailsExpandedChanged: (Boolean) -> Unit = {},
    filterFilesFullPath: Boolean = false,
    onFilterFilesFullPathChanged: (Boolean) -> Unit = {},
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeSelected: (ThemeMode) -> Unit = {},
    themeColor: ThemeColor = ThemeColor.SYSTEM,
    onThemeColorSelected: (ThemeColor) -> Unit = {},
    useStandardFont: Boolean = false,
    onUseStandardFontChanged: (Boolean) -> Unit = {},
    showDonations: Boolean = BuildConfig.DONATE_KOFI_URL.isNotEmpty(),
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
    var showExternalDisclaimerDialog by rememberSaveable { mutableStateOf(false) }
    var showRestoreConfirmDialog by rememberSaveable { mutableStateOf(false) }

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

            NavigationSettingCard(
                title = R.string.external_services_title,
                summary = R.string.external_services_summary,
                onClick = {
                    if (externalDisclaimerAccepted) {
                        onNavigateToExternalServices()
                    } else {
                        showExternalDisclaimerDialog = true
                    }
                },
            )

            NavigationSettingCard(
                title = R.string.providers_title,
                summary = R.string.providers_summary,
                onClick = onNavigateToProviders,
            )

            LanguageSettingsCard()

            WidgetsPremiumSettingsCard(
                entitlementState = entitlementState,
                onSubscribeClick = onSubscribeClick,
                onRestorePurchasesClick = onRestorePurchasesClick,
                onPinDownloadsWidgetClick = onPinDownloadsWidgetClick,
                onPinQuickActionsWidgetClick = onPinQuickActionsWidgetClick,
            )

            ThemeSettingsCard(
                themeMode = themeMode,
                onThemeModeSelected = onThemeModeSelected,
                themeColor = themeColor,
                onThemeColorSelected = onThemeColorSelected,
                useStandardFont = useStandardFont,
                onUseStandardFontChanged = onUseStandardFontChanged,
            )

            DownloadFormatSettingsCard(mode = downloadZipMode, onModeSelected = onDownloadZipModeSelected)

            DefaultSeedingSettingsCard(option = defaultSeedingOption, onOptionSelected = onDefaultSeedingOptionSelected)

            SwitchSettingCard(
                title = R.string.search_auto_add_download_title,
                summary = R.string.search_auto_add_download_summary,
                checked = searchAutoAddDownload,
                onCheckedChange = onSearchAutoAddDownloadChanged,
            )

            SwitchSettingCard(
                title = R.string.search_check_cache_title,
                summary = R.string.search_check_cache_summary,
                checked = searchCheckCache,
                onCheckedChange = onSearchCheckCacheChanged,
            )

            SwitchSettingCard(
                title = R.string.imdb_details_expanded_title,
                summary = R.string.imdb_details_expanded_summary,
                checked = imdbDetailsExpanded,
                onCheckedChange = onImdbDetailsExpandedChanged,
            )

            SwitchSettingCard(
                title = R.string.filter_files_full_path_title,
                summary = R.string.filter_files_full_path_summary,
                checked = filterFilesFullPath,
                onCheckedChange = onFilterFilesFullPathChanged,
            )

            PagingLoadSizeSettingsCard(pagingLoadSize = pagingLoadSize, onPagingLoadSizeSelected = onPagingLoadSizeSelected)

            BackupSettingsCard(
                isBusy = isBackupBusy,
                onCreateBackup = onCreateBackup,
                onRestoreClick = { showRestoreConfirmDialog = true },
            )

            if (showDonations) {
                DonationSettingsCard()
            }

            AboutSettingsCard()
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