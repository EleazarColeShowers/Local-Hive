package com.eleazar.localhive.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.eleazar.localhive.ui.main.MainScreen
import com.eleazar.localhive.ui.main.MainScreenColors
import com.eleazar.localhive.ui.main.MainTab

/** Fires each time the open tab's own button is tapped again. Empty outside the tab host. */
val LocalTabReselected = staticCompositionLocalOf<Flow<Unit>> { emptyFlow() }

/** Runs [action] when the user taps the button of the tab they're already on, e.g. to scroll to the top. */
@Composable
fun OnTabReselected(action: suspend () -> Unit) {
    val reselected = LocalTabReselected.current
    val currentAction by rememberUpdatedState(action)
    LaunchedEffect(reselected) { reselected.collect { currentAction() } }
}

/**
 * [MainScreen] plus the bottom-tab navigation. The tabs get their own nested controller,
 * separate from the app-level one in [LocalHiveNavGraph]; the first tab is the start destination.
 *
 * [tabContent] receives the tab's route and a `selectTab` callback for switching tabs from
 * inside a tab (e.g. Messages → Directory).
 */
@Composable
fun MainTabsHost(
    tabs: List<MainTab>,
    unreadNotifications: Int,
    colors: MainScreenColors,
    onNotificationsClick: () -> Unit,
    onCreatePostClick: () -> Unit,
    tabContent: @Composable (route: String, selectTab: (String) -> Unit) -> Unit
) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()

    val selectTab: (String) -> Unit = remember(tabNavController) {
        { route ->
            tabNavController.navigate(route) {
                popUpTo(tabNavController.graph.findStartDestination().route!!) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Keyed on routes only, so badge updates don't rebuild the tab graph.
    val routes = remember(tabs) { tabs.map { it.route } }
    val currentTabContent by rememberUpdatedState(tabContent)
    val reselects = remember { MutableSharedFlow<String>(extraBufferCapacity = 1) }
    val currentRoute = backStackEntry?.destination?.route

    MainScreen(
        tabs = tabs,
        currentRoute = currentRoute,
        unreadNotifications = unreadNotifications,
        colors = colors,
        onTabSelected = { tab ->
            if (tab.route == currentRoute) reselects.tryEmit(tab.route) else selectTab(tab.route)
        },
        onNotificationsClick = onNotificationsClick,
        onCreatePostClick = onCreatePostClick
    ) { paddingValues ->
        NavHost(
            navController = tabNavController,
            startDestination = routes.first(),
            modifier = Modifier.padding(paddingValues)
        ) {
            routes.forEach { route ->
                composable(route) {
                    val reselected = remember(route) { reselects.filter { it == route }.map { } }
                    CompositionLocalProvider(LocalTabReselected provides reselected) {
                        currentTabContent(route, selectTab)
                    }
                }
            }
        }
    }
}
