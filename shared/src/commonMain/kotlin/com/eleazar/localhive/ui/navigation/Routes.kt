package com.eleazar.localhive.ui.navigation

sealed class Screen(val route: String) {
    /** Route of the root graph itself; popping up to it (inclusive) clears the whole back stack. */
    object Root : Screen("root")

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

    object CreatePost : Screen("create_post")
    object EmergencyAlert : Screen("emergency_alert")
    object EditPost : Screen("edit_post/{postId}") {
        fun createRoute(postId: String) = "edit_post/$postId"
    }

    object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: String) = "post_detail/$postId"
    }
    object ChatDetail : Screen("chat_detail/{chatId}") {
        fun createRoute(chatId: String) = "chat_detail/$chatId"
    }
    object UserProfile : Screen("user_profile/{userId}") {
        fun createRoute(userId: String) = "user_profile/$userId"
    }

    object JoinEstate : Screen("join_estate?inviteCode={inviteCode}") {
        fun createRoute(inviteCode: String = "") = "join_estate?inviteCode=$inviteCode"
    }

    object EstateAdmin : Screen("estate_admin/{estateId}") {
        fun createRoute(estateId: String) = "estate_admin/$estateId"
    }

    object Finances : Screen("finances")
    object LevyDetail : Screen("levy/{levyId}") {
        fun createRoute(levyId: String) = "levy/$levyId"
    }
    object CreateLevy : Screen("create_levy")
    object Statement : Screen("finance_statement")
    object AddExpense : Screen("add_expense")

    object Polls : Screen("polls")
    object CreatePoll : Screen("create_poll")
    object Utilities : Screen("utilities")

    object Notifications : Screen("notifications")
    object Settings : Screen("settings")
    object BlockedUsers : Screen("blocked_users")
}