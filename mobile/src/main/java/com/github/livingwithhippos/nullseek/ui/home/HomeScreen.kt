package com.github.livingwithhippos.nullseek.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.NullRootUiState
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.core.data.NetProvider
import com.github.livingwithhippos.nullseek.ui.provider.ProviderSetup
import com.github.livingwithhippos.nullseek.ui.user.UserScreen
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType

@Composable
fun HomeScreen(
    contentType: NullContentType,
    uiState: NullRootUiState,
    navigationType: NullNavigationType,
    modifier: Modifier = Modifier,
    showSettings: () -> Unit,
    checkValidToken: (provider: NetProvider, token: String) -> Unit
) {
    val context = LocalContext.current

    // we have no double panel for the user info, unless we decide to put the settings one on the side
    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.setup) {
            ProviderSetup(
                modifier = modifier,
                uiState = uiState,
                checkValidToken = checkValidToken
            )
        } else {
            UserScreen(
                modifier = modifier,
                user = uiState.user,
                loading = uiState.loading,
                error = uiState.error,
                context = context
            )
        }

        // todo: move this and the showing of the settings to another place
        //  so it can be triggered by all screens

        if (navigationType == NullNavigationType.BOTTOM_NAVIGATION) {
            ExtendedFloatingActionButton(
                text = { Text(text = stringResource(id = R.string.settings)) },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = stringResource(id = R.string.settings)
                    )
                },
                onClick = { showSettings() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                // todo: manage expanded
            )
        }
    }
}