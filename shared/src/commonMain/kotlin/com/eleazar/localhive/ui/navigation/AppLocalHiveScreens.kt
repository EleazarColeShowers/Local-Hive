package com.eleazar.localhive.ui.navigation

import androidx.compose.runtime.Composable
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
import com.eleazar.localhive.ui.feed.EditPostRoute
import com.eleazar.localhive.ui.polls.CreatePollRoute
import com.eleazar.localhive.ui.polls.PollsRoute
import com.eleazar.localhive.ui.utilities.UtilitiesRoute
import com.eleazar.localhive.ui.feed.EmergencyAlertRoute
import com.eleazar.localhive.ui.finance.AddExpenseRoute
import com.eleazar.localhive.ui.finance.CreateLevyRoute
import com.eleazar.localhive.ui.finance.FinancesRoute
import com.eleazar.localhive.ui.finance.LevyDetailRoute
import com.eleazar.localhive.ui.feed.PostDetailRoute
import com.eleazar.localhive.ui.main.MainRoute
import com.eleazar.localhive.ui.messages.ChatDetailRoute
import com.eleazar.localhive.ui.notifications.NotificationRoute
import com.eleazar.localhive.ui.settings.SettingsRoute

/** The app's destinations for [LocalHiveNavGraph], on both platforms: each one is a Koin-backed route. */
object AppLocalHiveScreens : LocalHiveScreens {

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
    override fun EstateSelection(onEstateJoined: () -> Unit) =
        EstateSelectionRoute(onEstateJoined = onEstateJoined)

    @Composable
    override fun Main(actions: MainActions) = MainRoute(actions = actions)

    @Composable
    override fun CreatePost(onNavigateBack: () -> Unit) =
        CreatePostRoute(onNavigateBack = onNavigateBack)

    @Composable
    override fun EmergencyAlert(onNavigateBack: () -> Unit) =
        EmergencyAlertRoute(onNavigateBack = onNavigateBack)

    @Composable
    override fun Polls(onBack: () -> Unit, onCreatePoll: () -> Unit) =
        PollsRoute(onBack = onBack, onCreatePoll = onCreatePoll)

    @Composable
    override fun CreatePoll(onBack: () -> Unit) = CreatePollRoute(onBack = onBack)

    @Composable
    override fun Utilities(onBack: () -> Unit) = UtilitiesRoute(onBack = onBack)

    @Composable
    override fun EditPost(postId: String, onNavigateBack: () -> Unit) =
        EditPostRoute(postId = postId, onNavigateBack = onNavigateBack)

    @Composable
    override fun PostDetail(
        postId: String,
        onNavigateBack: () -> Unit,
        onOpenProfile: (userId: String) -> Unit,
        onEditPost: (postId: String) -> Unit
    ) = PostDetailRoute(postId = postId, onNavigateBack = onNavigateBack, onOpenProfile = onOpenProfile, onEditPost = onEditPost)

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
    override fun Notifications(
        onBack: () -> Unit,
        onNavigateToEstateAdmin: (estateId: String) -> Unit,
        onNavigateToPost: (postId: String) -> Unit,
        onNavigateToLevy: (levyId: String) -> Unit
    ) = NotificationRoute(
        onBack = onBack,
        onNavigateToEstateAdmin = onNavigateToEstateAdmin,
        onNavigateToPost = onNavigateToPost,
        onNavigateToLevy = onNavigateToLevy
    )

    @Composable
    override fun Finances(
        onBack: () -> Unit,
        onLevyClick: (levyId: String) -> Unit,
        onCreateLevy: () -> Unit,
        onAddExpense: () -> Unit
    ) = FinancesRoute(onBack = onBack, onLevyClick = onLevyClick, onCreateLevy = onCreateLevy, onAddExpense = onAddExpense)

    @Composable
    override fun LevyDetail(levyId: String, onBack: () -> Unit) = LevyDetailRoute(levyId = levyId, onBack = onBack)

    @Composable
    override fun CreateLevy(onBack: () -> Unit) = CreateLevyRoute(onBack = onBack)

    @Composable
    override fun AddExpense(onBack: () -> Unit) = AddExpenseRoute(onBack = onBack)

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
