package com.github.livingwithhippos.nullseek.ui.user

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.core.utils.MathConst
import com.github.livingwithhippos.nullseek.core.utils.mockUser
import com.github.livingwithhippos.nullseek.ui.theme.CustomTypography
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.utils.setClipboardText
import com.github.livingwithhippos.torbox.model.UserDataDto
import logcat.asLog
import logcat.logcat
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.Instant

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

fun convertToDays(datetime: String?): String {
    // todo: move to utils
    // todo: manage trnalsation and no premium avilable
    if (datetime == null) {
        return "No Premium"
    }
    try {
        val dateInstant = Instant.parse(datetime)
        val now = Clock.System.now()
        val days = (dateInstant - now).inWholeDays
        if (days < 0) {
            return "Expired"
        }
        return "$days days"
    } catch (e: Exception) {
        logcat("UserScreen") { e.asLog() }
    }
    return datetime
}

fun convertToSize(bytes: Long): String {
    return when {
        bytes < MathConst.BYTES_IN_KB -> "$bytes B"
        bytes < MathConst.BYTES_IN_MB -> "${(bytes / MathConst.BYTES_IN_KB).toInt()} KB"
        bytes < MathConst.BYTES_IN_GB -> "${(bytes / MathConst.BYTES_IN_MB).toInt()} MB"
        else -> "${(bytes / MathConst.BYTES_IN_GB).toInt()} GB"
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
            text = "User Info",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Plan: ${getPlanName(user.plan)}",
            style = MaterialTheme.typography.titleLargeEmphasized,
        )
        Text(text = convertToDays(user.premiumExpiresAt))
        Text(text = user.email, maxLines = 1, overflow = TextOverflow.Clip)
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                setClipboardText(context, user.email)
            }) {
                Icon(
                    painter = painterResource(R.drawable.icon_paste),
                    contentDescription = "Copy Mail"
                )
            }
            Text(text = user.email)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                setClipboardText(context, user.authId)
            }) {
                Icon(
                    painter = painterResource(R.drawable.icon_paste),
                    contentDescription = "Copy Auth ID"
                )
            }
            Text(text = user.authId)
        }

        if (user.isSubscribed) {
            Text(text = "Subscribed")
        } else {
            Text(text = "No active subscription")
        }

        if (user.isVendor) {
            Text(text = "Vendor Enabled")
        }

        ElevatedCard(
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            )
        ) {
            Row(
                modifier = Modifier.padding(top = 12.dp).widthIn(min = 250.dp, max = 400.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Files", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Downloaded", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = user.totalDownloaded.toString(),
                        style= CustomTypography.cardBoldValue,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Torrents", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Downloaded", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = user.torrentsDownloaded.toString(),
                        style= CustomTypography.cardBoldValue,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.padding(vertical = 12.dp).widthIn(min = 250.dp, max = 400.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Size", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Downloaded", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = convertToSize(user.totalBytesDownloaded),
                        style= CustomTypography.cardBoldValue,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Size", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Uploaded", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = convertToSize(user.totalBytesUploaded),
                        style= CustomTypography.cardBoldValue,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }


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