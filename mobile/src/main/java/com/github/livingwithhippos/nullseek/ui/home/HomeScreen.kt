package com.github.livingwithhippos.nullseek.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.livingwithhippos.nullseek.NullRootUiState
import com.github.livingwithhippos.nullseek.core.data.NetProvider
import com.github.livingwithhippos.nullseek.ui.provider.ProviderSetup
import com.github.livingwithhippos.nullseek.ui.user.UserScreen
import com.github.livingwithhippos.nullseek.ui.user.UserViewModel
import com.github.livingwithhippos.nullseek.ui.utils.ExtendableFabMenu
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    contentType: NullContentType,
    uiState: NullRootUiState,
    navigationType: NullNavigationType,
    modifier: Modifier = Modifier,
    showSettings: () -> Unit,
    onNewDownload: () -> Unit,
    checkValidToken: (provider: NetProvider, token: String) -> Unit,
    onBackupRestored: () -> Unit = {},
    userViewModel: UserViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val isRestoring by userViewModel.isRestoringBackup.collectAsState()
    val isLoggedIn = !uiState.setup && uiState.user != null

    // we have no double panel for the user info, unless we decide to put the settings one on the side
    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.setup) {
            ProviderSetup(
                modifier = modifier,
                uiState = uiState,
                checkValidToken = checkValidToken,
                onBackupRestored = onBackupRestored,
                viewModel = userViewModel,
            )
        } else {
            UserScreen(
                modifier = modifier,
                user = uiState.user,
                loading = uiState.loading,
                error = uiState.error,
                context = context,
                viewModel = userViewModel,
                enabled = !isRestoring,
            )
        }

        // the FABs have no disabled state, so hide them while a backup is being restored
        if (navigationType == NullNavigationType.BOTTOM_NAVIGATION && !isRestoring) {
            ExtendableFabMenu(
                onNewDownload = onNewDownload,
                showSettings = showSettings,
                showNewDownload = isLoggedIn,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}
