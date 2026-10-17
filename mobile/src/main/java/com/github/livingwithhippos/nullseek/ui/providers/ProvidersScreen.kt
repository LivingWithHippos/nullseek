package com.github.livingwithhippos.nullseek.ui.providers

import android.annotation.SuppressLint
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.core.data.NetProvider
import com.github.livingwithhippos.nullseek.core.data.database.model.NullProvider
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import org.koin.androidx.compose.koinViewModel

private sealed interface ProviderDialog {
    data object Add : ProviderDialog

    data class Rename(
        val provider: NullProvider,
    ) : ProviderDialog

    data class Token(
        val provider: NullProvider,
    ) : ProviderDialog

    data class Delete(
        val provider: NullProvider,
    ) : ProviderDialog
}

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
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
                }
                if (providers.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.providers_empty),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                providers.forEach { provider ->
                    ProviderCard(
                        provider = provider,
                        onSetDefault = { viewModel.setDefault(provider) },
                        onRename = { dialog = ProviderDialog.Rename(provider) },
                        onReplaceToken = {
                            viewModel.clearError()
                            dialog = ProviderDialog.Token(provider)
                        },
                        onDelete = { dialog = ProviderDialog.Delete(provider) },
                    )
                }
            }
        }
    }

    when (val d = dialog) {
        null -> {}
        ProviderDialog.Add -> ProviderInputDialog(
            title = stringResource(id = R.string.providers_add_title),
            showName = true,
            showToken = true,
            initialName = "",
            validating = uiState.validating,
            tokenError = uiState.tokenError,
            onDismiss = { dialog = null },
            onConfirm = { name, token ->
                viewModel.add(name, token) { ok -> if (ok) dialog = null }
            },
        )

        is ProviderDialog.Rename -> ProviderInputDialog(
            title = stringResource(id = R.string.providers_rename),
            showName = true,
            showToken = false,
            initialName = d.provider.name,
            validating = false,
            tokenError = false,
            onDismiss = { dialog = null },
            onConfirm = { name, _ ->
                viewModel.rename(d.provider, name)
                dialog = null
            },
        )

        is ProviderDialog.Token -> ProviderInputDialog(
            title = stringResource(id = R.string.providers_replace_token),
            showName = false,
            showToken = true,
            initialName = "",
            validating = uiState.validating,
            tokenError = uiState.tokenError,
            onDismiss = { dialog = null },
            onConfirm = { _, token ->
                viewModel.replaceToken(d.provider, token) { ok -> if (ok) dialog = null }
            },
        )

        is ProviderDialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(id = R.string.providers_delete_title)) },
            text = {
                Text(
                    stringResource(
                        id = R.string.providers_delete_message, d.provider.name
                    )
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

@Composable
private fun ProviderCard(
    provider: NullProvider,
    onSetDefault: () -> Unit,
    onRename: () -> Unit,
    onReplaceToken: () -> Unit,
    onDelete: () -> Unit,
) {
    // todo: just show info and open another screen with actions on click
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
                if (provider.active) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
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
                OutlinedButton(onClick = onRename) { Text(stringResource(id = R.string.providers_rename)) }
                if (!provider.active) {
                    OutlinedButton(
                        onClick = onSetDefault,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) { Text(stringResource(id = R.string.set_active)) }
                }
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(onClick = onReplaceToken) { Text(stringResource(id = R.string.providers_replace_token)) }

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
    }
}

@Composable
private fun ProviderInputDialog(
    title: String,
    showName: Boolean,
    showToken: Boolean,
    initialName: String,
    validating: Boolean,
    tokenError: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, token: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var token by rememberSaveable { mutableStateOf("") }
    val valid = (!showName || name.isNotBlank()) && (!showToken || token.isNotBlank())

    AlertDialog(
        onDismissRequest = { if (!validating) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showName) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(id = R.string.providers_name_label)) },
                        singleLine = true,
                        enabled = !validating,
                    )
                }
                if (showToken) {
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = { Text(stringResource(id = R.string.providers_token_label)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = tokenError,
                        supportingText = if (tokenError) {
                            { Text(stringResource(id = R.string.providers_invalid_token)) }
                        } else {
                            null
                        },
                        enabled = !validating,
                    )
                }
                if (validating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp), strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(id = R.string.providers_validating))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid && !validating, onClick = { onConfirm(name, token) }) {
                Text(stringResource(id = R.string.save))
            }
        },
        dismissButton = {
            TextButton(
                enabled = !validating, onClick = onDismiss
            ) { Text(stringResource(id = R.string.cancel)) }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                ProviderCard(
                    provider = NullProvider(
                        id = 1,
                        name = "Example Provider",
                        token = "token",
                        active = false,
                        type = NetProvider.Torbox().type
                    ),
                    onSetDefault = {},
                    onRename = {},
                    onReplaceToken = {},
                    onDelete = {},
                )
                ProviderCard(
                    provider = NullProvider(
                        id = 2,
                        name = "Main",
                        token = "token",
                        active = true,
                        type = NetProvider.Torbox().type
                    ),
                    onSetDefault = {},
                    onRename = {},
                    onReplaceToken = {},
                    onDelete = {},
                )
            }
        }
    }
}