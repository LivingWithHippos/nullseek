package com.github.livingwithhippos.nullseek.ui.provider

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.NullRootUiState
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.data.NetProvider
import com.github.livingwithhippos.nullseek.ui.theme.FilledTonalButton
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.theme.OutlinedButton
import com.github.livingwithhippos.nullseek.ui.theme.OutlinedIconButton
import com.github.livingwithhippos.nullseek.ui.theme.TextButton
import com.github.livingwithhippos.nullseek.ui.user.UserViewModel
import com.github.livingwithhippos.nullseek.utils.getClipboardText
import com.github.livingwithhippos.nullseek.utils.openWebPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProviderSetup(
    modifier: Modifier = Modifier,
    uiState: NullRootUiState,
    checkValidToken: (provider: NetProvider, token: String) -> Unit,
    onBackupRestored: () -> Unit = {},
    viewModel: UserViewModel = koinViewModel(),
) {
    val currentProvider = NetProvider.Torbox()
    val context = LocalContext.current

    ProviderSetup(
        modifier = modifier,
        provider = currentProvider,
        loading = uiState.loading,
        error = uiState.error,
        checkValidToken = checkValidToken,
        onBackupRestored = onBackupRestored,
        viewModel = viewModel,
        context = context,
    )
}

@Composable
fun ProviderSetup(
    modifier: Modifier = Modifier,
    provider: NetProvider,
    loading: Boolean,
    error: String?,
    checkValidToken: (provider: NetProvider, token: String) -> Unit,
    onBackupRestored: () -> Unit = {},
    viewModel: UserViewModel = koinViewModel(),
    context: Context = LocalContext.current,
) {
    val referralPreference by viewModel.includeReferralPreference.collectAsState()
    val isRestoringBackup by viewModel.isRestoringBackup.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var showReferralDialog by remember { mutableStateOf(false) }

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
                        viewModel.restoreBackup(rawJson) { success ->
                            val messageRes =
                                if (success) {
                                    onBackupRestored()
                                    R.string.backup_restored_success
                                } else {
                                    R.string.backup_restore_failed
                                }
                            Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, R.string.backup_restore_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    if (showReferralDialog) {
        AlertDialog(
            onDismissRequest = { showReferralDialog = false },
            title = {
                Text(text = stringResource(id = R.string.referral_dialog_title))
            },
            text = {
                Text(text = stringResource(id = R.string.setup_dialog_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReferralDialog = false
                        viewModel.setReferralPreference(true)
                        openWebPage(context, provider.referralLink)
                    },
                ) {
                    Text(text = stringResource(id = R.string.referral_dialog_positive))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showReferralDialog = false
                        viewModel.setReferralPreference(false)
                        openWebPage(context, provider.home)
                    },
                ) {
                    Text(text = stringResource(id = R.string.referral_dialog_negative))
                }
            },
        )
    }

    ProviderSetupContent(
        modifier = modifier,
        provider = provider,
        loading = loading,
        error = error,
        checkValidToken = checkValidToken,
        onSignUpClick = {
            if (referralPreference == null) {
                showReferralDialog = true
            } else {
                val targetUrl =
                    if (referralPreference == true) {
                        provider.referralLink
                    } else {
                        provider.home
                    }
                openWebPage(context, targetUrl)
            }
        },
        onGetTokenClick = {
            openWebPage(context, provider.tokenLink)
        },
        isRestoringBackup = isRestoringBackup,
        onRestoreBackupClick = {
            restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
        },
        context = context,
    )
}

@Composable
fun ProviderSetupContent(
    modifier: Modifier = Modifier,
    provider: NetProvider,
    loading: Boolean,
    error: String?,
    checkValidToken: (provider: NetProvider, token: String) -> Unit,
    onSignUpClick: () -> Unit,
    onGetTokenClick: () -> Unit,
    isRestoringBackup: Boolean = false,
    onRestoreBackupClick: () -> Unit = {},
    context: Context = LocalContext.current,
) {
    var token by rememberSaveable { mutableStateOf("") }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Card 1: Onboarding & Sign-Up Guidance Card
            ProviderOnboardingCard(
                provider = provider,
                onSignUpClick = onSignUpClick,
                enabled = !isRestoringBackup,
            )

            // Card 2: API Token Configuration Card
            ProviderTokenConfigCard(
                token = token,
                onTokenChange = { token = it },
                loading = loading,
                enabled = !loading && !isRestoringBackup,
                onGetTokenClick = onGetTokenClick,
                onConnectClick = {
                    val currentToken = token.trim()
                    if (currentToken.isNotBlank()) {
                        checkValidToken(provider, currentToken)
                    }
                },
                context = context,
            )

            // Card 3: Restore Backup Card
            ProviderRestoreBackupCard(
                isRestoringBackup = isRestoringBackup || loading,
                onRestoreBackupClick = onRestoreBackupClick,
            )

            // Card 4: Error Banner (if error occurred)
            if (error != null) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        ),
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ProviderOnboardingCard(
    provider: NetProvider,
    onSignUpClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(id = R.string.setup_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = stringResource(id = R.string.setup_provider_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = stringResource(id = R.string.setup_signup_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = stringResource(id = R.string.setup_signup_description),
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedButton(
                onClick = onSignUpClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_contact_page),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.setup_signup_button))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProviderTokenConfigCard(
    token: String,
    onTokenChange: (String) -> Unit,
    loading: Boolean,
    onGetTokenClick: () -> Unit,
    onConnectClick: () -> Unit,
    context: Context,
    modifier: Modifier = Modifier,
    enabled: Boolean = !loading,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(id = R.string.setup_key_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = stringResource(id = R.string.setup_key_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(
                onClick = onGetTokenClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(id = R.string.setup_get_key_button))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            OutlinedTextField(
                value = token,
                onValueChange = onTokenChange,
                label = { Text(text = stringResource(id = R.string.api_key)) },
                placeholder = { Text(text = stringResource(id = R.string.paste_api_key_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = enabled,
                trailingIcon = {
                    OutlinedIconButton(
                        onClick = {
                            getClipboardText(context)?.let { clipText ->
                                onTokenChange(clipText.trim())
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        enabled = enabled,
                        border = ButtonDefaults.outlinedButtonBorder(enabled),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_paste),
                            contentDescription = stringResource(id = R.string.paste_from_clipboard),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            FilledTonalButton(
                onClick = onConnectClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled && token.trim().length > 10,
            ) {
                if (loading) {
                    LoadingIndicator(
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = stringResource(id = R.string.setup_connect_button))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProviderRestoreBackupCard(
    isRestoringBackup: Boolean,
    onRestoreBackupClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(id = R.string.setup_restore_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = stringResource(id = R.string.setup_restore_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(
                onClick = onRestoreBackupClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRestoringBackup,
            ) {
                if (isRestoringBackup) {
                    LoadingIndicator(
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = stringResource(id = R.string.backup_restore_button))
            }
        }
    }
}

@Preview
@Composable
fun ProviderEditorPreview() {
    NullSeekTheme {
        ProviderSetupContent(
            modifier = Modifier.padding(horizontal = 8.dp),
            provider = NetProvider.Torbox(),
            loading = false,
            error = null,
            checkValidToken = { _, _ -> },
            onSignUpClick = {},
            onGetTokenClick = {},
            context = LocalContext.current,
        )
    }
}