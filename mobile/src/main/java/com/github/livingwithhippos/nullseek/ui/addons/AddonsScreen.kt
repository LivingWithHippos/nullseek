package com.github.livingwithhippos.nullseek.ui.addons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.core.data.database.model.StremioAddonEntity
import com.github.livingwithhippos.nullseek.ui.externalservices.AddonDeleteDialog
import com.github.livingwithhippos.nullseek.ui.externalservices.AddonEditDialog
import com.github.livingwithhippos.nullseek.ui.externalservices.AddonsSection
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddonsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddonsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val addons by viewModel.addons.collectAsState()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingAddonUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingAddonUrl by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.installSuccess) {
        if (uiState.installSuccess) {
            showAddDialog = false
            viewModel.clearInstallSuccess()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.stremio_addons_title)) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier =
                    Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                AddonsSection(
                    addons = addons,
                    onAdd = { showAddDialog = true },
                    onToggleEnabled = { addon, enabled ->
                        viewModel.toggleAddonEnabled(addon.manifestUrl, enabled)
                    },
                    onEdit = { addon ->
                        editingAddonUrl = addon.manifestUrl
                    },
                    onDelete = { addon ->
                        deletingAddonUrl = addon.manifestUrl
                    },
                )
            }
        }
    }

    if (showAddDialog) {
        AddonEditDialog(
            initialAddon = null,
            isInstalling = uiState.isLoading,
            onDismiss = {
                if (!uiState.isLoading) {
                    showAddDialog = false
                }
            },
            onInstall = { url, customName ->
                viewModel.installAddon(url, customName)
            },
        )
    }

    val editingAddon: StremioAddonEntity? =
        editingAddonUrl?.let { url -> addons.firstOrNull { it.manifestUrl == url } }
    if (editingAddon != null) {
        AddonEditDialog(
            initialAddon = editingAddon,
            isInstalling = false,
            onDismiss = { editingAddonUrl = null },
            onInstall = { _, _ -> },
            onUpdate = { addon, customName, enabled ->
                viewModel.updateAddonDetails(addon, customName, enabled)
                editingAddonUrl = null
            },
        )
    }

    val deletingAddon: StremioAddonEntity? =
        deletingAddonUrl?.let { url -> addons.firstOrNull { it.manifestUrl == url } }
    if (deletingAddon != null) {
        AddonDeleteDialog(
            addon = deletingAddon,
            onConfirm = {
                viewModel.uninstallAddon(deletingAddon.manifestUrl, deletingAddon.displayName)
                deletingAddonUrl = null
            },
            onDismiss = { deletingAddonUrl = null },
        )
    }
}
