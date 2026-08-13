package com.github.livingwithhippos.nullseek

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.livingwithhippos.nullseek.core.data.NetProvider
import com.github.livingwithhippos.nullseek.ui.home.HomeScreen
import com.github.livingwithhippos.nullseek.ui.lists.ListsScreen
import com.github.livingwithhippos.nullseek.ui.navigation.NavigationActions
import com.github.livingwithhippos.nullseek.ui.navigation.NullNavigationWrapper
import com.github.livingwithhippos.nullseek.ui.navigation.Route
import com.github.livingwithhippos.nullseek.ui.navigation.SETTINGS_DESTINATION
import com.github.livingwithhippos.nullseek.ui.search.SearchScreen
import com.github.livingwithhippos.nullseek.ui.settings.SettingsScreen
import com.github.livingwithhippos.nullseek.ui.utils.NullContentType
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationType
import com.github.livingwithhippos.torbox.model.TorrentDto
import org.koin.compose.viewmodel.koinViewModel

private fun NavigationSuiteType.toNullNavType() = when (this) {
    NavigationSuiteType.NavigationBar -> NullNavigationType.BOTTOM_NAVIGATION
    NavigationSuiteType.NavigationRail -> NullNavigationType.NAVIGATION_RAIL
    NavigationSuiteType.NavigationDrawer -> NullNavigationType.PERMANENT_NAVIGATION_DRAWER
    else -> NullNavigationType.BOTTOM_NAVIGATION
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NullSeekApp(
    windowSize: WindowSizeClass,
    rootUiState: NullRootUiState,
    checkValidToken: (provider: NetProvider, token: String) -> Unit,
    loadTorrentDetails: (torrent: TorrentDto) -> Unit,
    closeDetails: () -> Unit,
    viewmodel: NullViewModel = koinViewModel<NullViewModel>()
) {
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

    val contentType = when (windowSize.widthSizeClass) {
        WindowWidthSizeClass.Compact -> NullContentType.SINGLE_PANE

        WindowWidthSizeClass.Medium -> NullContentType.SINGLE_PANE

        WindowWidthSizeClass.Expanded -> NullContentType.DUAL_PANE

        else -> NullContentType.SINGLE_PANE
    }

    val navController = rememberNavController()
    val navigationActions = remember(navController) {
        NavigationActions(navController)
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Surface {
        NullNavigationWrapper(
            currentDestination = currentDestination,
            navigateToTopLevelDestination = { destination ->
                navigationActions.navigateTo(destination)
            },
        ) {
            NullNavHost(
                navController = navController,
                contentType = contentType,
                uiState = rootUiState,
                navigationType = navSuiteType.toNullNavType(),
                checkValidToken = checkValidToken,
                loadTorrentDetails = loadTorrentDetails,
                showSettings = {
                    navigationActions.navigateTo(SETTINGS_DESTINATION)
                },
                closeDetails = closeDetails
            )
        }
    }
}

@Composable
private fun NullNavHost(
    navController: NavHostController,
    contentType: NullContentType,
    uiState: NullRootUiState,
    navigationType: NullNavigationType,
    checkValidToken: (provider: NetProvider, token: String) -> Unit,
    loadTorrentDetails: (torrent: TorrentDto) -> Unit,
    showSettings: () -> Unit,
    closeDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Route.Home,
    ) {
        composable<Route.Home> {
            HomeScreen(
                contentType = contentType,
                navigationType = navigationType,
                uiState = uiState,
                checkValidToken = checkValidToken,
                showSettings = showSettings
            )
        }
        composable<Route.Lists> {
            ListsScreen(
                contentType = contentType,
                navigationType = navigationType,
                uiState = uiState,
                loadTorrentDetails = loadTorrentDetails,
                closeDetails = closeDetails
            )
        }
        composable<Route.Search> {
            SearchScreen(
                contentType = contentType, navigationType = navigationType, uiState = uiState
            )
        }
        composable<Route.Settings> {
            SettingsScreen(
                contentType = contentType,
                navigationType = navigationType
            )
        }
    }
}