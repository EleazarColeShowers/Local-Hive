package com.eleazar.localhive.ui.main

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.eleazar.localhive.R
import com.eleazar.localhive.ui.directory.DirectoryScreen
import com.eleazar.localhive.ui.feed.CreatePostScreen
import com.eleazar.localhive.ui.feed.FeedScreen
import com.eleazar.localhive.ui.messages.MessagesScreen
import com.eleazar.localhive.ui.navigation.Screen
import com.eleazar.localhive.ui.profile.ProfileScreen

@Composable
fun MainScreen(navController: NavHostController) {
    val bottomNavController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(bottomNavController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("create_post") },
                containerColor = colorResource(id = R.color.hivegreen)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Post", tint = androidx.compose.ui.graphics.Color.White)
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = Screen.Feed.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Feed.route) {
                FeedScreen(
                    onPostClick = { postId -> navController.navigate(Screen.PostDetail.createRoute(postId)) }
                )
            }
            composable(Screen.Directory.route) {
                DirectoryScreen(
                    onUserClick = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) },
                    onMessageUser = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) }
                )
            }
            composable(Screen.Messages.route) {
                MessagesScreen(
                    onChatClick = { chatId -> navController.navigate(Screen.ChatDetail.createRoute(chatId)) }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(Screen.Splash.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Feed", Icons.Default.Home, Screen.Feed.route),
        BottomNavItem("Directory", Icons.Default.Person, Screen.Directory.route),
        BottomNavItem("", Icons.Default.Add, ""),
        BottomNavItem("Messages", Icons.Default.Email, Screen.Messages.route),
        BottomNavItem("Profile", Icons.Default.Person, Screen.Profile.route)
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            if (item.route.isEmpty()) {
                Spacer(modifier = Modifier.weight(1f))
            } else {
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) },
                    selected = currentRoute == item.route,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colorResource(id = R.color.hivegreen),
                        selectedTextColor = colorResource(id = R.color.hivegreen),
                        indicatorColor = colorResource(id = R.color.hivegreen).copy(alpha = 0.12f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                )
            }
        }
    }
}

data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)
