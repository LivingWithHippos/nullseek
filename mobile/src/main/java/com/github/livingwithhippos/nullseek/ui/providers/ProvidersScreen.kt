package com.github.livingwithhippos.nullseek.ui.providers

import android.annotation.SuppressLint
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.data.NetProvider
import com.github.livingwithhippos.nullseek.core.domain.ProviderKind
import com.github.livingwithhippos.nullseek.ui.provider.ProviderKindSelector
import com.github.livingwithhippos.nullseek.core.data.database.model.NullProvider
import com.github.livingwithhippos.nullseek.ui.theme.Button
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.theme.OutlinedButton
import com.github.livingwithhippos.nullseek.ui.theme.TextButton
import com.github.livingwithhippos.nullseek.utils.getClipboardText
import com.github.livingwithhippos.nullseek.utils.openWebPage
import org.koin.androidx.compose.koinViewModel

private sealed interface ProviderDialog {
    data object Add : ProviderDialog

    data class Edit(
        val provider: NullProvider,
    ) : ProviderDialog

    data class Delete(
        val provider: NullProvider,
    ) : ProviderDialog
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun ProvidersScreen(
    onNavigateBack: () -> Unit,
    onProvidersChanged: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProvidersViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentOnChanged by rememberUpdatedState(onProvidersChanged)
    var dialog by remember { mutableStateOf<ProviderDialog?>(null) }
    // null shows every provider
    var filter by rememberSaveable { mutableStateOf<ProviderKind?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProvidersEvent.ShowMessage -> {
                    val res = event.resId
                    val arg = event.arg
                    snackbarHostState.showSnackbar(
                        if (arg != null) context.getString(
                            res, arg
                        ) else context.getString(res),
                    )
                }

                ProvidersEvent.ActiveChanged -> currentOnChanged()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.providers_title)) },
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = filter == null,
                        onClick = { filter = null },
                        label = { Text(stringResource(id = R.string.providers_filter_all)) },
                    )
                    ProviderKind.entries.forEach { kind ->
                        FilterChip(
                            selected = filter == kind,
                            onClick = { filter = kind },
                            label = { Text(kind.displayName) },
                        )
                    }
                }
                Button(onClick = {
                    viewModel.clearError()
                    dialog = ProviderDialog.Add
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(id = R.string.add))
                }

                val shownProviders = providers.filter { filter == null || ProviderKind.fromDbType(it.type) == filter }
                if (providers.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.providers_empty),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else if (shownProviders.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.providers_empty_filter, filter?.displayName.orEmpty()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                shownProviders.forEach { provider ->
                    ProviderCard(
                        provider = provider,
                        onSetDefault = { viewModel.setDefault(provider) },
                        onEdit = {
                            viewModel.clearError()
                            dialog = ProviderDialog.Edit(provider)
                        },
                    )
                }
            }
        }
    }

    when (val d = dialog) {
        null -> {}

        ProviderDialog.Add -> {
            AddProviderDialog(
                initialKind = filter ?: ProviderKind.TORBOX,
                validating = uiState.validating,
                tokenError = uiState.tokenError,
                onTokenChange = viewModel::clearError,
                onDismiss = { dialog = null },
                onConfirm = { kind, name, token ->
                    viewModel.add(kind, name, token) { ok -> if (ok) dialog = null }
                },
            )
        }

        is ProviderDialog.Edit -> {
            EditProviderDialog(
                provider = d.provider,
                validating = uiState.validating,
                tokenError = uiState.tokenError,
                onTokenChange = viewModel::clearError,
                onDismiss = { dialog = null },
                onDelete = { dialog = ProviderDialog.Delete(d.provider) },
                onConfirm = { name, token ->
                    viewModel.edit(d.provider, name, token) { ok -> if (ok) dialog = null }
                },
            )
        }

        is ProviderDialog.Delete -> {
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text(stringResource(id = R.string.providers_delete_title)) },
                text = {
                    Text(
                        stringResource(
                            id = R.string.providers_delete_message,
                            d.provider.name,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.delete(d.provider)
                        dialog = null
                    }) { Text(stringResource(id = R.string.providers_delete)) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        dialog = null
                    }) { Text(stringResource(id = R.string.cancel)) }
                },
            )
        }
    }
}

@Composable
private fun ProviderCard(
    provider: NullProvider,
    onSetDefault: () -> Unit,
    onEdit: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                )
                Surface(
                    shape = RoundedCornerShape(0.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    Text(
                        text = ProviderKind.fromDbType(provider.type).displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                if (provider.active) {
                    Surface(
                        shape = RoundedCornerShape(0.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = stringResource(id = R.string.active),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(id = R.string.providers_edit))
                }
                if (!provider.active) {
                    OutlinedButton(
                        onClick = onSetDefault,
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                    ) { Text(stringResource(id = R.string.set_active)) }
                }
            }
        }
    }
}

/** The API token input, with buttons to open the provider token page and to paste the clipboard. */
@Composable
private fun TokenField(
    value: String,
    onValueChange: (String) -> Unit,
    kind: ProviderKind,
    enabled: Boolean,
    isError: Boolean,
    supportingText: String? = null,
) {
    val context = LocalContext.current
    val provider = remember(kind) { NetProvider.of(kind) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(id = R.string.providers_token_label)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        isError = isError,
        supportingText =
            when {
                isError -> {
                    { Text(stringResource(id = R.string.providers_invalid_token)) }
                }

                supportingText != null -> {
                    { Text(supportingText) }
                }

                else -> {
                    null
                }
            },
        enabled = enabled,
        trailingIcon = {
            Row {
                IconButton(onClick = { openWebPage(context, provider.tokenLink) }, enabled = enabled) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_open_in_new),
                        contentDescription = stringResource(id = R.string.setup_get_key_button_provider, provider.name),
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = { getClipboardText(context)?.let { onValueChange(it.trim()) } },
                    enabled = enabled,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_paste),
                        contentDescription = stringResource(id = R.string.paste_from_clipboard),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
    )
}

@Composable
private fun ValidatingRow() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.width(8.dp))
        Text(stringResource(id = R.string.providers_validating))
    }
}

@Composable
private fun AddProviderDialog(
    initialKind: ProviderKind,
    validating: Boolean,
    tokenError: Boolean,
    onTokenChange: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (kind: ProviderKind, name: String, token: String) -> Unit,
) {
    var kind by rememberSaveable { mutableStateOf(initialKind) }
    var name by rememberSaveable { mutableStateOf(initialKind.displayName) }
    var token by rememberSaveable { mutableStateOf("") }
    val valid = name.isNotBlank() && token.isNotBlank()

    AlertDialog(
        onDismissRequest = { if (!validating) onDismiss() },
        title = { Text(stringResource(id = R.string.providers_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(id = R.string.provider_service),
                    style = MaterialTheme.typography.labelMedium,
                )
                ProviderKindSelector(
                    selected = kind,
                    onSelect = { newKind ->
                        // keep a custom name, follow the service otherwise
                        if (name.isBlank() || ProviderKind.entries.any { it.displayName == name }) {
                            name = newKind.displayName
                        }
                        kind = newKind
                    },
                    enabled = !validating,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.providers_name_label)) },
                    singleLine = true,
                    enabled = !validating,
                )
                TokenField(
                    value = token,
                    onValueChange = {
                        token = it
                        onTokenChange()
                    },
                    kind = kind,
                    enabled = !validating,
                    isError = tokenError,
                )
                if (validating) ValidatingRow()
            }
        },
        confirmButton = {
            TextButton(enabled = valid && !validating, onClick = { onConfirm(kind, name, token) }) {
                Text(stringResource(id = R.string.save))
            }
        },
        dismissButton = {
            TextButton(enabled = !validating, onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        },
    )
}

/** Renames an account and optionally replaces its token. */
@Composable
private fun EditProviderDialog(
    provider: NullProvider,
    validating: Boolean,
    tokenError: Boolean,
    onTokenChange: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onConfirm: (name: String, token: String?) -> Unit,
) {
    val kind = ProviderKind.fromDbType(provider.type)
    var name by rememberSaveable(provider.id) { mutableStateOf(provider.name) }
    var token by rememberSaveable(provider.id) { mutableStateOf("") }
    val changed = name.trim() != provider.name || token.isNotBlank()
    val valid = name.isNotBlank() && changed

    AlertDialog(
        onDismissRequest = { if (!validating) onDismiss() },
        title = { Text(stringResource(id = R.string.providers_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.providers_name_label)) },
                    singleLine = true,
                    enabled = !validating,
                )
                TokenField(
                    value = token,
                    onValueChange = {
                        token = it
                        onTokenChange()
                    },
                    kind = kind,
                    enabled = !validating,
                    isError = tokenError,
                    supportingText = stringResource(id = R.string.providers_token_keep_hint),
                )
                if (validating) ValidatingRow()
                TextButton(
                    onClick = onDelete,
                    enabled = !validating,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(id = R.string.providers_delete))
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid && !validating, onClick = { onConfirm(name, token.takeIf { it.isNotBlank() }) }) {
                Text(stringResource(id = R.string.save))
            }
        },
        dismissButton = {
            TextButton(enabled = !validating, onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        },
    )
}

@Preview
@Composable
private fun ProviderCardPreview() {
    NullSeekTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProviderCard(
                    provider =
                        NullProvider(
                            id = 1,
                            name = "Example Provider",
                            token = "token",
                            active = false,
                            type = ProviderKind.REAL_DEBRID.dbType,
                        ),
                    onSetDefault = {},
                    onEdit = {},
                )
                ProviderCard(
                    provider =
                        NullProvider(
                            id = 2,
                            name = "Main",
                            token = "token",
                            active = true,
                            type = ProviderKind.TORBOX.dbType,
                        ),
                    onSetDefault = {},
                    onEdit = {},
                )
            }
        }
    }
}
