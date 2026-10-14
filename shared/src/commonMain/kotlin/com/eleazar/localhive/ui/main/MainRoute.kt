package com.eleazar.localhive.ui.main

import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.domain.model.Chat
import com.eleazar.localhive.ui.directory.DirectoryRoute
import com.eleazar.localhive.ui.feed.FeedRoute
import com.eleazar.localhive.ui.messages.MessagesRoute
import com.eleazar.localhive.ui.messages.MessagesViewModel
import com.eleazar.localhive.ui.navigation.MainActions
import com.eleazar.localhive.ui.navigation.MainTabsHost
import com.eleazar.localhive.ui.navigation.Screen
import com.eleazar.localhive.ui.notifications.NotificationViewModel
import com.eleazar.localhive.ui.profile.ProfileRoute

/** Navigation lives in the shared graph; this only supplies ViewModel state and the tab contents. */
@Composable
fun MainRoute(actions: MainActions) {
    val membershipViewModel: MembershipViewModel = koinViewModel()
    val removedFromEstate by membershipViewModel.removed.collectAsStateWithLifecycle()
    LaunchedEffect(removedFromEstate) {
        if (removedFromEstate) actions.onRemovedFromEstate()
    }

    val notificationViewModel: NotificationViewModel = koinViewModel()
    val notificationUiState by notificationViewModel.uiState.collectAsStateWithLifecycle()

    // Hoisted so the tab badge and the Messages tab share one chats listener.
    val messagesViewModel: MessagesViewModel = koinViewModel()
    val messagesUiState by messagesViewModel.uiState.collectAsStateWithLifecycle()

    val tabs = remember(messagesUiState.totalUnread) {
        listOf(
            MainTab(Screen.Feed.route, "Feed", Icons.Outlined.Home, Icons.Filled.Home),
            MainTab(Screen.Directory.route, "Directory", Icons.Outlined.Search, Icons.Filled.Search),
            MainTab(
                Screen.Messages.route, "Messages", Icons.Outlined.Email, Icons.Filled.Email,
                badgeCount = messagesUiState.totalUnread
            ),
            MainTab(
                Screen.Profile.route, "Profile", Icons.Outlined.Person, Icons.Filled.Person
            )
        )
    }

    val colors = MainScreenColors(
        accent = MaterialTheme.colorScheme.primary,
        chrome = MaterialTheme.colorScheme.onSurface
    )

    MainTabsHost(
        tabs = tabs,
        unreadNotifications = notificationUiState.unreadCount,
        colors = colors,
        onNotificationsClick = actions.onNotificationsClick,
        onCreatePostClick = actions.onCreatePost
    ) { route, selectTab ->
        when (route) {
            Screen.Feed.route -> FeedRoute(
                onPostClick = actions.onPostClick,
                onCreatePost = actions.onCreatePost,
                onUserClick = actions.onUserClick,
                onEmergency = actions.onEmergency,
                onEditPost = actions.onEditPost,
                onUtilities = actions.onUtilities,
                onPolls = actions.onPolls,
                onDues = actions.onFinances
            )
            Screen.Directory.route -> DirectoryRoute(
                onUserClick = actions.onUserClick,
                onMessageUser = { userId ->
                    val currentUserId = messagesUiState.currentUserId
                    if (currentUserId != null) {
                        actions.onChatClick(Chat.idFor(currentUserId, userId))
                    } else {
                        actions.onUserClick(userId)
                    }
                }
            )
            Screen.Messages.route -> MessagesRoute(
                onChatClick = actions.onChatClick,
                onNewMessage = { selectTab(Screen.Directory.route) },
                viewModel = messagesViewModel
            )
            Screen.Profile.route -> ProfileRoute(
                onSettings = actions.onSettings,
                onAdminPanel = actions.onAdminPanel,
                onFinances = actions.onFinances,
                onPolls = actions.onPolls,
                onUtilities = actions.onUtilities
            )
        }
    }
}
