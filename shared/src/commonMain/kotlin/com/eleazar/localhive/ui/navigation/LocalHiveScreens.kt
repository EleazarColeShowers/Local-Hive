package com.eleazar.localhive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * The content of each destination, supplied by the platform. The shared graph decides where
 * every callback leads; the platform only decides how a screen gets its state (Hilt
 * ViewModels on Android, whatever iOS ends up using).
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
    fun DetailsSetup(onDetailsComplete: () -> Unit)

    @Composable
    fun EstateSelection(onEstateJoined: () -> Unit)

    /** The tabbed home screen. Use [MainTabsHost] for the tab navigation itself. */
    @Composable
    fun Main(actions: MainActions)

    @Composable
    fun CreatePost(onNavigateBack: () -> Unit)

    @Composable
    fun PostDetail(postId: String, onNavigateBack: () -> Unit)

    @Composable
    fun ChatDetail(chatId: String, onNavigateBack: () -> Unit, onOpenProfile: (userId: String) -> Unit)

    @Composable
    fun JoinEstate(initialCode: String, onSuccess: () -> Unit, onBack: () -> Unit)

    @Composable
    fun UserProfile(userId: String, onNavigateBack: () -> Unit, onMessageUser: (chatId: String) -> Unit)

    @Composable
    fun EstateAdmin(estateId: String, onBack: () -> Unit)

    @Composable
    fun Notifications(onBack: () -> Unit, onNavigateToEstateAdmin: (estateId: String) -> Unit)

    @Composable
    fun Settings(
        onBack: () -> Unit,
        onLoggedOut: () -> Unit,
        onLeftEstate: () -> Unit,
        onBlockedUsers: () -> Unit
    )

    @Composable
    fun BlockedUsers(onBack: () -> Unit)
}

/** Where the Main screen and its tabs can lead in the app-level graph. */
@Stable
class MainActions(
    val onPostClick: (postId: String) -> Unit,
    val onUserClick: (userId: String) -> Unit,
    val onChatClick: (chatId: String) -> Unit,
    val onNotificationsClick: () -> Unit,
    val onCreatePost: () -> Unit,
    val onSettings: () -> Unit,
    val onAdminPanel: (estateId: String) -> Unit,
    val onLeftEstate: () -> Unit,
    /** The user was removed by an admin; ignored unless Main is the current destination. */
    val onRemovedFromEstate: () -> Unit
)
