package com.eleazar.localhive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.eleazar.localhive.ui.auth.LoginScreen
import com.eleazar.localhive.ui.auth.OnboardingScreen
import com.eleazar.localhive.ui.auth.SignUpScreen
import com.eleazar.localhive.ui.directory.UserProfileScreen
import com.eleazar.localhive.ui.estate.EstateSelectionScreen
import com.eleazar.localhive.ui.feed.PostDetailScreen
import com.eleazar.localhive.ui.main.MainScreen
import com.eleazar.localhive.ui.messages.ChatDetailScreen

@Composable
fun LocalHiveNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            com.eleazar.localhive.ui.auth.SplashScreen(
                onNavigateNext = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Onboarding
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateNext = {
                    navController.navigate(Screen.SignUp.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(

            )
        }

        // OTP Verification
        composable( Screen.Login.route){
            LoginScreen(

            )
        }

        // Profile Setup
        composable(Screen.ProfileSetup.route) {
//            ProfileSetupScreen(
//                onProfileComplete = {
//                    navController.navigate(Screen.EstateSelection.route) {
//                        popUpTo(Screen.PhoneInput.route) { inclusive = true }
//                    }
//                }
//            )
        }

        // Estate Selection
        composable(Screen.EstateSelection.route) {
            EstateSelectionScreen(
//                onEstateSelected = {
//                    navController.navigate(Screen.Main.route) {
//                        popUpTo(Screen.Splash.route) { inclusive = true }
//                    }
//                }
            )
        }

        // Main App (Bottom Navigation)
        composable(Screen.Main.route) {
//            MainScreen(navController = navController)
        }

        // Post Detail
        composable(
            route = Screen.PostDetail.route,
            arguments = listOf(
                navArgument("postId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
//            val postId = backStackEntry.arguments?.getString("postId")
//            PostDetailScreen(
//                postId = postId ?: "",
//                onNavigateBack = { navController.navigateUp() }
//            )
        }

        // Chat Detail
        composable(
            route = Screen.ChatDetail.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId")
            ChatDetailScreen(
//                chatId = chatId ?: "",
//                onNavigateBack = { navController.navigateUp() }
            )
        }

        // User Profile
        composable(
            route = Screen.UserProfile.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            UserProfileScreen(
//                userId = userId ?: "",
//                onNavigateBack = { navController.navigateUp() },
//                onMessageUser = { chatId ->
//                    navController.navigate(Screen.ChatDetail.createRoute(chatId))
//                }
            )
        }
    }
}