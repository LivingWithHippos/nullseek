package com.github.livingwithhippos.nullseek.ui.provider

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.core.viewmodel.provider.ProviderViewModel
import com.github.livingwithhippos.nullseek.core.data.NetProvider
import com.github.livingwithhippos.nullseek.core.viewmodel.provider.ProviderAction
import com.github.livingwithhippos.nullseek.core.viewmodel.provider.ProviderEvent
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.utils.getClipboardText
import com.github.livingwithhippos.nullseek.utils.openWebPage
import com.github.livingwithhippos.torbox.model.UserDataDto
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProviderEditor(
    modifier: Modifier = Modifier,
    currentToken: String = "",
    onMissingToken: () -> Unit,
    navigateToUser: () -> Unit,
    viewModel: ProviderViewModel = koinViewModel<ProviderViewModel>(),
) {
    val currentProvider = NetProvider.Torbox()
    val context = LocalContext.current

    val providerScreenUiState by viewModel.state.collectAsStateWithLifecycle()
    val uiState = providerScreenUiState

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                ProviderEvent.TokenConfirmed -> navigateToUser()
            }
        }
    }


    ProviderEditor(
        modifier = modifier,
        provider = currentProvider,
        token = currentToken,
        validToken = uiState.validToken,
        isLoading = uiState.isLoading,
        user = uiState.user,
        error = uiState.errorMessage,
        onMissingToken = onMissingToken,
        onProviderAction = viewModel::onProviderAction,
        context = context,
        navigateToUser = navigateToUser
    )

}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProviderEditor(
    modifier: Modifier = Modifier,
    provider: NetProvider,
    token: String,
    validToken: String?,
    isLoading: Boolean,
    user: UserDataDto?,
    error: String?,
    onMissingToken: () -> Unit,
    onProviderAction: (ProviderAction) -> Unit,
    navigateToUser: () -> Unit,
    context: Context = LocalContext.current,
) {
    val tokenState = rememberTextFieldState(token)

    Surface {
        Column(modifier = modifier.padding(horizontal = 8.dp)) {
            // fixme: check how to get the NullSeekTheme typography
            Text(text = provider.name, style = MaterialTheme.typography.displayMedium)
            TextButton(onClick = {
                // referral dialog here
                openWebPage(context, provider.home)
            }, shape = ButtonDefaults.textShape) { Text(provider.home) }

            if (validToken == null || user == null) {
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
                    enabled = isLoading.not(),
                )
                // todo: add text explaining how to sign up and how to retrieve the API
                OutlinedButton(
                    onClick = {
                        openWebPage(context, provider.tokenLink)
                    },
                    enabled = isLoading.not(),
                ) { Text("Open token page") }
                ElevatedButton(onClick = {
                    val currentToken = tokenState.text.toString()
                    if (currentToken.isBlank()) {
                        onMissingToken()
                        return@ElevatedButton
                    }

                    onProviderAction(ProviderAction.CheckToken(currentToken))

                }, enabled = isLoading.not() && tokenState.text.length > 10) { Text("Save") }

                if (isLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { LoadingIndicator() }
                }

                if (error != null) {
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            } else {
                QuickUserInfo(
                    user,
                    validToken,
                    onSave = { t -> onProviderAction(ProviderAction.UserConfirmed(provider, t)) }
                )

                // todo: reset option unless we decide to just save and go on to the user screen
            }
        }
    }
}

@Composable
fun QuickUserInfo(
    user: UserDataDto,
    token: String,
    onSave: (token: String) -> Unit
) {
    Column {
        Text(text = user.email)
        Text(text = user.baseEmail)
        Text(text = user.premiumExpiresAt ?: "No Premium")
        Button(onClick = { onSave(token) }) {
            Text("Confirm")
        }

    }
}

@Preview
@Composable
fun ProviderEditorPreview() {
    NullSeekTheme {
        ProviderEditor(
            modifier = Modifier.padding(horizontal = 8.dp),
            provider = NetProvider.Torbox(),
            token = "",
            validToken = null,
            isLoading = false,
            user = null,
            error = null,
            onMissingToken = {},
            onProviderAction = {},
            context = LocalContext.current,
            navigateToUser = { }
        )
    }
}