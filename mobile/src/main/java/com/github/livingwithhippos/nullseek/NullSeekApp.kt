package com.github.livingwithhippos.nullseek

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.github.livingwithhippos.nullseek.ui.provider.ProviderEditor
import com.github.livingwithhippos.nullseek.ui.user.UserScreen
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NullSeekApp(
    appState: NullSeekState = rememberNullSeekState(),
    viewmodel: NullViewModel = koinViewModel<NullViewModel>()
) {

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(Unit) {
        viewmodel.events.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = event.actionLabel,
                    withDismissAction = event.withDismissAction,
                    duration = event.duration
                )
            }
        }
    }


    NavHost(
        navController = appState.navController,
        startDestination = Screen.Setup.route
    ) {
        composable(Screen.Setup.route) { backStackEntry ->
            ProviderEditor(
                onMissingToken = viewmodel::onMissingToken,
                navigateToUser = {
                    appState.navigateToUser(backStackEntry)
                },
            )
        }
        composable(Screen.User.route) { backStackEntry ->
            UserScreen()
        }

    }
}