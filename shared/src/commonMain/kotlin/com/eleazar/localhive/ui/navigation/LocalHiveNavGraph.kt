package com.eleazar.localhive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

/**
 * @param hasPendingShare something was shared into the app from elsewhere. The composer opens
 *   as soon as the user reaches the home screen (signing in first if they need to), and
 *   [LocalHiveScreens.CreatePost] is expected to pick the share up.
 * @param pendingLink a localhive:// link to open, such as from a tapped push on iOS, where the
 *   deep links declared below aren't delivered. Like a share, it waits for the user to reach
 *   the home screen; [onLinkHandled] is called once it has been followed or found unusable.
 */
@Composable
fun LocalHiveNavGraph(
    screens: LocalHiveScreens,
    navController: NavHostController = rememberNavController(),
    hasPendingShare: Boolean = false,
    pendingLink: String? = null,
    onLinkHandled: () -> Unit = {}
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
                    navController.navigate(Screen.EstateSelection.route) {
                        popUpTo(Screen.ProfileSetup.route) { inclusive = true }
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
            // An invite link is the one link that's useful before the user has an estate.
            val joinRoute = pendingLink?.let(::routeForLink)?.takeIf { it.startsWith("join_estate") }
            if (joinRoute != null) {
                LaunchedEffect(pendingLink) {
                    navController.navigate(joinRoute) { launchSingleTop = true }
                    onLinkHandled()
                }
            }
        }

        composable(Screen.Main.route) {
            val actions = remember(navController) {
                MainActions(
                    onPostClick = { postId -> navController.navigate(Screen.PostDetail.createRoute(postId)) },
                    onUserClick = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) },
                    onChatClick = { chatId -> navController.navigate(Screen.ChatDetail.createRoute(chatId)) },
                    onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                    onCreatePost = { navController.navigate(Screen.CreatePost.route) },
                    onEmergency = { navController.navigate(Screen.EmergencyAlert.route) { launchSingleTop = true } },
                    onEditPost = { postId -> navController.navigate(Screen.EditPost.createRoute(postId)) },
                    onPolls = { navController.navigate(Screen.Polls.route) { launchSingleTop = true } },
                    onUtilities = { navController.navigate(Screen.Utilities.route) { launchSingleTop = true } },
                    onSettings = { navController.navigate(Screen.Settings.route) },
                    onAdminPanel = { estateId -> navController.navigate(Screen.EstateAdmin.createRoute(estateId)) },
                    onFinances = { navController.navigate(Screen.Finances.route) },
                    onRemovedFromEstate = {
                        // Leaving from Settings also clears estateId and navigates there itself.
                        if (navController.currentDestination?.route == Screen.Main.route) {
                            navController.navigate(Screen.EstateSelection.route) { clearBackStack() }
                        }
                    }
                )
            }
            screens.Main(actions)
            if (hasPendingShare) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.CreatePost.route) { launchSingleTop = true }
                }
            }
            if (pendingLink != null) {
                LaunchedEffect(pendingLink) {
                    routeForLink(pendingLink)?.let { navController.navigate(it) { launchSingleTop = true } }
                    onLinkHandled()
                }
            }
        }

        composable(Screen.CreatePost.route) {
            screens.CreatePost(onNavigateBack = { navController.navigateUp() })
        }

        composable(Screen.EmergencyAlert.route) {
            screens.EmergencyAlert(onNavigateBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.PostDetail.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType }),
            // Announcement pushes open the post directly.
            deepLinks = listOf(navDeepLink { uriPattern = "localhive://post/{postId}" })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            screens.PostDetail(
                postId = postId,
                onNavigateBack = { navController.navigateUp() },
                onOpenProfile = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) },
                onEditPost = { id -> navController.navigate(Screen.EditPost.createRoute(id)) }
            )
        }

        composable(
            route = Screen.Polls.route,
            // New-poll pushes open the list.
            deepLinks = listOf(navDeepLink { uriPattern = "localhive://polls" })
        ) {
            screens.Polls(
                onBack = { navController.navigateUp() },
                onCreatePoll = { navController.navigate(Screen.CreatePoll.route) }
            )
        }

        composable(Screen.CreatePoll.route) {
            screens.CreatePoll(onBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.Utilities.route,
            // "Light is back" pushes open it.
            deepLinks = listOf(navDeepLink { uriPattern = "localhive://utilities" })
        ) {
            screens.Utilities(onBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.EditPost.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            screens.EditPost(
                postId = backStackEntry.arguments?.getString("postId") ?: "",
                onNavigateBack = { navController.navigateUp() }
            )
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
                },
                onNavigateToPost = { postId -> navController.navigate(Screen.PostDetail.createRoute(postId)) },
                onNavigateToLevy = { levyId -> navController.navigate(Screen.LevyDetail.createRoute(levyId)) }
            )
        }

        composable(Screen.Finances.route) {
            screens.Finances(
                onBack = { navController.navigateUp() },
                onLevyClick = { levyId -> navController.navigate(Screen.LevyDetail.createRoute(levyId)) },
                onCreateLevy = { navController.navigate(Screen.CreateLevy.route) },
                onAddExpense = { navController.navigate(Screen.AddExpense.route) }
            )
        }

        composable(
            route = Screen.LevyDetail.route,
            arguments = listOf(navArgument("levyId") { type = NavType.StringType }),
            // Payment notices and levy reminders open the levy directly.
            deepLinks = listOf(navDeepLink { uriPattern = "localhive://levy/{levyId}" })
        ) { backStackEntry ->
            val levyId = backStackEntry.arguments?.getString("levyId") ?: ""
            screens.LevyDetail(levyId = levyId, onBack = { navController.navigateUp() })
        }

        composable(Screen.CreateLevy.route) {
            screens.CreateLevy(onBack = { navController.navigateUp() })
        }

        composable(Screen.AddExpense.route) {
            screens.AddExpense(onBack = { navController.navigateUp() })
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
