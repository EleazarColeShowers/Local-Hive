package com.eleazar.localhive.ui.main

// ───────────────────────────────────────────────────────────────────────────
// APP MODULE  (route: Hilt, navigation, resources, state collection)
// ───────────────────────────────────────────────────────────────────────────

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.eleazar.localhive.R
import com.eleazar.localhive.ui.directory.DirectoryScreen
import com.eleazar.localhive.ui.feed.FeedScreen
import com.eleazar.localhive.ui.messages.MessagesScreen
import com.eleazar.localhive.ui.messages.MessagesViewModel
import com.eleazar.localhive.ui.navigation.Screen
import com.eleazar.localhive.ui.notifications.NotificationViewModel
import com.eleazar.localhive.ui.profile.ProfileRoute

/** [navController] is the app-level controller; the tabs use their own nested controller. */
@Composable
fun MainRoute(navController: NavHostController) {
    val bottomNavController = rememberNavController()

    val notificationViewModel: NotificationViewModel = hiltViewModel()
    val notificationUiState by notificationViewModel.uiState.collectAsStateWithLifecycle()

    // Hoisted so the tab badge and the Messages tab share one chats listener.
    val messagesViewModel: MessagesViewModel = hiltViewModel()
    val messagesUiState by messagesViewModel.uiState.collectAsStateWithLifecycle()

    val backStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val tabs = remember(messagesUiState.totalUnread) {
        listOf(
            MainTab(Screen.Feed.route, "Feed", Icons.Outlined.Home, Icons.Filled.Home),
            MainTab(Screen.Directory.route, "Directory", Icons.Outlined.Search, Icons.Filled.Search),
            MainTab(
                Screen.Messages.route, "Messages", Icons.Outlined.Email, Icons.Filled.Email,
                badgeCount = messagesUiState.totalUnread
            ),
            MainTab(
                Screen.Profile.route, "Profile", Icons.Outlined.Person, Icons.Filled.Person,
                edgeToEdge = true // Profile has its own hero header
            )
        )
    }

    val colors = MainScreenColors(
        accent = colorResource(R.color.hivegreen),
        chrome = colorResource(R.color.darkgray)
    )

    MainScreen(
        tabs = tabs,
        currentRoute = currentRoute,
        unreadNotifications = notificationUiState.unreadCount,
        colors = colors,
        onTabSelected = { tab ->
            bottomNavController.navigate(tab.route) {
                popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
        onCreatePostClick = { navController.navigate("create_post") }
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = Screen.Feed.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Feed.route) {
                FeedScreen(
                    onPostClick = { postId ->
                        navController.navigate(Screen.PostDetail.createRoute(postId))
                    }
                )
            }
            composable(Screen.Directory.route) {
                DirectoryScreen(
                    onUserClick = { userId ->
                        navController.navigate(Screen.UserProfile.createRoute(userId))
                    },
                    onMessageUser = { userId ->
                        navController.navigate(Screen.UserProfile.createRoute(userId))
                    }
                )
            }
            composable(Screen.Messages.route) {
                MessagesScreen(
                    onChatClick = { chatId ->
                        navController.navigate(Screen.ChatDetail.createRoute(chatId))
                    },
                    viewModel = messagesViewModel
                )
            }
            composable(Screen.Profile.route) {
                ProfileRoute(
                    onSettings = { navController.navigate(Screen.Settings.route) },
                    onAdminPanel = { estateId ->
                        navController.navigate(Screen.EstateAdmin.createRoute(estateId))
                    }
                )
            }
        }
    }
}