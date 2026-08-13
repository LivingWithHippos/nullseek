package com.github.livingwithhippos.nullseek.ui.provider

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.NullRootUiState
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.core.data.NetProvider
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.utils.getClipboardText
import com.github.livingwithhippos.nullseek.utils.openWebPage

@Composable
fun ProviderSetup(
    modifier: Modifier = Modifier,
    uiState: NullRootUiState,
    checkValidToken: (provider: NetProvider, token: String) -> Unit,
) {
    val currentProvider = NetProvider.Torbox()
    val context = LocalContext.current

    ProviderSetup(
        modifier = modifier,
        provider = currentProvider,
        loading = uiState.loading,
        error = uiState.error,
        checkValidToken = checkValidToken,
        context = context
    )

}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProviderSetup(
    modifier: Modifier = Modifier,
    provider: NetProvider,
    loading: Boolean,
    error: String?,
    checkValidToken: (provider: NetProvider, token: String) -> Unit,
    context: Context = LocalContext.current,
) {
    val tokenState = rememberTextFieldState("")

    Surface {
        Column(modifier = modifier.padding(horizontal = 8.dp)) {
            Text(text = provider.name, style = MaterialTheme.typography.displayMedium)
            TextButton(onClick = {
                // referral dialog here
                openWebPage(context, provider.home)
            }, shape = ButtonDefaults.textShape) { Text(provider.home) }

            OutlinedTextField(
                state = tokenState,
                label = { Text("Token") },
                modifier = Modifier.width(300.dp),
                lineLimits = TextFieldLineLimits.SingleLine,
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
                            painter = painterResource(R.drawable.ic_paste),
                            contentDescription = "Paste"
                        )
                    }
                },
                enabled = loading.not(),
            )
            // todo: add text explaining how to sign up and how to retrieve the API
            OutlinedButton(
                onClick = {
                    openWebPage(context, provider.tokenLink)
                },
                enabled = loading.not(),
            ) { Text("Open token page") }
            ElevatedButton(
                onClick = {
                    val currentToken = tokenState.text.toString()
                    if (currentToken.isBlank()) {
                        // todo: snackbar
                        return@ElevatedButton
                    }

                    checkValidToken(provider, tokenState.text.toString())
                },
                enabled = loading.not() && tokenState.text.length > 10
            ) { Text("Save") }

            if (loading) {
                // todo: see if a full screen one is better. Transparent overlay?
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) { LoadingIndicator() }
            } else if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview
@Composable
fun ProviderEditorPreview() {
    NullSeekTheme {
        ProviderSetup(
            modifier = Modifier.padding(horizontal = 8.dp),
            provider = NetProvider.Torbox(),
            loading = false,
            error = "Showing error message",
            checkValidToken = { _, _ -> },
            context = LocalContext.current,
        )
    }
}