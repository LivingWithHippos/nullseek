package com.github.livingwithhippos.nullseek.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.livingwithhippos.nullseek.NullRootUiState
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
    checkValidToken: (provider: NetProvider, token: String) -> Unit
) {
    val context = LocalContext.current

    // we have no double panel for the user info, unless we decide to put the settings one on the side
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
}