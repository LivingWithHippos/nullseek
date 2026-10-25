package com.github.livingwithhippos.nullseek.tv.ui.externalservices

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.FilterChip
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.data.database.model.StremioAddonEntity
import com.github.livingwithhippos.nullseek.data.model.ExternalService
import com.github.livingwithhippos.nullseek.data.model.ExternalServiceCategory
import com.github.livingwithhippos.nullseek.data.model.ExternalServiceType
import com.github.livingwithhippos.nullseek.data.model.Indexer
import com.github.livingwithhippos.nullseek.data.model.IndexerType
import com.github.livingwithhippos.nullseek.tv.ui.components.Badge
import com.github.livingwithhippos.nullseek.tv.ui.components.ConfirmDialog
import com.github.livingwithhippos.nullseek.tv.ui.components.EmptyState
import com.github.livingwithhippos.nullseek.tv.ui.components.LocalMessenger
import com.github.livingwithhippos.nullseek.tv.ui.components.ScreenHeader
import com.github.livingwithhippos.nullseek.tv.ui.components.SectionCard
import com.github.livingwithhippos.nullseek.tv.ui.components.SwitchRow
import com.github.livingwithhippos.nullseek.tv.ui.components.TvActionButton
import com.github.livingwithhippos.nullseek.tv.ui.components.TvDialog
import com.github.livingwithhippos.nullseek.tv.ui.components.TvTextField
import com.github.livingwithhippos.nullseek.ui.addons.AddonsViewModel
import com.github.livingwithhippos.nullseek.ui.externalservices.ExternalServicesEvent
import com.github.livingwithhippos.nullseek.ui.externalservices.ExternalServicesViewModel
import com.github.livingwithhippos.nullseek.ui.externalservices.IndexersViewModel
import org.koin.androidx.compose.koinViewModel

private enum class ServicesTab(val label: Int) {
    PLAYERS(R.string.external_services_media_players_title),
    INDEXERS(R.string.indexers_title),
    ADDONS(R.string.stremio_addons_title),
}

private fun hasLocalNetworkPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

@Composable
fun TvExternalServicesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var hasPermission by remember { mutableStateOf(hasLocalNetworkPermission(context)) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasPermission = granted }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { hasPermission = hasLocalNetworkPermission(context) }

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeader(title = stringResource(R.string.external_services_title), subtitle = stringResource(R.string.external_services_summary)) {
            TvActionButton(stringResource(R.string.close), onBack, icon = R.drawable.ic_close)
        }
        if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            SectionCard(title = stringResource(R.string.external_services_local_network_warning_title), icon = R.drawable.ic_warning) {
                Text(stringResource(R.string.external_services_local_network_warning_message))
                TvActionButton(
                    text = stringResource(R.string.external_services_grant_permission),
                    primary = true,
                    onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK) },
                )
            }
        }
        TabRow(selectedTabIndex = selectedTab) {
            ServicesTab.entries.forEachIndexed { index, tab ->
                Tab(selected = index == selectedTab, onFocus = { selectedTab = index }) {
                    Text(stringResource(tab.label), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        }
        when (ServicesTab.entries[selectedTab]) {
            ServicesTab.PLAYERS -> MediaPlayersTab()
            ServicesTab.INDEXERS -> IndexersTab()
            ServicesTab.ADDONS -> AddonsTab()
        }
    }
}

// ---------------- media players ----------------

@Composable
private fun MediaPlayersTab(viewModel: ExternalServicesViewModel = koinViewModel()) {
    val messenger = LocalMessenger.current
    val services by viewModel.services.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val players = services.filter { it.type.category == ExternalServiceCategory.MEDIA_PLAYER }
    var editing by remember { mutableStateOf<ExternalService?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ExternalService?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ExternalServicesEvent.ShowMessage -> messenger.show(event.message)
            }
        }
    }

    ServiceList(
        addLabel = stringResource(R.string.external_services_add_player),
        onAdd = { adding = true },
        isEmpty = players.isEmpty(),
        emptyText = stringResource(R.string.external_services_empty),
        emptyHint = stringResource(R.string.external_services_empty_hint_tv),
    ) {
        items(players, key = { it.id }) { service ->
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(service.name, style = MaterialTheme.typography.titleLarge)
                    Badge(service.type.displayName, color = MaterialTheme.colorScheme.secondary)
                    if (service.isDefault) Badge(stringResource(R.string.external_services_default_badge))
                }
                Text(service.address, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ActionsRow {
                    TvActionButton(
                        stringResource(R.string.external_services_test_connection),
                        { viewModel.testConnection(service) },
                        icon = R.drawable.ic_refresh,
                        loading = uiState.testingServiceId == service.id,
                    )
                    if (!service.isDefault) {
                        TvActionButton(
                            stringResource(R.string.external_services_set_default),
                            { viewModel.setDefault(service.id, ExternalServiceCategory.MEDIA_PLAYER, service.name) },
                            icon = R.drawable.ic_play_arrow,
                        )
                    }
                    TvActionButton(stringResource(R.string.edit), { editing = service }, icon = R.drawable.ic_edit)
                    TvActionButton(stringResource(R.string.delete), { deleting = service }, icon = R.drawable.ic_delete)
                }
            }
        }
    }

    if (adding || editing != null) {
        MediaPlayerDialog(
            initial = editing,
            testing = uiState.isTestingInDialog,
            testResult = uiState.dialogTestResult,
            onTest = viewModel::testDialogConnection,
            onDismiss = {
                adding = false
                editing = null
                viewModel.clearDialogTestResult()
            },
            onSave = { service ->
                if (service.id == 0L) viewModel.addService(service) else viewModel.updateService(service)
                adding = false
                editing = null
                viewModel.clearDialogTestResult()
            },
        )
    }

    deleting?.let { service ->
        ConfirmDialog(
            title = stringResource(R.string.external_services_delete_confirm_title),
            message = stringResource(R.string.external_services_delete_confirm_message, service.type.displayName, service.name),
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                viewModel.deleteService(service.id, service.name)
                deleting = null
            },
            onDismiss = { deleting = null },
            dismissText = stringResource(R.string.cancel),
        )
    }
}

@Composable
private fun MediaPlayerDialog(
    initial: ExternalService?,
    testing: Boolean,
    testResult: Result<Unit>?,
    onTest: (ExternalService) -> Unit,
    onDismiss: () -> Unit,
    onSave: (ExternalService) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var type by remember { mutableStateOf(initial?.type ?: ExternalServiceType.KODI) }
    var address by remember { mutableStateOf(initial?.address.orEmpty()) }
    var username by remember { mutableStateOf(initial?.username.orEmpty()) }
    var password by remember { mutableStateOf(initial?.password.orEmpty()) }
    var isDefault by remember { mutableStateOf(initial?.isDefault ?: false) }

    fun build() =
        ExternalService(
            id = initial?.id ?: 0L,
            name = name.trim(),
            type = type,
            address = address.trim(),
            username = username.trim(),
            password = password,
            isDefault = isDefault,
        )

    TvDialog(
        title = stringResource(if (initial == null) R.string.external_services_dialog_add_title else R.string.external_services_dialog_edit_title),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.save),
        confirmEnabled = name.isNotBlank() && address.isNotBlank(),
        onConfirm = { onSave(build()) },
        dismissText = stringResource(R.string.cancel),
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TvTextField(name, { name = it }, label = stringResource(R.string.external_services_name_label), placeholder = stringResource(R.string.external_services_name_hint))
            ActionsRow {
                ExternalServiceType.entries.filter { it.category == ExternalServiceCategory.MEDIA_PLAYER }.forEach { option ->
                    FilterChip(selected = type == option, onClick = { type = option }) { Text(option.displayName) }
                }
            }
            TvTextField(
                address,
                { address = it },
                label = stringResource(R.string.external_services_address_label),
                placeholder = "192.168.1.100:${type.defaultPort}",
                keyboardType = KeyboardType.Uri,
            )
            TvTextField(username, { username = it }, label = stringResource(R.string.external_services_username_label))
            TvTextField(
                password,
                { password = it },
                label = stringResource(R.string.external_services_password_label),
                isPassword = true,
                supportingText = if (type == ExternalServiceType.KODI) stringResource(R.string.external_services_password_kodi_hint) else null,
            )
            SwitchRow(stringResource(R.string.external_services_set_as_default_label), isDefault, { isDefault = it })
            TestRow(testing = testing, result = testResult, enabled = address.isNotBlank(), onTest = { onTest(build()) })
        }
    }
}

// ---------------- indexers ----------------

@Composable
private fun IndexersTab(viewModel: IndexersViewModel = koinViewModel()) {
    val messenger = LocalMessenger.current
    val indexers by viewModel.indexers.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Indexer?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Indexer?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ExternalServicesEvent.ShowMessage -> messenger.show(event.message)
            }
        }
    }

    ServiceList(
        addLabel = stringResource(R.string.indexers_dialog_add_title),
        onAdd = { adding = true },
        isEmpty = indexers.isEmpty(),
        emptyText = stringResource(R.string.indexers_empty),
        emptyHint = stringResource(R.string.indexers_empty_hint_tv),
    ) {
        items(indexers, key = { it.id }) { indexer ->
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(indexer.name, style = MaterialTheme.typography.titleLarge)
                    Badge(indexer.type.displayName, color = MaterialTheme.colorScheme.secondary)
                }
                Text(indexer.address, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SwitchRow(stringResource(R.string.indexers_active_label), indexer.active, { viewModel.setActive(indexer, it) })
                ActionsRow {
                    TvActionButton(
                        stringResource(R.string.external_services_test_connection),
                        { viewModel.testConnection(indexer) },
                        icon = R.drawable.ic_refresh,
                        loading = uiState.testingIndexerId == indexer.id,
                    )
                    TvActionButton(stringResource(R.string.edit), { editing = indexer }, icon = R.drawable.ic_edit)
                    TvActionButton(stringResource(R.string.delete), { deleting = indexer }, icon = R.drawable.ic_delete)
                }
            }
        }
    }

    if (adding || editing != null) {
        val initial = editing
        var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
        var type by remember(initial) { mutableStateOf(initial?.type ?: IndexerType.JACKETT) }
        var address by remember(initial) { mutableStateOf(initial?.address.orEmpty()) }
        var token by remember(initial) { mutableStateOf(initial?.apiToken.orEmpty()) }
        var active by remember(initial) { mutableStateOf(initial?.active ?: true) }
        val close = {
            adding = false
            editing = null
            viewModel.clearDialogTestResult()
        }

        fun build() = Indexer(id = initial?.id ?: 0L, name = name.trim(), type = type, address = address.trim(), apiToken = token.trim(), active = active)

        TvDialog(
            title = stringResource(if (initial == null) R.string.indexers_dialog_add_title else R.string.indexers_dialog_edit_title),
            onDismiss = close,
            confirmText = stringResource(R.string.save),
            confirmEnabled = name.isNotBlank() && address.isNotBlank(),
            onConfirm = {
                val indexer = build()
                if (indexer.id == 0L) viewModel.addIndexer(indexer) else viewModel.updateIndexer(indexer)
                close()
            },
            dismissText = stringResource(R.string.cancel),
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TvTextField(name, { name = it }, label = stringResource(R.string.external_services_name_label), placeholder = stringResource(R.string.indexers_name_hint))
                ActionsRow {
                    IndexerType.entries.forEach { option ->
                        FilterChip(selected = type == option, onClick = { type = option }) { Text(option.displayName) }
                    }
                }
                TvTextField(
                    address,
                    { address = it },
                    label = stringResource(R.string.external_services_address_label),
                    placeholder = "http://192.168.1.100:${type.defaultPort}",
                    keyboardType = KeyboardType.Uri,
                )
                TvTextField(token, { token = it }, label = stringResource(R.string.indexers_api_token_label), isPassword = true)
                SwitchRow(stringResource(R.string.indexers_active_label), active, { active = it })
                TestRow(
                    testing = uiState.isTestingInDialog,
                    result = uiState.dialogTestResult,
                    enabled = address.isNotBlank(),
                    onTest = { viewModel.testDialogConnection(build()) },
                )
            }
        }
    }

    deleting?.let { indexer ->
        ConfirmDialog(
            title = stringResource(R.string.indexers_delete_confirm_title),
            message = stringResource(R.string.indexers_delete_confirm_message, indexer.type.displayName, indexer.name),
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                viewModel.deleteIndexer(indexer)
                deleting = null
            },
            onDismiss = { deleting = null },
            dismissText = stringResource(R.string.cancel),
        )
    }
}

// ---------------- stremio addons ----------------

@Composable
private fun AddonsTab(viewModel: AddonsViewModel = koinViewModel()) {
    val messenger = LocalMessenger.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<StremioAddonEntity?>(null) }
    var deleting by remember { mutableStateOf<StremioAddonEntity?>(null) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            messenger.show(it)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(uiState.installSuccess) {
        if (uiState.installSuccess) {
            adding = false
            viewModel.clearInstallSuccess()
        }
    }

    ServiceList(
        addLabel = stringResource(R.string.stremio_addon_add),
        onAdd = { adding = true },
        isEmpty = uiState.addons.isEmpty(),
        emptyText = stringResource(R.string.stremio_addons_empty),
        emptyHint = stringResource(R.string.stremio_addons_empty_hint_tv),
    ) {
        items(uiState.addons, key = { it.manifestUrl }) { addon ->
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(addon.displayName, style = MaterialTheme.typography.titleLarge)
                    Badge(stringResource(R.string.version_format, addon.version), color = MaterialTheme.colorScheme.secondary)
                }
                if (addon.description.isNotBlank()) {
                    Text(addon.description, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SwitchRow(stringResource(R.string.enabled), addon.isEnabled, { viewModel.toggleAddonEnabled(addon.manifestUrl, it) })
                ActionsRow {
                    TvActionButton(stringResource(R.string.edit), { editing = addon }, icon = R.drawable.ic_edit)
                    TvActionButton(stringResource(R.string.delete), { deleting = addon }, icon = R.drawable.ic_delete)
                }
            }
        }
    }

    if (adding) {
        var url by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        TvDialog(
            title = stringResource(R.string.stremio_addon_add),
            onDismiss = {
                adding = false
                viewModel.clearError()
            },
            confirmText = if (uiState.isLoading) stringResource(R.string.installing) else stringResource(R.string.add),
            confirmEnabled = url.isNotBlank() && !uiState.isLoading,
            onConfirm = { viewModel.installAddon(url.trim(), name.trim().ifBlank { null }) },
            dismissText = stringResource(R.string.cancel),
        ) {
            TvTextField(
                url,
                {
                    url = it
                    viewModel.clearError()
                },
                label = stringResource(R.string.manifest_url),
                placeholder = stringResource(R.string.stremio_addon_url_hint),
                keyboardType = KeyboardType.Uri,
                isError = uiState.error != null,
                supportingText = uiState.error,
            )
            TvTextField(
                name,
                { name = it },
                label = stringResource(R.string.stremio_addon_custom_name_optional_label),
                placeholder = stringResource(R.string.stremio_addon_custom_name_hint),
            )
        }
    }

    editing?.let { addon ->
        var name by remember(addon) { mutableStateOf(addon.customName.orEmpty()) }
        var enabled by remember(addon) { mutableStateOf(addon.isEnabled) }
        TvDialog(
            title = stringResource(R.string.stremio_addon_edit),
            onDismiss = { editing = null },
            confirmText = stringResource(R.string.save),
            onConfirm = {
                viewModel.updateAddonDetails(addon, name, enabled)
                editing = null
            },
            dismissText = stringResource(R.string.cancel),
        ) {
            Text(stringResource(R.string.stremio_addon_official_name_label) + ": " + addon.name)
            Text(stringResource(R.string.manifest_url) + ": " + addon.manifestUrl, maxLines = 2)
            TvTextField(name, { name = it }, label = stringResource(R.string.stremio_addon_custom_name_label), placeholder = addon.name)
            SwitchRow(stringResource(R.string.enabled), enabled, { enabled = it })
        }
    }

    deleting?.let { addon ->
        ConfirmDialog(
            title = stringResource(R.string.stremio_addon_delete_title),
            message = stringResource(R.string.stremio_addon_delete_message, addon.displayName),
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                viewModel.uninstallAddon(addon.manifestUrl, addon.displayName)
                deleting = null
            },
            onDismiss = { deleting = null },
            dismissText = stringResource(R.string.cancel),
        )
    }
}

// ---------------- shared ----------------

@Composable
private fun ServiceList(
    addLabel: String,
    onAdd: () -> Unit,
    isEmpty: Boolean,
    emptyText: String,
    emptyHint: String,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { TvActionButton(addLabel, onAdd, icon = R.drawable.ic_add, primary = true) }
        if (isEmpty) item { EmptyState(text = emptyText, hint = emptyHint) }
        content()
    }
}

@Composable
private fun ActionsRow(content: @Composable () -> Unit) {
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
}

@Composable
private fun TestRow(
    testing: Boolean,
    result: Result<Unit>?,
    enabled: Boolean,
    onTest: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TvActionButton(
            text = if (testing) stringResource(R.string.testing_connection) else stringResource(R.string.external_services_test_connection),
            icon = R.drawable.ic_refresh,
            loading = testing,
            enabled = enabled,
            onClick = onTest,
        )
        result?.let {
            it.fold(
                onSuccess = { Text("✓", color = MaterialTheme.colorScheme.primary) },
                onFailure = { error ->
                    Text(stringResource(R.string.connection_failed, error.message.orEmpty()), color = MaterialTheme.colorScheme.error, maxLines = 2)
                },
            )
        }
    }
}
