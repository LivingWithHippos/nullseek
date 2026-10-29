package com.github.livingwithhippos.nullseek.ui.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.github.livingwithhippos.nullseek.core.R

data class TopLevelDestination(
    val route: Route,
    val selectedIcon: Int,
    val unselectedIcon: Int,
    val iconTextId: Int,
)

class NavigationActions(
    private val navController: NavHostController,
) {
    fun navigateTo(destination: TopLevelDestination) {
        if (destination.route is Route.NewDownload) {
            openNewDownload()
            return
        }
        // leaving New Download through a tab: drop it, or it would be saved with the previous tab's state
        if (navController.currentDestination?.hasRoute<Route.NewDownload>() == true) {
            navController.popBackStack()
        }
        navController.navigate(destination.route) {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            // on the back stack as users select items
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when
            // reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }
    }

    /** Opens New Download over the current screen, so closing it returns there. */
    fun openNewDownload() {
        navController.navigate(Route.NewDownload()) { launchSingleTop = true }
    }
}

val TOP_LEVEL_DESTINATIONS =
    listOf(
        TopLevelDestination(
            route = Route.Home,
            selectedIcon = R.drawable.ic_contact_page,
            unselectedIcon = R.drawable.ic_contact_page,
            iconTextId = R.string.home,
        ),
        TopLevelDestination(
            route = Route.Lists,
            selectedIcon = R.drawable.ic_list_bullet,
            unselectedIcon = R.drawable.ic_list_bullet,
            iconTextId = R.string.lists,
        ),
        TopLevelDestination(
            route = Route.Search,
            selectedIcon = R.drawable.ic_manage_search,
            unselectedIcon = R.drawable.ic_manage_search,
            iconTextId = R.string.search,
        ),
    )

/**
 * The [TOP_LEVEL_DESTINATIONS] will be all shown in the menu bar, we want the settings page to be separated
 */
val SETTINGS_DESTINATION =
    TopLevelDestination(
        route = Route.Settings,
        selectedIcon = R.drawable.ic_settings,
        unselectedIcon = R.drawable.ic_settings,
        iconTextId = R.string.settings,
    )

val NEW_DOWNLOAD_DESTINATION =
    TopLevelDestination(
        route = Route.NewDownload(),
        selectedIcon = R.drawable.ic_add,
        unselectedIcon = R.drawable.ic_add,
        iconTextId = R.string.new_download,
    )

/**
 * If the lifecycle is not resumed it means this NavBackStackEntry already processed a nav event.
 *
 * This is used to de-duplicate navigation events.
 */
private fun NavBackStackEntry.lifecycleIsResumed() = this.lifecycle.currentState == Lifecycle.State.RESUMED
