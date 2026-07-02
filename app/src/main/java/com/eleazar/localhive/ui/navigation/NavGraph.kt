package com.eleazar.localhive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.eleazar.localhive.ui.auth.DetailsSetUpScreen
import com.eleazar.localhive.ui.auth.LoginScreen
import com.eleazar.localhive.ui.auth.OnboardingScreen
import com.eleazar.localhive.ui.auth.ProfileSetUpScreen
import com.eleazar.localhive.ui.auth.SignupScreen
import com.eleazar.localhive.ui.auth.SplashScreen
import com.eleazar.localhive.ui.directory.UserProfileScreen
import com.eleazar.localhive.ui.estate.EstateSelectionScreen
import com.eleazar.localhive.ui.estate.JoinEstateScreen
import com.eleazar.localhive.ui.feed.CreatePostScreen
import com.eleazar.localhive.ui.feed.PostDetailScreen
import com.eleazar.localhive.ui.main.MainScreen
import com.eleazar.localhive.ui.messages.ChatDetailScreen

@Composable
fun LocalHiveNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(
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
            SignupScreen(
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
            LoginScreen(
                onNavigateToSignup = {
                    navController.navigate(Screen.SignUp.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onLoginSuccess = { hasEstate ->
                    val dest = if (hasEstate) Screen.Main.route else Screen.EstateSelection.route
                    navController.navigate(dest) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(Screen.ProfileSetup.route) {
            ProfileSetUpScreen(
                onProfileComplete = {
                    navController.navigate(Screen.DetailsSetup.route) {
                        popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.DetailsSetup.route) {
            DetailsSetUpScreen(
                onDetailsComplete = {
                    navController.navigate(Screen.EstateSelection.route) {
                        popUpTo(Screen.DetailsSetup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.EstateSelection.route) {
            EstateSelectionScreen(
                onEstateJoined = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(navController = navController)
        }

        composable("create_post") {
            CreatePostScreen(onNavigateBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.PostDetail.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            PostDetailScreen(postId = postId, onNavigateBack = { navController.navigateUp() })
        }

        composable(
            route = Screen.ChatDetail.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatDetailScreen(chatId = chatId, onNavigateBack = { navController.navigateUp() })
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
            JoinEstateScreen(
                initialCode = code,
                onSuccess = {
                    navController.navigate(Screen.Main.route) { popUpTo(0) { inclusive = true } }
                },
                onBack = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.UserProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserProfileScreen(
                userId = userId,
                onNavigateBack = { navController.navigateUp() },
                onMessageUser = { uid ->
                    navController.navigate(Screen.ChatDetail.createRoute(uid))
                }
            )
        }
    }
}
