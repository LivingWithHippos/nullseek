package com.github.livingwithhippos.nullseek.tv.ui.providers

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.data.database.model.NullProvider
import com.github.livingwithhippos.nullseek.tv.ui.components.Badge
import com.github.livingwithhippos.nullseek.tv.ui.components.ConfirmDialog
import com.github.livingwithhippos.nullseek.tv.ui.components.EmptyState
import com.github.livingwithhippos.nullseek.tv.ui.components.LocalMessenger
import com.github.livingwithhippos.nullseek.tv.ui.components.ScreenHeader
import com.github.livingwithhippos.nullseek.tv.ui.components.SectionCard
import com.github.livingwithhippos.nullseek.tv.ui.components.TvActionButton
import com.github.livingwithhippos.nullseek.tv.ui.components.TvDialog
import com.github.livingwithhippos.nullseek.tv.ui.components.TvTextField
import com.github.livingwithhippos.nullseek.ui.providers.ProvidersEvent
import com.github.livingwithhippos.nullseek.ui.providers.ProvidersViewModel
import org.koin.androidx.compose.koinViewModel

private sealed interface ProviderDialog {
    data object Add : ProviderDialog

    data class Rename(val provider: NullProvider) : ProviderDialog

    data class ReplaceToken(val provider: NullProvider) : ProviderDialog

    data class Delete(val provider: NullProvider) : ProviderDialog
}

@Composable
fun TvProvidersScreen(
    onBack: () -> Unit,
    onProvidersChanged: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProvidersViewModel = koinViewModel(),
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnProvidersChanged by rememberUpdatedState(onProvidersChanged)
    var dialog by remember { mutableStateOf<ProviderDialog?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ProvidersEvent.ShowMessage ->
                    messenger.show(
                        if (event.arg != null) context.getString(event.resId, event.arg) else context.getString(event.resId),
                    )
                ProvidersEvent.ActiveChanged -> currentOnProvidersChanged()
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(R.string.providers_title), subtitle = stringResource(R.string.providers_summary)) {
            TvActionButton(stringResource(R.string.add), { dialog = ProviderDialog.Add }, icon = R.drawable.ic_add, primary = true)
            TvActionButton(stringResource(R.string.close), onBack, icon = R.drawable.ic_close)
        }
        if (providers.isEmpty()) {
            EmptyState(text = stringResource(R.string.providers_empty))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(providers, key = { it.id }) { provider ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(provider.name, style = androidx.tv.material3.MaterialTheme.typography.titleLarge)
                        if (provider.active) Badge(stringResource(R.string.providers_default))
                    }
                    Text("••••" + provider.token.takeLast(4))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!provider.active) {
                            TvActionButton(stringResource(R.string.providers_set_default), { viewModel.setDefault(provider) }, icon = R.drawable.ic_contact_page)
                        }
                        TvActionButton(stringResource(R.string.providers_rename), { dialog = ProviderDialog.Rename(provider) }, icon = R.drawable.ic_edit)
                        TvActionButton(
                            stringResource(R.string.providers_replace_token),
                            { dialog = ProviderDialog.ReplaceToken(provider) },
                            icon = R.drawable.ic_settings,
                        )
                        TvActionButton(stringResource(R.string.providers_delete), { dialog = ProviderDialog.Delete(provider) }, icon = R.drawable.ic_delete)
                    }
                }
            }
        }
    }

    when (val current = dialog) {
        null -> {}
        ProviderDialog.Add -> {
            var name by remember { mutableStateOf("") }
            var token by remember { mutableStateOf("") }
            TvDialog(
                title = stringResource(R.string.providers_add_title),
                onDismiss = {
                    viewModel.clearError()
                    dialog = null
                },
                confirmText = if (uiState.validating) stringResource(R.string.providers_validating) else stringResource(R.string.add),
                confirmEnabled = name.isNotBlank() && token.isNotBlank() && !uiState.validating,
                onConfirm = { viewModel.add(name, token) { added -> if (added) dialog = null } },
                dismissText = stringResource(R.string.cancel),
            ) {
                TvTextField(name, { name = it }, label = stringResource(R.string.providers_name_label))
                TvTextField(
                    token,
                    {
                        token = it
                        viewModel.clearError()
                    },
                    label = stringResource(R.string.providers_token_label),
                    isPassword = true,
                    isError = uiState.tokenError,
                    supportingText = if (uiState.tokenError) stringResource(R.string.providers_invalid_token) else null,
                )
            }
        }

        is ProviderDialog.Rename -> {
            var name by remember(current) { mutableStateOf(current.provider.name) }
            TvDialog(
                title = stringResource(R.string.providers_rename),
                onDismiss = { dialog = null },
                confirmText = stringResource(R.string.save),
                confirmEnabled = name.isNotBlank(),
                onConfirm = {
                    viewModel.rename(current.provider, name)
                    dialog = null
                },
                dismissText = stringResource(R.string.cancel),
            ) {
                TvTextField(name, { name = it }, label = stringResource(R.string.providers_name_label))
            }
        }

        is ProviderDialog.ReplaceToken -> {
            var token by remember(current) { mutableStateOf("") }
            TvDialog(
                title = stringResource(R.string.providers_replace_token),
                onDismiss = {
                    viewModel.clearError()
                    dialog = null
                },
                confirmText = if (uiState.validating) stringResource(R.string.providers_validating) else stringResource(R.string.save),
                confirmEnabled = token.isNotBlank() && !uiState.validating,
                onConfirm = { viewModel.replaceToken(current.provider, token) { ok -> if (ok) dialog = null } },
                dismissText = stringResource(R.string.cancel),
            ) {
                TvTextField(
                    token,
                    {
                        token = it
                        viewModel.clearError()
                    },
                    label = stringResource(R.string.providers_token_label),
                    isPassword = true,
                    isError = uiState.tokenError,
                    supportingText = if (uiState.tokenError) stringResource(R.string.providers_invalid_token) else null,
                )
            }
        }

        is ProviderDialog.Delete ->
            ConfirmDialog(
                title = stringResource(R.string.providers_delete_title),
                message = stringResource(R.string.providers_delete_message, current.provider.name),
                confirmText = stringResource(R.string.delete),
                onConfirm = {
                    viewModel.delete(current.provider)
                    dialog = null
                },
                onDismiss = { dialog = null },
                dismissText = stringResource(R.string.cancel),
            )
    }
}
