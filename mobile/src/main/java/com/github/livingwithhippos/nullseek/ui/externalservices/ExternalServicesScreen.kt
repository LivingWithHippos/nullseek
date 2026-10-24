package com.github.livingwithhippos.nullseek.ui.externalservices

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.data.model.ExternalService
import com.github.livingwithhippos.nullseek.data.model.ExternalServiceCategory
import com.github.livingwithhippos.nullseek.data.model.ExternalServiceType
import com.github.livingwithhippos.nullseek.data.model.Indexer
import com.github.livingwithhippos.nullseek.ui.addons.AddonsViewModel
import com.github.livingwithhippos.nullseek.ui.theme.Button
import com.github.livingwithhippos.nullseek.ui.theme.FilledTonalButton
import com.github.livingwithhippos.nullseek.ui.theme.FilterChip
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.theme.OutlinedButton
import com.github.livingwithhippos.nullseek.ui.theme.TextButton
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import com.github.livingwithhippos.nullseek.utils.openWebPage
import org.koin.androidx.compose.koinViewModel

private fun hasLocalNetworkPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_LOCAL_NETWORK,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

private fun openAppSettings(context: Context) {
    val intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    context.startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalServicesScreen(
    contentType: NullContentType,
    navigationType: NullNavigationType,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExternalServicesViewModel = koinViewModel(),
    indexersViewModel: IndexersViewModel = koinViewModel(),
    addonsViewModel: AddonsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val services by viewModel.services.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val indexers by indexersViewModel.indexers.collectAsStateWithLifecycle()
    val indexersState by indexersViewModel.uiState.collectAsStateWithLifecycle()
    val addons by addonsViewModel.addons.collectAsStateWithLifecycle()
    val addonsUiState by addonsViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var hasPermission by remember { mutableStateOf(hasLocalNetworkPermission(context)) }
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            hasPermission = isGranted
        }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasPermission = hasLocalNetworkPermission(context)
    }

    var serviceToEdit by remember { mutableStateOf<ExternalService?>(null) }
    var isAddingService by remember { mutableStateOf(false) }
    var serviceToDelete by remember { mutableStateOf<ExternalService?>(null) }
    var isAddingIndexer by remember { mutableStateOf(false) }
    var indexerToEdit by remember { mutableStateOf<Indexer?>(null) }
    var indexerToDelete by remember { mutableStateOf<Indexer?>(null) }
    var isAddingAddon by remember { mutableStateOf(false) }
    var addonToEdit by remember { mutableStateOf<com.github.livingwithhippos.nullseek.core.data.database.model.StremioAddonEntity?>(null) }
    var addonToDelete by remember { mutableStateOf<com.github.livingwithhippos.nullseek.core.data.database.model.StremioAddonEntity?>(null) }

    LaunchedEffect(Unit) {
        indexersViewModel.events.collect { event ->
            if (event is ExternalServicesEvent.ShowMessage) snackbarHostState.showSnackbar(event.message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExternalServicesEvent.ShowMessage ->
                    snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(addonsUiState.error) {
        addonsUiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            addonsViewModel.clearError()
        }
    }

    LaunchedEffect(addonsUiState.installSuccess) {
        if (addonsUiState.installSuccess) {
            isAddingAddon = false
            addonToEdit = null
            addonsViewModel.clearInstallSuccess()
        }
    }

    LaunchedEffect(addonsUiState.message) {
        addonsUiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            addonsViewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.external_services_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = stringResource(id = R.string.close),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (!hasPermission) {
                    LocalNetworkPermissionWarningCard(
                        onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                                permissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                            }
                        },
                        onOpenSettings = {
                            openAppSettings(context)
                        },
                    )
                }

                // Media Players Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = stringResource(id = R.string.external_services_media_players_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Kodi and VLC remote playback",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = { isAddingService = true },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(id = R.string.add))
                    }
                }

                val mediaPlayers =
                    services.filter { it.type.category == ExternalServiceCategory.MEDIA_PLAYER }

                if (mediaPlayers.isEmpty()) {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(id = R.string.external_services_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(id = R.string.external_services_empty_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    mediaPlayers.forEach { service ->
                        ServiceCard(
                            service = service,
                            isTesting = uiState.testingServiceId == service.id,
                            onSetDefault = {
                                viewModel.setDefault(
                                    service.id,
                                    service.type.category,
                                    service.name
                                )
                            },
                            onTestConnection = {
                                viewModel.testConnection(service)
                            },
                            onEdit = {
                                serviceToEdit = service
                            },
                            onDelete = {
                                serviceToDelete = service
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                IndexersSection(
                    indexers = indexers,
                    testingIndexerId = indexersState.testingIndexerId,
                    onAdd = { isAddingIndexer = true },
                    onToggleActive = { indexer, active -> indexersViewModel.setActive(indexer, active) },
                    onTest = { indexersViewModel.testConnection(it) },
                    onEdit = { indexerToEdit = it },
                    onDelete = { indexerToDelete = it },
                )

                Spacer(modifier = Modifier.height(16.dp))

                AddonsSection(
                    addons = addons,
                    onAdd = { isAddingAddon = true },
                    onToggleEnabled = { addon, enabled ->
                        addonsViewModel.toggleAddonEnabled(addon.manifestUrl, enabled)
                    },
                    onEdit = { addonToEdit = it },
                    onDelete = { addonToDelete = it },
                )
            }
        }
    }

    if (isAddingIndexer || indexerToEdit != null) {
        IndexerEditDialog(
            initialIndexer = indexerToEdit,
            isTesting = indexersState.isTestingInDialog,
            testResult = indexersState.dialogTestResult,
            onTest = { indexersViewModel.testDialogConnection(it) },
            onDismiss = {
                isAddingIndexer = false
                indexerToEdit = null
                indexersViewModel.clearDialogTestResult()
            },
            onSave = { indexer ->
                if (indexer.id == 0L) {
                    indexersViewModel.addIndexer(indexer)
                } else {
                    indexersViewModel.updateIndexer(indexer)
                }
                isAddingIndexer = false
                indexerToEdit = null
                indexersViewModel.clearDialogTestResult()
            },
        )
    }

    indexerToDelete?.let { indexer ->
        IndexerDeleteDialog(
            indexer = indexer,
            onConfirm = {
                indexersViewModel.deleteIndexer(indexer)
                indexerToDelete = null
            },
            onDismiss = { indexerToDelete = null },
        )
    }

    if (isAddingService) {
        ServiceEditDialog(
            initialService = null,
            isTesting = uiState.isTestingInDialog,
            testResult = uiState.dialogTestResult,
            onTest = { service -> viewModel.testDialogConnection(service) },
            onDismiss = {
                isAddingService = false
                viewModel.clearDialogTestResult()
            },
            onSave = { service ->
                viewModel.addService(service)
                isAddingService = false
                viewModel.clearDialogTestResult()
            },
        )
    }

    serviceToEdit?.let { service ->
        ServiceEditDialog(
            initialService = service,
            isTesting = uiState.isTestingInDialog,
            testResult = uiState.dialogTestResult,
            onTest = { s -> viewModel.testDialogConnection(s) },
            onDismiss = {
                serviceToEdit = null
                viewModel.clearDialogTestResult()
            },
            onSave = { updated ->
                if (updated.id == 0L) {
                    viewModel.addService(updated)
                } else {
                    viewModel.updateService(updated)
                }
                serviceToEdit = null
                viewModel.clearDialogTestResult()
            },
        )
    }

    serviceToDelete?.let { service ->
        AlertDialog(
            onDismissRequest = { serviceToDelete = null },
            title = { Text(text = stringResource(id = R.string.external_services_delete_confirm_title)) },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.external_services_delete_confirm_message,
                        service.type.displayName,
                        service.name,
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteService(service.id, service.name)
                        serviceToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(text = stringResource(id = R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { serviceToDelete = null }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }

    if (isAddingAddon || addonToEdit != null) {
        AddonEditDialog(
            initialAddon = addonToEdit,
            isInstalling = addonsUiState.isLoading,
            onDismiss = {
                if (!addonsUiState.isLoading) {
                    isAddingAddon = false
                    addonToEdit = null
                }
            },
            onInstall = { manifestUrl, customName ->
                addonsViewModel.installAddon(manifestUrl, customName)
            },
            onUpdate = { addon, customName, enabled ->
                addonsViewModel.updateAddonDetails(addon, customName, enabled)
                isAddingAddon = false
                addonToEdit = null
            },
        )
    }

    addonToDelete?.let { addon ->
        AddonDeleteDialog(
            addon = addon,
            onConfirm = {
                addonsViewModel.uninstallAddon(addon.manifestUrl, addon.displayName)
                addonToDelete = null
            },
            onDismiss = { addonToDelete = null },
        )
    }
}

@Composable
private fun ServiceCard(
    service: ExternalService,
    isTesting: Boolean,
    onSetDefault: () -> Unit,
    onTestConnection: () -> Unit,
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
                    modifier =
                        Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onEdit,
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = service.name,
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
                            text = service.type.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    if (service.isDefault) {
                        Surface(
                            shape = RoundedCornerShape(0.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = stringResource(id = R.string.external_services_default_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }

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
                text = service.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onTestConnection,
                    enabled = !isTesting,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(text = stringResource(id = R.string.external_services_test_connection))
                }

                if (!service.isDefault) {
                    FilledTonalButton(
                        onClick = onSetDefault,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(id = R.string.external_services_set_default))
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceEditDialog(
    initialService: ExternalService?,
    isTesting: Boolean,
    testResult: Result<Unit>?,
    onTest: (ExternalService) -> Unit,
    onDismiss: () -> Unit,
    onSave: (ExternalService) -> Unit,
) {
    var serviceId by rememberSaveable { mutableStateOf(initialService?.id ?: 0L) }
    var name by rememberSaveable { mutableStateOf(initialService?.name.orEmpty()) }
    var selectedType by rememberSaveable {
        mutableStateOf(
            initialService?.type ?: ExternalServiceType.KODI
        )
    }
    var address by rememberSaveable { mutableStateOf(initialService?.address.orEmpty()) }
    var username by rememberSaveable {
        mutableStateOf(
            if (initialService?.type == ExternalServiceType.VLC) "" else initialService?.username.orEmpty(),
        )
    }
    var password by rememberSaveable { mutableStateOf(initialService?.password.orEmpty()) }
    var isDefault by rememberSaveable { mutableStateOf(initialService?.isDefault ?: false) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    fun currentService() = ExternalService(
        id = serviceId,
        name = name.trim(),
        type = selectedType,
        address = address.trim(),
        username = if (selectedType == ExternalServiceType.VLC) "" else username.trim(),
        password = password,
        isDefault = isDefault,
    )

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        id = if (serviceId == 0L) {
                            R.string.external_services_dialog_add_title
                        } else {
                            R.string.external_services_dialog_edit_title
                        }
                    )
                )
                IconButton(
                    onClick = {
                        val helpUrl =
                            when (selectedType) {
                                ExternalServiceType.VLC -> "https://wiki.videolan.org/Documentation:Modules/http_intf"
                                else -> "https://kodi.wiki/view/Settings/Services/Control"
                            }
                        openWebPage(context, helpUrl)
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_help),
                        contentDescription = stringResource(id = R.string.help),
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedType == ExternalServiceType.KODI,
                        onClick = {
                            selectedType = ExternalServiceType.KODI
                            if (address.isBlank()) address = "192.168.1.100:8080"
                        },
                        label = { Text("Kodi") },
                    )
                    FilterChip(
                        selected = selectedType == ExternalServiceType.VLC,
                        onClick = {
                            selectedType = ExternalServiceType.VLC
                            username = ""
                            if (address.isBlank()) address = "192.168.1.100:9090"
                        },
                        label = { Text("VLC") },
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = stringResource(id = R.string.external_services_name_label)) },
                    placeholder = { Text(text = stringResource(id = R.string.external_services_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(text = stringResource(id = R.string.external_services_address_label)) },
                    placeholder = { Text(text = stringResource(id = R.string.external_services_address_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    enabled = selectedType != ExternalServiceType.VLC,
                    label = { Text(text = stringResource(id = R.string.external_services_username_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(text = stringResource(id = R.string.external_services_password_label)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                painter = painterResource(id = if (passwordVisible) R.drawable.ic_close else R.drawable.ic_contact_page),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    singleLine = true,
                    supportingText = {
                        if (selectedType == ExternalServiceType.KODI) {
                            Text(text = stringResource(id = R.string.external_services_password_kodi_hint))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDefault = !isDefault }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.external_services_set_as_default_label),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                // Test Connection button inside dialog
                OutlinedButton(
                    onClick = { onTest(currentService()) },
                    enabled = !isTesting && address.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = R.string.testing_connection))
                    } else {
                        Text(text = stringResource(id = R.string.external_services_test_connection))
                    }
                }

                if (serviceId != 0L) {
                    OutlinedButton(
                        onClick = {
                            serviceId = 0L
                            name = ""
                            isDefault = false
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
                onClick = { onSave(currentService()) },
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
private fun LocalNetworkPermissionWarningCard(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_warning),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(id = R.string.external_services_local_network_warning_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            Text(
                text = stringResource(id = R.string.external_services_local_network_warning_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                OutlinedButton(
                    onClick = onOpenSettings,
                ) {
                    Text(text = stringResource(id = R.string.external_services_open_settings))
                }
                Button(
                    onClick = onRequestPermission,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                ) {
                    Text(text = stringResource(id = R.string.external_services_grant_permission))
                }
            }
        }
    }
}

@Preview
@Composable
fun LocalNetworkPermissionWarningCardPreview() {
    NullSeekTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            LocalNetworkPermissionWarningCard(
                onRequestPermission = {},
                onOpenSettings = {},
            )
        }
    }
}

@Preview
@Composable
fun ServiceCardPreview() {
    NullSeekTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            ServiceCard(
                service =
                    ExternalService(
                        id = 1L,
                        name = "Living Room Kodi",
                        type = ExternalServiceType.KODI,
                        address = "http://192.168.1.100:8080",
                        username = "kodi",
                        password = "password",
                        isDefault = true,
                    ),
                isTesting = false,
                onSetDefault = {},
                onTestConnection = {},
                onEdit = {},
                onDelete = {},
            )
        }
    }
}



