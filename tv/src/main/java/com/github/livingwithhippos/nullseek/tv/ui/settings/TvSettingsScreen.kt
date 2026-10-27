package com.github.livingwithhippos.nullseek.tv.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.FilterChip
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.data.repository.DownloadZipMode
import com.github.livingwithhippos.nullseek.data.repository.ThemeColor
import com.github.livingwithhippos.nullseek.data.repository.ThemeMode
import com.github.livingwithhippos.nullseek.tv.ui.components.ConfirmDialog
import com.github.livingwithhippos.nullseek.tv.ui.components.LocalMessenger
import com.github.livingwithhippos.nullseek.tv.ui.components.ScreenHeader
import com.github.livingwithhippos.nullseek.tv.ui.components.SectionCard
import com.github.livingwithhippos.nullseek.tv.ui.components.SwitchRow
import com.github.livingwithhippos.nullseek.tv.ui.components.TvActionButton
import com.github.livingwithhippos.nullseek.tv.ui.components.TvDialog
import com.github.livingwithhippos.nullseek.tv.ui.components.launchSafely
import com.github.livingwithhippos.nullseek.tv.ui.components.onFocusSelect
import com.github.livingwithhippos.nullseek.tv.ui.components.readTextFromUri
import com.github.livingwithhippos.nullseek.tv.ui.components.writeTextToUri
import com.github.livingwithhippos.nullseek.tv.ui.theme.shouldUseDarkTheme
import com.github.livingwithhippos.nullseek.tv.ui.theme.swatchColor
import com.github.livingwithhippos.nullseek.ui.settings.SettingsEvent
import com.github.livingwithhippos.nullseek.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PAGING_LOAD_SIZE_PRESETS = listOf(100, 500, 1000, 5000)

private enum class SettingsSection(
    @StringRes val title: Int,
    @StringRes val summary: Int,
    @DrawableRes val icon: Int,
) {
    ACCOUNTS(R.string.providers_title, R.string.providers_summary, R.drawable.ic_contact_page),
    EXTERNAL(R.string.external_services_title, R.string.external_services_summary, R.drawable.ic_apps),
    APPEARANCE(R.string.theme_preference_title, R.string.theme_preference_summary, R.drawable.ic_tune),
    DOWNLOADS(R.string.settings_downloads_section, R.string.download_zip_preference_summary, R.drawable.ic_download),
    BACKUP(R.string.backup_preference_title, R.string.backup_preference_summary, R.drawable.ic_history),
}

private fun defaultBackupFileName(): String {
    val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    return "nullseek_backup_${formatter.format(Date())}.json"
}

@Composable
fun TvSettingsScreen(
    onOpenProviders: () -> Unit,
    onOpenExternalServices: () -> Unit,
    onProvidersChanged: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    val scope = rememberCoroutineScope()
    val currentOnProvidersChanged by rememberUpdatedState(onProvidersChanged)

    val disclaimerAccepted by viewModel.externalDisclaimerAccepted.collectAsStateWithLifecycle()
    val isBackupBusy by viewModel.isBackupBusy.collectAsStateWithLifecycle()

    var section by rememberSaveable { mutableIntStateOf(0) }
    var showDisclaimer by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingBackupJson by remember { mutableStateOf<String?>(null) }

    val createBackupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            val json = pendingBackupJson
            pendingBackupJson = null
            if (uri != null && json != null) {
                scope.launch { viewModel.onBackupWriteCompleted(context.writeTextToUri(uri, json)) }
            }
        }
    val restoreBackupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    val json = context.readTextFromUri(uri)
                    if (json != null) viewModel.importBackup(json) else viewModel.onBackupReadFailed()
                }
            }
        }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowMessage -> messenger.show(context.getString(event.resId))
                is SettingsEvent.WriteBackupFile -> {
                    pendingBackupJson = event.json
                    createBackupLauncher.launchSafely(context, defaultBackupFileName())
                }
                SettingsEvent.BackupRestored -> currentOnProvidersChanged()
            }
        }
    }

    val openExternalServices = {
        if (disclaimerAccepted) onOpenExternalServices() else showDisclaimer = true
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(R.string.settings_title))
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp), modifier = Modifier.fillMaxSize()) {
            // sections: focusing one shows its content on the right
            Column(
                modifier = Modifier.weight(0.38f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SettingsSection.entries.forEachIndexed { index, entry ->
                    ListItem(
                        selected = section == index,
                        onClick = {
                            section = index
                            when (entry) {
                                SettingsSection.ACCOUNTS -> onOpenProviders()
                                SettingsSection.EXTERNAL -> openExternalServices()
                                else -> {}
                            }
                        },
                        modifier =
                            Modifier.onFocusSelect { section = index },
                        leadingContent = { Icon(painterResource(entry.icon), null, Modifier.size(24.dp)) },
                        supportingContent = { Text(stringResource(entry.summary), maxLines = 2) },
                        headlineContent = { Text(stringResource(entry.title)) },
                    )
                }
            }

            Column(
                modifier = Modifier.weight(0.62f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (SettingsSection.entries[section]) {
                    SettingsSection.ACCOUNTS ->
                        SectionCard(title = stringResource(R.string.providers_title), icon = R.drawable.ic_contact_page) {
                            Text(stringResource(R.string.providers_summary))
                            TvActionButton(stringResource(R.string.providers_title), onOpenProviders, icon = R.drawable.ic_open_in_new, primary = true)
                        }

                    SettingsSection.EXTERNAL ->
                        SectionCard(title = stringResource(R.string.external_services_title), icon = R.drawable.ic_apps) {
                            Text(stringResource(R.string.external_services_summary))
                            TvActionButton(
                                stringResource(R.string.external_services_title),
                                openExternalServices,
                                icon = R.drawable.ic_open_in_new,
                                primary = true,
                            )
                        }

                    SettingsSection.APPEARANCE -> AppearanceSettings(viewModel)
                    SettingsSection.DOWNLOADS -> DownloadSettings(viewModel)
                    SettingsSection.BACKUP ->
                        SectionCard(title = stringResource(R.string.backup_preference_title), icon = R.drawable.ic_history) {
                            Text(stringResource(R.string.backup_preference_summary))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TvActionButton(
                                    text = stringResource(R.string.backup_create_button),
                                    icon = R.drawable.ic_download,
                                    loading = isBackupBusy,
                                    onClick = viewModel::exportBackup,
                                )
                                TvActionButton(
                                    text = stringResource(R.string.backup_restore_button),
                                    icon = R.drawable.ic_history,
                                    enabled = !isBackupBusy,
                                    onClick = { showRestoreConfirm = true },
                                )
                            }
                        }
                }
            }
        }
    }

    if (showDisclaimer) {
        TvDialog(
            title = stringResource(R.string.external_content_disclaimer_title),
            onDismiss = { showDisclaimer = false },
            confirmText = stringResource(R.string.external_content_disclaimer_accept),
            onConfirm = {
                showDisclaimer = false
                viewModel.acceptExternalDisclaimer()
                onOpenExternalServices()
            },
            dismissText = stringResource(R.string.cancel),
        ) {
            androidx.compose.material3.Text(stringResource(R.string.external_content_disclaimer_message))
        }
    }

    if (showRestoreConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.backup_restore_confirm_title),
            message = stringResource(R.string.backup_restore_confirm_message),
            confirmText = stringResource(R.string.backup_restore_confirm_action),
            onConfirm = {
                showRestoreConfirm = false
                restoreBackupLauncher.launchSafely(context, arrayOf("application/json", "*/*"))
            },
            onDismiss = { showRestoreConfirm = false },
            dismissText = stringResource(R.string.cancel),
        )
    }
}

@Composable
private fun AppearanceSettings(viewModel: SettingsViewModel) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val themeColor by viewModel.themeColor.collectAsStateWithLifecycle()
    val dark = shouldUseDarkTheme(themeMode)

    SectionCard(title = stringResource(R.string.theme_preference_title), icon = R.drawable.ic_tune) {
        Text(stringResource(R.string.theme_dark_mode_label), style = MaterialTheme.typography.titleSmall)
        ChipRow {
            listOf(
                ThemeMode.SYSTEM to R.string.theme_mode_system,
                ThemeMode.LIGHT to R.string.theme_mode_light,
                ThemeMode.DARK to R.string.theme_mode_dark,
            ).forEach { (mode, label) ->
                FilterChip(selected = themeMode == mode, onClick = { viewModel.setThemeMode(mode) }) { Text(stringResource(label)) }
            }
        }
        Text(stringResource(R.string.theme_color_label), style = MaterialTheme.typography.titleSmall)
        ChipRow {
            listOf(
                ThemeColor.SYSTEM to R.string.theme_color_system,
                ThemeColor.PURPLE to R.string.theme_color_purple,
                ThemeColor.BLUE to R.string.theme_color_blue,
                ThemeColor.TEAL to R.string.theme_color_teal,
                ThemeColor.GREEN to R.string.theme_color_green,
                ThemeColor.ORANGE to R.string.theme_color_orange,
                ThemeColor.RED to R.string.theme_color_red,
            ).forEach { (color, label) ->
                FilterChip(
                    selected = themeColor == color,
                    onClick = { viewModel.setThemeColor(color) },
                    leadingIcon =
                        color.swatchColor(dark)?.let { swatch ->
                            { Box(Modifier.size(16.dp).background(swatch, CircleShape)) }
                        },
                ) { Text(stringResource(label)) }
            }
        }
    }
}

@Composable
private fun DownloadSettings(viewModel: SettingsViewModel) {
    val zipMode by viewModel.downloadZipMode.collectAsStateWithLifecycle()
    val pagingSize by viewModel.pagingLoadSize.collectAsStateWithLifecycle()
    val autoAdd by viewModel.searchAutoAddDownload.collectAsStateWithLifecycle()
    val fullPath by viewModel.filterFilesFullPath.collectAsStateWithLifecycle()

    SectionCard(title = stringResource(R.string.download_zip_preference_title), icon = R.drawable.ic_download) {
        Text(stringResource(R.string.download_zip_preference_summary), color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChipRow {
            listOf(
                DownloadZipMode.ASK to R.string.download_zip_preference_ask,
                DownloadZipMode.ZIP_NON_MEDIA to R.string.download_zip_preference_zip_non_media,
                DownloadZipMode.ZIP to R.string.download_zip_preference_zip,
                DownloadZipMode.UNCOMPRESSED to R.string.download_zip_preference_uncompressed,
            ).forEach { (mode, label) ->
                FilterChip(selected = zipMode == mode, onClick = { viewModel.setDownloadZipMode(mode) }) { Text(stringResource(label)) }
            }
        }
    }
    SectionCard(title = stringResource(R.string.paging_load_size_title), icon = R.drawable.ic_list_bullet) {
        Text(stringResource(R.string.paging_load_size_summary), color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChipRow {
            PAGING_LOAD_SIZE_PRESETS.forEach { size ->
                FilterChip(selected = pagingSize == size, onClick = { viewModel.setPagingLoadSize(size) }) { Text(size.toString()) }
            }
        }
    }
    SectionCard {
        SwitchRow(
            title = stringResource(R.string.search_auto_add_download_title),
            summary = stringResource(R.string.search_auto_add_download_summary),
            checked = autoAdd,
            onCheckedChange = viewModel::setSearchAutoAddDownload,
        )
        SwitchRow(
            title = stringResource(R.string.filter_files_full_path_title),
            summary = stringResource(R.string.filter_files_full_path_summary),
            checked = fullPath,
            onCheckedChange = viewModel::setFilterFilesFullPath,
        )
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        content()
    }
}
