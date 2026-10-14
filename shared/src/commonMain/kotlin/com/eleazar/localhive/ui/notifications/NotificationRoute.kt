package com.eleazar.localhive.ui.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.domain.model.NotificationType
import com.eleazar.localhive.ui.util.formatDate
import com.eleazar.localhive.ui.util.nowMillis

@Composable
fun NotificationRoute(
    onBack: () -> Unit,
    onNavigateToEstateAdmin: (String) -> Unit,
    onNavigateToPost: (String) -> Unit = {},
    onNavigateToLevy: (String) -> Unit = {},
    viewModel: NotificationViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val colors = rememberNotificationColors()

    NotificationScreen(
        notifications = uiState.notifications,
        unreadCount = uiState.unreadCount,
        error = uiState.error,
        colors = colors,
        onBack = onBack,
        onMarkAllAsRead = { viewModel.markAllAsRead() },
        onNotificationClick = { notification ->
            if (!notification.isRead) viewModel.markAsRead(notification.id)
            val estateId = notification.estateId
            val postId = notification.postId
            if (notification.type == NotificationType.JOIN_REQUEST && estateId != null) {
                onNavigateToEstateAdmin(estateId)
            } else if (notification.type == NotificationType.MENTION && postId != null) {
                onNavigateToPost(postId)
            } else if (notification.type.name.startsWith("PAYMENT_")) {
                notification.levyId?.let(onNavigateToLevy)
            }
        },
        onNotificationDismiss = { notificationId ->
            viewModel.deleteNotification(notificationId)
        },
        formatTime = ::formatNotificationTime
    )
}

/** Backwards-compatibility alias for callers expecting NotificationsRoute or NotificationsScreen. */
@Composable
fun NotificationsRoute(
    onBack: () -> Unit,
    onNavigateToEstateAdmin: (String) -> Unit,
    viewModel: NotificationViewModel = koinViewModel()
) = NotificationRoute(
    onBack = onBack,
    onNavigateToEstateAdmin = onNavigateToEstateAdmin,
    viewModel = viewModel
)

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateToEstateAdmin: (String) -> Unit,
    viewModel: NotificationViewModel = koinViewModel()
) = NotificationRoute(
    onBack = onBack,
    onNavigateToEstateAdmin = onNavigateToEstateAdmin,
    viewModel = viewModel
)

private fun formatNotificationTime(millis: Long): String {
    val diff = nowMillis() - millis
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> formatDate(millis, "MMM d")
    }
}
