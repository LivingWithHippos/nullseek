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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.core.utils.mockUser
import com.github.livingwithhippos.nullseek.ui.theme.CustomTypography
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.nullseek.ui.utils.CardStat
import com.github.livingwithhippos.nullseek.ui.utils.CopyButton
import com.github.livingwithhippos.nullseek.utils.convertToDays
import com.github.livingwithhippos.nullseek.utils.convertToSize
import com.github.livingwithhippos.nullseek.utils.getPlanName
import com.github.livingwithhippos.nullseek.utils.setClipboardText
import com.github.livingwithhippos.torbox.model.UserDataDto

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UserScreen(
    modifier: Modifier = Modifier,
    user: UserDataDto?,
    loading: Boolean,
    error: String?,
    context: Context = LocalContext.current,
) {
    if (loading || user == null) {
        // user == null should not happen
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LoadingIndicator()
        }
        return
    }

    // todo: reload button

    Column(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth(),
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
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            CopyButton(
                text = user.email,
                description = "Copy Mail",
                context = context
            )
            Text(text = user.email, overflow = TextOverflow.Clip)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            CopyButton(
                text = user.authId,
                description = "Copy Auth ID",
                context = context
            )
            Text(text = user.authId, overflow = TextOverflow.Clip)
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
                modifier = Modifier
                    .padding(top = 12.dp)
                    .widthIn(min = 250.dp, max = 400.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardStat(
                    titles = listOf("Files", "Downloaded"),
                    description = user.totalDownloaded.toString()
                )
                Spacer(modifier = Modifier.width(16.dp))
                CardStat(
                    titles = listOf("Torrents", "Downloaded"),
                    description = user.torrentsDownloaded.toString()
                )
            }
            Row(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .widthIn(min = 250.dp, max = 400.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {

                CardStat(
                    titles = listOf("Size", "Downloaded"),
                    description = convertToSize(user.totalBytesDownloaded)
                )
                Spacer(modifier = Modifier.width(16.dp))

                CardStat(
                    titles = listOf("Size", "Uploaded"),
                    description = convertToSize(user.totalBytesUploaded)
                )
            }
        }

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
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
                loading = false,
                error = null
            )
        }
    }
}