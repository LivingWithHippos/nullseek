package com.github.livingwithhippos.nullseek.ui.user

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.data.NetProvider
import com.github.livingwithhippos.nullseek.ui.theme.NullSeekTheme
import com.github.livingwithhippos.torbox.model.UserDataDto

@Composable
fun TorboxUserPage(
    modifier: Modifier = Modifier,
    user: UserDataDto,
    token: String?,
    onSave: (provider: String, token: String) -> Unit,
) {

    Column(modifier = modifier.padding(horizontal = 8.dp)) {
        Text(text = user.email)
        // plan
        // is subscribed
        // expiration
        Text(text = user.premiumExpiresAt ?: "No Premium")
        if (token != null) {
            // if we have a token we are setting up the service, otherwise this is the user page
            Button(onClick = { onSave(NetProvider.Torbox().name, token) }) {
                Text("Confirm")
            }
        }

    }
}

@Preview
@Composable
fun UserPagePreview() {
    NullSeekTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            TorboxUserPage(
                modifier = Modifier.padding(innerPadding),
                user = UserDataDto(
                    id = 1,
                    authId = "123",
                    createdAt = "2026-05-22T10:53:44Z",
                    updatedAt = "2026-06-11T22:18:32Z",
                    plan = 2,
                    totalDownloaded = 5,
                    customer = "123",
                    isSubscribed = true,
                    premiumExpiresAt = "2026-08-25T12:51:51Z",
                    cooldownUntil = "2026-06-12T22:18:31Z",
                    email = "cool@mail.com",
                    userReferral = "123456",
                    baseEmail = "cool@mail.com",
                    totalBytesDownloaded = 2201170739L,
                    totalBytesUploaded = 0,
                    torrentsDownloaded = 45,
                    webDownloadsDownloaded = 4,
                    usenetDownloadsDownloaded = 0,
                    additionalConcurrentSlots = 0,
                    longTermSeeding = false,
                    longTermStorage = false,
                    isVendor = false,
                    vendorId = null,
                    purchasesReferred = 0
                ),
                token = null,
                onSave = { _, _ -> }
            )
        }
    }
}