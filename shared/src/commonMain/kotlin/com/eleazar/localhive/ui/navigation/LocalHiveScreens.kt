package com.eleazar.localhive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * The content of each destination. The graph decides where every callback leads; this decides
 * how a screen gets its state. [AppLocalHiveScreens] is the real one, used on both platforms.
 */
@Stable
interface LocalHiveScreens {
    @Composable
    fun Splash(
        onNavigateToOnboarding: () -> Unit,
        onNavigateToEstateSelection: () -> Unit,
        onNavigateToMain: () -> Unit
    )

    @Composable
    fun SignUp(onNavigateToLogin: () -> Unit, onSignupSuccess: () -> Unit)

    @Composable
    fun Login(onNavigateToSignup: () -> Unit, onLoginSuccess: (hasEstate: Boolean) -> Unit)

    @Composable
    fun ProfileSetup(onProfileComplete: () -> Unit)

    @Composable
    fun EstateSelection(onEstateJoined: () -> Unit)

    /** The tabbed home screen. Use [MainTabsHost] for the tab navigation itself. */
    @Composable
    fun Main(actions: MainActions)

    @Composable
    fun CreatePost(onNavigateBack: () -> Unit)

    /** The SOS screen. */
    @Composable
    fun EmergencyAlert(onNavigateBack: () -> Unit)

    /** The composer, opened on one of the user's own posts. */
    @Composable
    fun EditPost(postId: String, onNavigateBack: () -> Unit)

    @Composable
    fun PostDetail(
        postId: String,
        onNavigateBack: () -> Unit,
        onOpenProfile: (userId: String) -> Unit,
        onEditPost: (postId: String) -> Unit
    )

    @Composable
    fun ChatDetail(chatId: String, onNavigateBack: () -> Unit, onOpenProfile: (userId: String) -> Unit)

    @Composable
    fun JoinEstate(initialCode: String, onSuccess: () -> Unit, onBack: () -> Unit)

    @Composable
    fun UserProfile(userId: String, onNavigateBack: () -> Unit, onMessageUser: (chatId: String) -> Unit)

    @Composable
    fun EstateAdmin(estateId: String, onBack: () -> Unit)

    @Composable
    fun Notifications(
        onBack: () -> Unit,
        onNavigateToEstateAdmin: (estateId: String) -> Unit,
        onNavigateToPost: (postId: String) -> Unit,
        onNavigateToLevy: (levyId: String) -> Unit
    )

    /** The estate's dues and expenses. */
    @Composable
    fun Finances(
        onBack: () -> Unit,
        onLevyClick: (levyId: String) -> Unit,
        onCreateLevy: () -> Unit,
        onAddExpense: () -> Unit
    )

    @Composable
    fun LevyDetail(levyId: String, onBack: () -> Unit)

    @Composable
    fun CreateLevy(onBack: () -> Unit)

    @Composable
    fun AddExpense(onBack: () -> Unit)

    @Composable
    fun Settings(
        onBack: () -> Unit,
        onLoggedOut: () -> Unit,
        onLeftEstate: () -> Unit,
        onBlockedUsers: () -> Unit
    )

    @Composable
    fun BlockedUsers(onBack: () -> Unit)

    @Composable
    fun Polls(onBack: () -> Unit, onCreatePoll: () -> Unit)

    @Composable
    fun CreatePoll(onBack: () -> Unit)

    /** "Is there light?": power and water status. */
    @Composable
    fun Utilities(onBack: () -> Unit)
}

/** Where the Main screen and its tabs can lead in the app-level graph. */
@Stable
class MainActions(
    val onPostClick: (postId: String) -> Unit,
    val onUserClick: (userId: String) -> Unit,
    val onChatClick: (chatId: String) -> Unit,
    val onNotificationsClick: () -> Unit,
    val onCreatePost: () -> Unit,
    val onEmergency: () -> Unit,
    val onEditPost: (postId: String) -> Unit,
    val onPolls: () -> Unit,
    val onUtilities: () -> Unit,
    val onSettings: () -> Unit,
    val onAdminPanel: (estateId: String) -> Unit,
    val onFinances: () -> Unit,
    /** The user was removed by an admin; ignored unless Main is the current destination. */
    val onRemovedFromEstate: () -> Unit
)
