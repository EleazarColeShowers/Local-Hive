package com.eleazar.localhive.ui.navigation

import androidx.compose.runtime.Composable
import com.eleazar.localhive.ui.auth.DetailsSetUpRoute
import com.eleazar.localhive.ui.auth.LoginRoute
import com.eleazar.localhive.ui.auth.ProfileSetUpRoute
import com.eleazar.localhive.ui.auth.SignupRoute
import com.eleazar.localhive.ui.auth.SplashRoute
import com.eleazar.localhive.ui.blocking.BlockedUsersRoute
import com.eleazar.localhive.ui.directory.UserProfileRoute
import com.eleazar.localhive.ui.estate.EstateAdminRoute
import com.eleazar.localhive.ui.estate.EstateSelectionRoute
import com.eleazar.localhive.ui.estate.JoinEstateRoute
import com.eleazar.localhive.ui.feed.CreatePostRoute
import com.eleazar.localhive.ui.feed.PostDetailRoute
import com.eleazar.localhive.ui.main.MainRoute
import com.eleazar.localhive.ui.messages.ChatDetailRoute
import com.eleazar.localhive.ui.notifications.NotificationRoute
import com.eleazar.localhive.ui.settings.SettingsRoute

/** Android destinations for the shared [LocalHiveNavGraph]: each one is a Hilt-backed route. */
object AndroidLocalHiveScreens : LocalHiveScreens {

    @Composable
    override fun Splash(
        onNavigateToOnboarding: () -> Unit,
        onNavigateToEstateSelection: () -> Unit,
        onNavigateToMain: () -> Unit
    ) = SplashRoute(onNavigateToOnboarding, onNavigateToEstateSelection, onNavigateToMain)

    @Composable
    override fun SignUp(onNavigateToLogin: () -> Unit, onSignupSuccess: () -> Unit) =
        SignupRoute(onNavigateToLogin = onNavigateToLogin, onSignupSuccess = onSignupSuccess)

    @Composable
    override fun Login(onNavigateToSignup: () -> Unit, onLoginSuccess: (hasEstate: Boolean) -> Unit) =
        LoginRoute(onNavigateToSignup = onNavigateToSignup, onLoginSuccess = onLoginSuccess)

    @Composable
    override fun ProfileSetup(onProfileComplete: () -> Unit) =
        ProfileSetUpRoute(onProfileComplete = onProfileComplete)

    @Composable
    override fun DetailsSetup(onDetailsComplete: () -> Unit) =
        DetailsSetUpRoute(onDetailsComplete = onDetailsComplete)

    @Composable
    override fun EstateSelection(onEstateJoined: () -> Unit) =
        EstateSelectionRoute(onEstateJoined = onEstateJoined)

    @Composable
    override fun Main(actions: MainActions) = MainRoute(actions = actions)

    @Composable
    override fun CreatePost(onNavigateBack: () -> Unit) =
        CreatePostRoute(onNavigateBack = onNavigateBack)

    @Composable
    override fun PostDetail(postId: String, onNavigateBack: () -> Unit) =
        PostDetailRoute(postId = postId, onNavigateBack = onNavigateBack)

    @Composable
    override fun ChatDetail(chatId: String, onNavigateBack: () -> Unit, onOpenProfile: (userId: String) -> Unit) =
        ChatDetailRoute(chatId = chatId, onNavigateBack = onNavigateBack, onOpenProfile = onOpenProfile)

    @Composable
    override fun JoinEstate(initialCode: String, onSuccess: () -> Unit, onBack: () -> Unit) =
        JoinEstateRoute(initialCode = initialCode, onSuccess = onSuccess, onBack = onBack)

    @Composable
    override fun UserProfile(userId: String, onNavigateBack: () -> Unit, onMessageUser: (chatId: String) -> Unit) =
        UserProfileRoute(userId = userId, onNavigateBack = onNavigateBack, onMessageUser = onMessageUser)

    @Composable
    override fun EstateAdmin(estateId: String, onBack: () -> Unit) =
        EstateAdminRoute(estateId = estateId, onBack = onBack)

    @Composable
    override fun Notifications(onBack: () -> Unit, onNavigateToEstateAdmin: (estateId: String) -> Unit) =
        NotificationRoute(onBack = onBack, onNavigateToEstateAdmin = onNavigateToEstateAdmin)

    @Composable
    override fun Settings(
        onBack: () -> Unit,
        onLoggedOut: () -> Unit,
        onLeftEstate: () -> Unit,
        onBlockedUsers: () -> Unit
    ) = SettingsRoute(
        onBack = onBack,
        onLoggedOut = onLoggedOut,
        onLeftEstate = onLeftEstate,
        onBlockedUsers = onBlockedUsers
    )

    @Composable
    override fun BlockedUsers(onBack: () -> Unit) = BlockedUsersRoute(onBack = onBack)
}
