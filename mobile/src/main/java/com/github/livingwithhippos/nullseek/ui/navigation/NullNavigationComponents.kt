package com.github.livingwithhippos.nullseek.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.toSize
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.window.core.layout.WindowSizeClass
import com.github.livingwithhippos.nullseek.R
import com.github.livingwithhippos.nullseek.ui.utils.NullNavigationContentPosition
import kotlinx.coroutines.launch


private fun WindowSizeClass.isCompact() = !isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) ||
        !isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)

class NullNavSuiteScope(val navSuiteType: NavigationSuiteType)

@Composable
fun NullNavigationWrapper(
    currentDestination: NavDestination?,
    navigateToTopLevelDestination: (TopLevelDestination) -> Unit,
    content: @Composable NullNavSuiteScope.() -> Unit,
) {
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val windowSize = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.toSize().toDpSize()
    }

    val navLayoutType = when {
        adaptiveInfo.windowPosture.isTabletop -> NavigationSuiteType.NavigationBar

        adaptiveInfo.windowSizeClass.isCompact() -> NavigationSuiteType.NavigationBar

        adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) &&
                windowSize.width >= 1200.dp -> NavigationSuiteType.NavigationDrawer

        else -> NavigationSuiteType.NavigationRail
    }
    val navContentPosition = if (adaptiveInfo.windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)) {
        NullNavigationContentPosition.CENTER
    } else {
        NullNavigationContentPosition.TOP
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    // Avoid opening the modal drawer when there is a permanent drawer or a bottom nav bar,
    // but always allow closing an open drawer.
    val gesturesEnabled =
        drawerState.isOpen || navLayoutType == NavigationSuiteType.NavigationRail

    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch {
            drawerState.close()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        drawerContent = {
            ModalNavigationDrawerContent(
                currentDestination = currentDestination,
                navigationContentPosition = navContentPosition,
                navigateToTopLevelDestination = navigateToTopLevelDestination,
                onDrawerClicked = {
                    coroutineScope.launch {
                        drawerState.close()
                    }
                },
            )
        },
    ) {
        NavigationSuiteScaffoldLayout(
            layoutType = navLayoutType,
            navigationSuite = {
                when (navLayoutType) {
                    NavigationSuiteType.NavigationBar -> NullBottomNavigationBar(
                        currentDestination = currentDestination,
                        navigateToTopLevelDestination = navigateToTopLevelDestination,
                    )

                    NavigationSuiteType.NavigationRail -> NullNavigationRail(
                        currentDestination = currentDestination,
                        navigationContentPosition = navContentPosition,
                        navigateToTopLevelDestination = navigateToTopLevelDestination,
                        onDrawerClicked = {
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        },
                    )

                    NavigationSuiteType.NavigationDrawer -> PermanentNavigationDrawerContent(
                        currentDestination = currentDestination,
                        navigationContentPosition = navContentPosition,
                        navigateToTopLevelDestination = navigateToTopLevelDestination,
                    )
                }
            },
        ) {
            NullNavSuiteScope(navLayoutType).content()
        }
    }
}

@Composable
fun NullNavigationRail(
    currentDestination: NavDestination?,
    navigationContentPosition: NullNavigationContentPosition,
    navigateToTopLevelDestination: (TopLevelDestination) -> Unit,
    onDrawerClicked: () -> Unit = {},
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Column(
            modifier = Modifier.layoutId(LayoutType.HEADER),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            NavigationRailItem(
                selected = false,
                onClick = onDrawerClicked,
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_menu),
                        contentDescription = stringResource(id = R.string.navigation_drawer),
                    )
                },
            )
            FloatingActionButton(
                onClick = { navigateToTopLevelDestination(SETTINGS_DESTINATION) },
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ) {
                Icon(
                    painter = painterResource(id = SETTINGS_DESTINATION.selectedIcon),
                    contentDescription = stringResource(id = SETTINGS_DESTINATION.iconTextId),
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(8.dp)) // NavigationRailHeaderPadding
            Spacer(Modifier.height(4.dp)) // NavigationRailVerticalPadding
        }

        Column(
            modifier = Modifier.layoutId(LayoutType.CONTENT),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TOP_LEVEL_DESTINATIONS.forEach { nullDestination ->
                NavigationRailItem(
                    selected = currentDestination.hasRoute(nullDestination),
                    onClick = { navigateToTopLevelDestination(nullDestination) },
                    icon = {
                        Icon(
                            painter = painterResource(id = nullDestination.selectedIcon),
                            contentDescription = stringResource(
                                id = nullDestination.iconTextId,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun NullBottomNavigationBar(currentDestination: NavDestination?, navigateToTopLevelDestination: (TopLevelDestination) -> Unit) {
    NavigationBar(modifier = Modifier.fillMaxWidth()) {
        TOP_LEVEL_DESTINATIONS.forEach { nullDestination ->
            NavigationBarItem(
                selected = currentDestination.hasRoute(nullDestination),
                onClick = { navigateToTopLevelDestination(nullDestination) },
                icon = {
                    Icon(
                        painter = painterResource(id = nullDestination.selectedIcon),
                        contentDescription = stringResource(id = nullDestination.iconTextId),
                    )
                },
            )
        }
    }
}

@Composable
fun PermanentNavigationDrawerContent(
    currentDestination: NavDestination?,
    navigationContentPosition: NullNavigationContentPosition,
    navigateToTopLevelDestination: (TopLevelDestination) -> Unit
) {
    PermanentDrawerSheet(
        modifier = Modifier.sizeIn(minWidth = 200.dp, maxWidth = 300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        // TODO remove custom nav drawer content positioning when NavDrawer component supports it. ticket : b/232495216
        Layout(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(16.dp),
            content = {
                Column(
                    modifier = Modifier.layoutId(LayoutType.HEADER),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        modifier = Modifier
                            .padding(16.dp),
                        text = stringResource(id = R.string.app_name).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    ExtendedFloatingActionButton(
                        onClick = { navigateToTopLevelDestination(SETTINGS_DESTINATION) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 40.dp),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ) {
                        Icon(
                            painter = painterResource(id = SETTINGS_DESTINATION.selectedIcon),
                            contentDescription = stringResource(id = SETTINGS_DESTINATION.iconTextId),
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = stringResource(id = SETTINGS_DESTINATION.iconTextId),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .layoutId(LayoutType.CONTENT)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TOP_LEVEL_DESTINATIONS.forEach { nullDestination ->
                        NavigationDrawerItem(
                            selected = currentDestination.hasRoute(nullDestination),
                            label = {
                                Text(
                                    text = stringResource(id = nullDestination.iconTextId),
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(id = nullDestination.selectedIcon),
                                    contentDescription = stringResource(
                                        id = nullDestination.iconTextId,
                                    ),
                                )
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                            ),
                            onClick = { navigateToTopLevelDestination(nullDestination) },
                        )
                    }
                }
            },
            measurePolicy = navigationMeasurePolicy(navigationContentPosition),
        )
    }
}

@Composable
fun ModalNavigationDrawerContent(
    currentDestination: NavDestination?,
    navigationContentPosition: NullNavigationContentPosition,
    navigateToTopLevelDestination: (TopLevelDestination) -> Unit,
    onDrawerClicked: () -> Unit = {},
) {
    ModalDrawerSheet {
        // TODO remove custom nav drawer content positioning when NavDrawer component supports it. ticket : b/232495216
        Layout(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.inverseOnSurface)
                .padding(16.dp),
            content = {
                Column(
                    modifier = Modifier.layoutId(LayoutType.HEADER),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(id = R.string.app_name).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        IconButton(onClick = onDrawerClicked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_menu_open),
                                contentDescription = stringResource(id = R.string.close_drawer),
                            )
                        }
                    }

                    ExtendedFloatingActionButton(
                        onClick = { navigateToTopLevelDestination(SETTINGS_DESTINATION) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 40.dp),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ) {
                        Icon(
                            painter = painterResource(id = SETTINGS_DESTINATION.selectedIcon),
                            contentDescription = stringResource(id = SETTINGS_DESTINATION.iconTextId),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(id = SETTINGS_DESTINATION.iconTextId),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .layoutId(LayoutType.CONTENT)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TOP_LEVEL_DESTINATIONS.forEach { nullDestination ->
                        NavigationDrawerItem(
                            selected = currentDestination.hasRoute(nullDestination),
                            label = {
                                Text(
                                    text = stringResource(id = nullDestination.iconTextId),
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(id = nullDestination.selectedIcon),
                                    contentDescription = stringResource(
                                        id = nullDestination.iconTextId,
                                    ),
                                )
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                            ),
                            onClick = { navigateToTopLevelDestination(nullDestination) },
                        )
                    }
                }
            },
            measurePolicy = navigationMeasurePolicy(navigationContentPosition),
        )
    }
}

fun navigationMeasurePolicy(navigationContentPosition: NullNavigationContentPosition): MeasurePolicy {
    return MeasurePolicy { measurables, constraints ->
        lateinit var headerMeasurable: Measurable
        lateinit var contentMeasurable: Measurable
        measurables.forEach {
            when (it.layoutId) {
                LayoutType.HEADER -> headerMeasurable = it
                LayoutType.CONTENT -> contentMeasurable = it
                else -> error("Unknown layoutId encountered!")
            }
        }

        val headerPlaceable = headerMeasurable.measure(constraints)
        val contentPlaceable = contentMeasurable.measure(
            constraints.offset(vertical = -headerPlaceable.height),
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            // Place the header, this goes at the top
            headerPlaceable.placeRelative(0, 0)

            // Determine how much space is not taken up by the content
            val nonContentVerticalSpace = constraints.maxHeight - contentPlaceable.height

            val contentPlaceableY = when (navigationContentPosition) {
                // Figure out the place we want to place the content, with respect to the
                // parent (ignoring the header for now)
                NullNavigationContentPosition.TOP -> 0

                NullNavigationContentPosition.CENTER -> nonContentVerticalSpace / 2
            }
                // And finally, make sure we don't overlap with the header.
                .coerceAtLeast(headerPlaceable.height)

            contentPlaceable.placeRelative(0, contentPlaceableY)
        }
    }
}

enum class LayoutType {
    HEADER,
    CONTENT,
}

fun NavDestination?.hasRoute(destination: TopLevelDestination): Boolean = this?.hasRoute(destination.route::class) ?: false
