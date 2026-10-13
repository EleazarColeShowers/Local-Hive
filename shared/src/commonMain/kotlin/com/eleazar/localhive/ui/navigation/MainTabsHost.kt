package com.eleazar.localhive.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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

    MainScreen(
        tabs = tabs,
        currentRoute = backStackEntry?.destination?.route,
        unreadNotifications = unreadNotifications,
        colors = colors,
        onTabSelected = { tab -> selectTab(tab.route) },
        onNotificationsClick = onNotificationsClick,
        onCreatePostClick = onCreatePostClick
    ) { paddingValues ->
        NavHost(
            navController = tabNavController,
            startDestination = routes.first(),
            modifier = Modifier.padding(paddingValues)
        ) {
            routes.forEach { route ->
                composable(route) { currentTabContent(route, selectTab) }
            }
        }
    }
}
