package com.eleazar.localhive.ui.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.colorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.R
import com.eleazar.localhive.domain.model.NotificationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationRoute(
    onBack: () -> Unit,
    onNavigateToEstateAdmin: (String) -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val colors = NotificationColors(
        darkGray = colorResource(id = R.color.darkgray),
        hiveGreen = colorResource(id = R.color.hivegreen),
        lightGold = colorResource(id = R.color.lightgold)
    )

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
            if (notification.type == NotificationType.JOIN_REQUEST && estateId != null) {
                onNavigateToEstateAdmin(estateId)
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
    viewModel: NotificationViewModel = hiltViewModel()
) = NotificationRoute(
    onBack = onBack,
    onNavigateToEstateAdmin = onNavigateToEstateAdmin,
    viewModel = viewModel
)

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateToEstateAdmin: (String) -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) = NotificationRoute(
    onBack = onBack,
    onNavigateToEstateAdmin = onNavigateToEstateAdmin,
    viewModel = viewModel
)

private fun formatNotificationTime(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
    }
}
