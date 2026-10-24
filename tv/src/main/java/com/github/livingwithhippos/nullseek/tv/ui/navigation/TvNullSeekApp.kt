package com.github.livingwithhippos.nullseek.tv.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.tv.material3.Icon
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.github.livingwithhippos.nullseek.NullRootUiState
import com.github.livingwithhippos.nullseek.core.R
import com.github.livingwithhippos.nullseek.core.data.NetProvider
import com.github.livingwithhippos.nullseek.tv.ui.components.ProvideMessenger
import com.github.livingwithhippos.nullseek.tv.ui.externalservices.TvExternalServicesScreen
import com.github.livingwithhippos.nullseek.tv.ui.home.TvHomeScreen
import com.github.livingwithhippos.nullseek.tv.ui.lists.TvListsScreen
import com.github.livingwithhippos.nullseek.tv.ui.newdownload.TvNewDownloadScreen
import com.github.livingwithhippos.nullseek.tv.ui.providers.TvProvidersScreen
import com.github.livingwithhippos.nullseek.tv.ui.search.TvSearchScreen
import com.github.livingwithhippos.nullseek.tv.ui.settings.TvSettingsScreen
import com.github.livingwithhippos.nullseek.ui.navigation.Route
import com.github.livingwithhippos.torbox.model.TorrentDto
import com.github.livingwithhippos.torbox.model.UsenetDownloadDto
import com.github.livingwithhippos.torbox.model.WebDownloadDto

private data class DrawerDestination(
    val route: Route,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    val needsLogin: Boolean = false,
    /** other routes that keep this item highlighted */
    val children: List<Route> = emptyList(),
)

private val DRAWER_DESTINATIONS =
    listOf(
        DrawerDestination(Route.Home, R.string.home, R.drawable.ic_contact_page),
        DrawerDestination(Route.Lists, R.string.lists, R.drawable.ic_list_bullet, needsLogin = true),
        DrawerDestination(Route.Search, R.string.search, R.drawable.ic_manage_search),
        DrawerDestination(Route.NewDownload(), R.string.new_download, R.drawable.ic_add, needsLogin = true),
        DrawerDestination(
            Route.Settings,
            R.string.settings,
            R.drawable.ic_settings,
            children = listOf(Route.ManageProviders, Route.ExternalServices),
        ),
    )

private fun NavDestination?.matches(route: Route): Boolean =
    this?.let {
        when (route) {
            Route.Home -> it.hasRoute<Route.Home>()
            Route.Lists -> it.hasRoute<Route.Lists>()
            Route.Search -> it.hasRoute<Route.Search>()
            Route.Settings -> it.hasRoute<Route.Settings>()
            Route.ExternalServices -> it.hasRoute<Route.ExternalServices>()
            Route.ManageProviders -> it.hasRoute<Route.ManageProviders>()
            is Route.NewDownload -> it.hasRoute<Route.NewDownload>()
        }
    } ?: false

private fun NavHostController.navigateTopLevel(route: Route) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun TvNullSeekApp(
    rootUiState: NullRootUiState,
    checkValidToken: (NetProvider, String) -> Unit,
    loadTorrentDetails: (TorrentDto) -> Unit,
    loadWebDetails: (WebDownloadDto) -> Unit,
    loadUsenetDetails: (UsenetDownloadDto) -> Unit,
    closeDetails: () -> Unit,
    reloadProviders: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isLoggedIn = !rootUiState.setup && rootUiState.user != null

    // when the last account is removed, go back to the setup screen
    LaunchedEffect(rootUiState.setup) {
        if (rootUiState.setup && !currentDestination.matches(Route.Home)) {
            navController.navigate(Route.Home) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    ProvideMessenger(snackbarHostState) {
        Surface(modifier = Modifier.fillMaxSize(), shape = RectangleShape) {
            Box(Modifier.fillMaxSize()) {
                NavigationDrawer(
                    drawerContent = {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxHeight()
                                    .padding(12.dp)
                                    .selectableGroup(),
                            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                        ) {
                            DRAWER_DESTINATIONS.forEach { destination ->
                                if (destination.needsLogin && !isLoggedIn) return@forEach
                                val selected =
                                    currentDestination.matches(destination.route) ||
                                        destination.children.any { currentDestination.matches(it) }
                                NavigationDrawerItem(
                                    selected = selected,
                                    onClick = { navController.navigateTopLevel(destination.route) },
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(destination.icon),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                ) {
                                    Text(stringResource(destination.label))
                                }
                            }
                        }
                    },
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Route.Home,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp),
                    ) {
                        composable<Route.Home> {
                            TvHomeScreen(
                                uiState = rootUiState,
                                checkValidToken = checkValidToken,
                                onBackupRestored = reloadProviders,
                                onOpenLists = { navController.navigateTopLevel(Route.Lists) },
                                onOpenSearch = { navController.navigateTopLevel(Route.Search) },
                                onNewDownload = { navController.navigateTopLevel(Route.NewDownload()) },
                                onOpenSettings = { navController.navigateTopLevel(Route.Settings) },
                            )
                        }
                        composable<Route.Lists> {
                            TvListsScreen(
                                uiState = rootUiState,
                                loadTorrentDetails = loadTorrentDetails,
                                loadWebDetails = loadWebDetails,
                                loadUsenetDetails = loadUsenetDetails,
                                closeDetails = closeDetails,
                            )
                        }
                        composable<Route.Search> {
                            TvSearchScreen(
                                canAddDownloads = isLoggedIn,
                                onDownloadLink = { link -> navController.navigate(Route.NewDownload(links = link)) },
                                onOpenExternalServices = { navController.navigate(Route.ExternalServices) },
                            )
                        }
                        composable<Route.NewDownload> { entry ->
                            val route = entry.toRoute<Route.NewDownload>()
                            TvNewDownloadScreen(initialLinks = route.links.orEmpty())
                        }
                        composable<Route.Settings> {
                            TvSettingsScreen(
                                onOpenProviders = { navController.navigate(Route.ManageProviders) },
                                onOpenExternalServices = { navController.navigate(Route.ExternalServices) },
                                onProvidersChanged = reloadProviders,
                            )
                        }
                        composable<Route.ManageProviders> {
                            TvProvidersScreen(
                                onBack = { navController.popBackStack() },
                                onProvidersChanged = reloadProviders,
                            )
                        }
                        composable<Route.ExternalServices> {
                            TvExternalServicesScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                )
            }
        }
    }
}
