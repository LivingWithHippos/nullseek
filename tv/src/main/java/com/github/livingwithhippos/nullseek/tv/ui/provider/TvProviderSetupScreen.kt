package com.github.livingwithhippos.nullseek.tv.ui.provider

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.livingwithhippos.nullseek.NullRootUiState
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.data.NetProvider
import com.github.livingwithhippos.nullseek.tv.ui.components.ErrorBanner
import com.github.livingwithhippos.nullseek.tv.ui.components.LocalMessenger
import com.github.livingwithhippos.nullseek.tv.ui.components.ScreenHeader
import com.github.livingwithhippos.nullseek.tv.ui.components.SectionCard
import com.github.livingwithhippos.nullseek.tv.ui.components.TvActionButton
import com.github.livingwithhippos.nullseek.tv.ui.components.TvDialog
import com.github.livingwithhippos.nullseek.tv.ui.components.TvTextField
import com.github.livingwithhippos.nullseek.tv.ui.components.launchSafely
import com.github.livingwithhippos.nullseek.tv.ui.components.readTextFromUri
import com.github.livingwithhippos.nullseek.tv.ui.components.safeOpenUrl
import com.github.livingwithhippos.nullseek.ui.user.UserViewModel
import com.github.livingwithhippos.nullseek.utils.getClipboardText
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun TvProviderSetupScreen(
    uiState: NullRootUiState,
    checkValidToken: (NetProvider, String) -> Unit,
    onBackupRestored: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UserViewModel = koinViewModel(),
) {
    val provider = remember { NetProvider.Torbox() }
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    val scope = rememberCoroutineScope()
    val referralPreference by viewModel.includeReferralPreference.collectAsStateWithLifecycle()
    val isRestoring by viewModel.isRestoringBackup.collectAsStateWithLifecycle()
    var token by rememberSaveable { mutableStateOf("") }
    var showReferralDialog by remember { mutableStateOf(false) }
    // nothing on the setup screen can be used while a backup is being restored
    val inputsEnabled = !uiState.loading && !isRestoring

    val restoredText = stringResource(R.string.backup_restored_success)
    val restoreFailedText = stringResource(R.string.backup_restore_failed)
    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val json = context.readTextFromUri(uri)
                if (json == null) {
                    messenger.show(restoreFailedText)
                } else {
                    viewModel.restoreBackup(json) { success ->
                        if (success) onBackupRestored()
                        messenger.show(if (success) restoredText else restoreFailedText)
                    }
                }
            }
        }

    val connect = {
        val clean = token.trim()
        if (clean.isNotBlank() && !isRestoring) checkValidToken(provider, clean)
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(
            title = stringResource(R.string.setup_title),
            subtitle = stringResource(R.string.setup_provider_description),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                SectionCard(title = stringResource(R.string.setup_key_title), icon = R.drawable.ic_settings) {
                    Text(stringResource(R.string.setup_key_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TvTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = stringResource(R.string.api_key),
                        placeholder = stringResource(R.string.paste_api_key_placeholder),
                        enabled = inputsEnabled,
                        isPassword = true,
                        imeAction = ImeAction.Done,
                        onImeAction = connect,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvActionButton(
                            text = stringResource(R.string.setup_connect_button),
                            primary = true,
                            loading = uiState.loading,
                            enabled = token.isNotBlank() && !isRestoring,
                            onClick = connect,
                        )
                        TvActionButton(
                            text = stringResource(R.string.paste_from_clipboard),
                            icon = R.drawable.ic_paste,
                            enabled = inputsEnabled,
                            onClick = { getClipboardText(context)?.let { token = it.trim() } },
                        )
                    }
                    TvActionButton(
                        text = stringResource(R.string.setup_get_key_button),
                        icon = R.drawable.ic_open_in_new,
                        enabled = !isRestoring,
                        onClick = { safeOpenUrl(context, provider.tokenLink) },
                    )
                    Text(provider.tokenLink, style = MaterialTheme.typography.bodySmall)
                }
                uiState.error?.let { ErrorBanner(it) }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                SectionCard(title = stringResource(R.string.setup_signup_title), icon = R.drawable.ic_contact_page) {
                    Text(stringResource(R.string.setup_signup_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TvActionButton(
                        text = stringResource(R.string.setup_signup_button),
                        icon = R.drawable.ic_open_in_new,
                        enabled = !isRestoring,
                        onClick = {
                            when (referralPreference) {
                                null -> showReferralDialog = true
                                true -> safeOpenUrl(context, provider.referralLink)
                                false -> safeOpenUrl(context, provider.home)
                            }
                        },
                    )
                    Text(provider.home, style = MaterialTheme.typography.bodySmall)
                }
                SectionCard(title = stringResource(R.string.setup_restore_title), icon = R.drawable.ic_history) {
                    Text(stringResource(R.string.setup_restore_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TvActionButton(
                        text = stringResource(R.string.backup_restore_button),
                        icon = R.drawable.ic_history,
                        loading = isRestoring,
                        enabled = !uiState.loading,
                        onClick = { restoreLauncher.launchSafely(context, arrayOf("application/json", "*/*")) },
                    )
                }
            }
        }
    }

    if (showReferralDialog) {
        TvDialog(
            title = stringResource(R.string.referral_dialog_title),
            onDismiss = { showReferralDialog = false },
            confirmText = stringResource(R.string.referral_dialog_positive),
            onConfirm = {
                showReferralDialog = false
                viewModel.setReferralPreference(true)
                safeOpenUrl(context, provider.referralLink)
            },
            dismissText = stringResource(R.string.referral_dialog_negative),
            onDismissClick = {
                showReferralDialog = false
                viewModel.setReferralPreference(false)
                safeOpenUrl(context, provider.home)
            },
        ) {
            androidx.compose.material3.Text(stringResource(R.string.setup_dialog_message))
        }
    }
}
