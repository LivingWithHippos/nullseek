package com.github.livingwithhippos.nullseek.ui.user

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.core.utils.mockUser
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.utils.setClipboardText
import com.github.livingwithhippos.torbox.model.UserDataDto
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun UserScreen(
    modifier: Modifier = Modifier,
    viewModel: UserViewModel = koinViewModel<UserViewModel>(),
) {
    val userScreenUiState by viewModel.state.collectAsStateWithLifecycle()
    val uiState = userScreenUiState

    val context = LocalContext.current

    // todo: check if back navigation needs to be managed
    /*
    val scope = rememberCoroutineScope()

    BackHandler(enabled = navigator.canNavigateBack()) {
        scope.launch {
            navigator.navigateBack()
        }
    }
     */

    UserScreen(
        modifier = modifier,
        user = uiState.user,
        isLoading = uiState.isLoading,
        error = uiState.error,
        context = context
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UserScreen(
    modifier: Modifier = Modifier,
    user: UserDataDto?,
    isLoading: Boolean,
    error: String?,
    context: Context = LocalContext.current,
) {
    if (isLoading) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { LoadingIndicator() }
        return
    }

    if (user == null) {
        // todo: show various loading state and buttons
        Text(text = "No user data available")
        return
    }

    // todo: reload button with error message if found

    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        Text(text = user.email)
        Row {
            IconButton(onClick = {
                setClipboardText(context, user.userReferral)
            }) {
                Icon(
                    painter = painterResource(R.drawable.icon_paste),
                    contentDescription = "Copy"
                )
            }
            Text(text = user.userReferral)
        }
        Row {
            IconButton(onClick = {
                setClipboardText(context, user.authId)
            }) {
                Icon(
                    painter = painterResource(R.drawable.icon_paste),
                    contentDescription = "Copy"
                )
            }
            Text(text = user.authId)
        }

        if (user.premiumExpiresAt != null) {
            Text(text = user.premiumExpiresAt.toString())
        } else {
            Text(text = "No Premium")
        }
        when (user.plan) {
            0 -> Text(text = "Plan: Free")
            1 -> Text(text = "Plan: Essential")
            2 -> Text(text = "Plan: Pro")
            3 -> Text(text = "Plan: Standard")
            else -> Text(text = "Plan: Unknown")
        }
        if (user.isVendor) {
            Text(text = "Vendor Enabled")
        }

        Text(text = user.totalDownloaded.toString())
        Text(text = user.torrentsDownloaded.toString())

    }
}

@Preview
@Composable
fun UserScreenPreview() {
    NullSeekTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            UserScreen(
                modifier = Modifier.padding(innerPadding),
                user = mockUser,
                isLoading = false,
                error = null
            )
        }
    }
}