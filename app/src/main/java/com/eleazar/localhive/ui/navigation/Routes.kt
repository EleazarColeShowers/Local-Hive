package com.eleazar.localhive.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object SignUp : Screen("signup")
    object Login : Screen("login")
    object ProfileSetup : Screen("profile_setup")
    object EstateSelection : Screen("estate_selection")
    object Main : Screen("main")

    object Feed : Screen("feed")
    object Directory : Screen("directory")
    object Messages : Screen("messages")
    object Profile : Screen("profile")

    object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: String) = "post_detail/$postId"
    }
    object ChatDetail : Screen("chat_detail/{chatId}") {
        fun createRoute(chatId: String) = "chat_detail/$chatId"
    }
    object UserProfile : Screen("user_profile/{userId}") {
        fun createRoute(userId: String) = "user_profile/$userId"
    }
}