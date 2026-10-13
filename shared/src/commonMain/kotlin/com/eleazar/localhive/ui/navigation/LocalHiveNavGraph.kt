package com.eleazar.localhive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.eleazar.localhive.ui.auth.OnboardingScreen

/** Clears the whole back stack, so the destination becomes the new root. */
private fun NavOptionsBuilder.clearBackStack() {
    popUpTo(Screen.Root.route) { inclusive = true }
}

@Composable
fun LocalHiveNavGraph(
    screens: LocalHiveScreens,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        route = Screen.Root.route
    ) {

        composable(Screen.Splash.route) {
            screens.Splash(
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToEstateSelection = {
                    navController.navigate(Screen.EstateSelection.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateNext = {
                    navController.navigate(Screen.SignUp.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SignUp.route) {
            screens.SignUp(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                },
                onSignupSuccess = {
                    navController.navigate(Screen.ProfileSetup.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            screens.Login(
                onNavigateToSignup = {
                    navController.navigate(Screen.SignUp.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onLoginSuccess = { hasEstate ->
                    val dest = if (hasEstate) Screen.Main.route else Screen.EstateSelection.route
                    navController.navigate(dest) { clearBackStack() }
                }
            )
        }

        composable(Screen.ProfileSetup.route) {
            screens.ProfileSetup(
                onProfileComplete = {
                    navController.navigate(Screen.DetailsSetup.route) {
                        popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.DetailsSetup.route) {
            screens.DetailsSetup(
                onDetailsComplete = {
                    navController.navigate(Screen.EstateSelection.route) {
                        popUpTo(Screen.DetailsSetup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.EstateSelection.route) {
            screens.EstateSelection(
                onEstateJoined = {
                    navController.navigate(Screen.Main.route) { clearBackStack() }
                }
            )
        }

        composable(Screen.Main.route) {
            val actions = remember(navController) {
                MainActions(
                    onPostClick = { postId -> navController.navigate(Screen.PostDetail.createRoute(postId)) },
                    onUserClick = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) },
                    onChatClick = { chatId -> navController.navigate(Screen.ChatDetail.createRoute(chatId)) },
                    onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                    onCreatePost = { navController.navigate(Screen.CreatePost.route) },
                    onSettings = { navController.navigate(Screen.Settings.route) },
                    onAdminPanel = { estateId -> navController.navigate(Screen.EstateAdmin.createRoute(estateId)) },
                    onLeftEstate = {
                        navController.navigate(Screen.EstateSelection.route) { clearBackStack() }
                    },
                    onRemovedFromEstate = {
                        // Leaving from the Profile tab also clears estateId and navigates there itself.
                        if (navController.currentDestination?.route == Screen.Main.route) {
                            navController.navigate(Screen.EstateSelection.route) { clearBackStack() }
                        }
                    }
                )
            }
            screens.Main(actions)
        }

        composable(Screen.CreatePost.route) {
            screens.CreatePost(onNavigateBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.PostDetail.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType }),
            // Announcement pushes open the post directly.
            deepLinks = listOf(navDeepLink { uriPattern = "localhive://post/{postId}" })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            screens.PostDetail(postId = postId, onNavigateBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.ChatDetail.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "localhive://chat/{chatId}" })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            screens.ChatDetail(
                chatId = chatId,
                onNavigateBack = { navController.navigateUp() },
                onOpenProfile = { userId ->
                    // Arriving from that person's profile: go back to it rather than stacking a copy.
                    val previous = navController.previousBackStackEntry
                    if (previous?.destination?.route == Screen.UserProfile.route &&
                        previous.arguments?.getString("userId") == userId
                    ) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.UserProfile.createRoute(userId))
                    }
                }
            )
        }

        composable(
            route = Screen.JoinEstate.route,
            arguments = listOf(navArgument("inviteCode") {
                type = NavType.StringType
                defaultValue = ""
            }),
            deepLinks = listOf(navDeepLink { uriPattern = "localhive://join/{inviteCode}" })
        ) { backStackEntry ->
            val code = backStackEntry.arguments?.getString("inviteCode") ?: ""
            screens.JoinEstate(
                initialCode = code,
                onSuccess = {
                    navController.navigate(Screen.Main.route) { clearBackStack() }
                },
                onBack = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.UserProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            screens.UserProfile(
                userId = userId,
                onNavigateBack = { navController.navigateUp() },
                onMessageUser = { chatId ->
                    navController.navigate(Screen.ChatDetail.createRoute(chatId))
                }
            )
        }

        composable(
            route = Screen.EstateAdmin.route,
            arguments = listOf(navArgument("estateId") { type = NavType.StringType })
        ) { backStackEntry ->
            val estateId = backStackEntry.arguments?.getString("estateId") ?: ""
            screens.EstateAdmin(
                estateId = estateId,
                onBack = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.Notifications.route,
            deepLinks = listOf(navDeepLink { uriPattern = "localhive://notifications" })
        ) {
            screens.Notifications(
                onBack = { navController.navigateUp() },
                onNavigateToEstateAdmin = { estateId ->
                    navController.navigate(Screen.EstateAdmin.createRoute(estateId))
                }
            )
        }

        composable(Screen.Settings.route) {
            screens.Settings(
                onBack = { navController.navigateUp() },
                onLoggedOut = {
                    navController.navigate(Screen.Splash.route) { clearBackStack() }
                },
                onLeftEstate = {
                    navController.navigate(Screen.EstateSelection.route) { clearBackStack() }
                },
                onBlockedUsers = { navController.navigate(Screen.BlockedUsers.route) }
            )
        }

        composable(Screen.BlockedUsers.route) {
            screens.BlockedUsers(onBack = { navController.navigateUp() })
        }
    }
}
