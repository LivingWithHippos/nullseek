package com.github.livingwithhippos.nullseek.ui.provider

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.data.NetProvider
import com.github.livingwithhippos.nullseek.utils.getClipboardText
import com.github.livingwithhippos.nullseek.utils.openWebPage
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import com.github.livingwithhippos.nullseek.core.viewmodel.provider.ProviderViewModel
import com.github.livingwithhippos.torbox.model.UserDataDto
import org.koin.compose.viewmodel.koinViewModel

@ExperimentalMaterial3ExpressiveApi
@Composable
fun ProviderEditor(
    modifier: Modifier = Modifier,
    currentToken: String = "",
    viewModel: ProviderViewModel= koinViewModel<ProviderViewModel>(),
    onSave: (
        provider: String,
        token: String,
    ) -> Unit,
    onSnack: (
        text: String,
        duration: SnackbarDuration,
        label: String?
    ) -> Unit,
) {
    val providerScreenUiState by viewModel.state.collectAsStateWithLifecycle()
    val uiState = providerScreenUiState

    val context = LocalContext.current

    val currentProvider = NetProvider.Torbox()
    val tokenState = rememberTextFieldState(currentToken)

    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        // fixme: check how to get the NullSeekTheme typography
        Text(text = currentProvider.name, style = MaterialTheme.typography.displayMedium)
        TextButton(onClick = {
            // referral dialog here
            openWebPage(context, currentProvider.home)
        }, shape = ButtonDefaults.textShape) { Text(currentProvider.home) }
        if (uiState.validToken == null || uiState.user == null) {
            // todo: fixed width
            OutlinedTextField(
                state = tokenState,
                lineLimits = TextFieldLineLimits.SingleLine,
                label = { Text("Token") },
                isError = tokenState.text.isBlank(),
                trailingIcon = {
                    IconButton(onClick = {
                        getClipboardText(context)?.let { text ->
                            tokenState.setTextAndPlaceCursorAtEnd(
                                text
                            )
                        }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.icon_paste),
                            contentDescription = "Paste"
                        )
                    }
                },
                enabled = uiState.isLoading.not(),
            )
            // todo: add text explaining how to sign up and how to retrieve the API
            OutlinedButton(
                onClick = {
                    openWebPage(context, currentProvider.tokenLink)
                },
                enabled = uiState.isLoading.not(),
            ) { Text("Open token page") }
            ElevatedButton(onClick = {
                val currentToken = tokenState.text.toString()
                if (currentToken.isBlank()) {
                    onSnack("API token is required", SnackbarDuration.Short, null)
                    return@ElevatedButton
                }

                viewModel.checkTorboxToken(currentToken)
            }, enabled = uiState.isLoading.not()) { Text("Save") }

            if (uiState.isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { LoadingIndicator() }
            }

            uiState.errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        } else {
            uiState.user?.let { user ->
                QuickUserInfo(
                    modifier,
                    user,
                    uiState.validToken ?: "",
                    currentProvider,
                    onSave = { p, t -> onSave(p, t) }
                )
            }

        }
    }

}

@Composable
fun QuickUserInfo(
    modifier: Modifier = Modifier,
    user: UserDataDto,
    token: String,
    provider: NetProvider,
    onSave: (provider: String, token: String) -> Unit
) {
    Column {
        Text(text = user.email)
        Text(text = user.baseEmail)
        Text(text = user.premiumExpiresAt ?: "No Premium")
        Button(onClick = { onSave(provider.name, token) }) {
            Text("Confirm")
        }

    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun ProviderEditorPreview() {
    ProviderEditor(onSave = { _, _ -> }, onSnack = { _, _, _ -> })
}