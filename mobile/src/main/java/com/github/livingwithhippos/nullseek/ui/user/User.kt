package com.github.livingwithhippos.nullseek.ui.user

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    // todo: add support for loading user externally and pass it here
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

private fun getPlanName(plan: Int): String {
    return when (plan) {
        0 -> "Free"
        1 -> "Essential"
        2 -> "Pro"
        3 -> "Standard"
        else -> "Unknown"
    }
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
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LoadingIndicator()
        }
        return
    }

    if (user == null) {
        // todo: show various loading state and buttons
        Text(text = "No user data available")
        return
    }

    // todo: reload button with error message if found

    Column(
        modifier = modifier.padding(horizontal = 8.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Plan: ${getPlanName(user.plan)}",
            style = MaterialTheme.typography.titleLargeEmphasized
        )

        if (user.premiumExpiresAt != null) {
            Text(text = user.premiumExpiresAt.toString())
        } else {
            Text(text = "No Premium")
        }
        Text(text = user.email, maxLines = 1, overflow = TextOverflow.Clip)
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
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
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
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