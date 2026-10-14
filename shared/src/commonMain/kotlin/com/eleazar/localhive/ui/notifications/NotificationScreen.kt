package com.eleazar.localhive.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.AppNotification
import com.eleazar.localhive.domain.model.NotificationType

data class NotificationColors(
    val darkGray: Color = Color(0xFF2C2C2C),
    val hiveGreen: Color = Color(0xFF4CAF50),
    val lightGold: Color = Color(0xFFFFF9C4),
    val cardBackground: Color = Color.White
)

@Composable
fun rememberNotificationColors(): NotificationColors {
    val colorScheme = androidx.compose.material3.MaterialTheme.colorScheme
    return NotificationColors(
        darkGray = colorScheme.onBackground,
        hiveGreen = colorScheme.primary,
        lightGold = colorScheme.surfaceVariant,
        cardBackground = colorScheme.surface
    )
}

/**
 * Bare notification UI. No ViewModel, Android Context, or navigation side effects in here.
 */
@Composable
fun NotificationScreen(
    notifications: List<AppNotification>,
    unreadCount: Int,
    error: String?,
    colors: NotificationColors = rememberNotificationColors(),
    onBack: () -> Unit,
    onMarkAllAsRead: () -> Unit,
    onNotificationClick: (AppNotification) -> Unit,
    onNotificationDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
    formatTime: (Long) -> String = { if (it > 0) "$it" else "" }
) {
    val dark = colors.darkGray
    val green = colors.hiveGreen

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .padding(top = 8.dp, start = 4.dp, end = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = dark)
            }
            Text(
                "Notifications",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = dark,
                modifier = Modifier.weight(1f)
            )
            if (unreadCount > 0) {
                TextButton(onClick = onMarkAllAsRead) {
                    Text("Mark all read", color = green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        HorizontalDivider(color = dark.copy(alpha = 0.08f))

        if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    error,
                    color = dark.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }
            return
        }

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Notifications,
                        null,
                        tint = dark.copy(alpha = 0.2f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("No notifications yet", color = dark.copy(alpha = 0.4f), fontSize = 15.sp)
                }
            }
            return
        }

        LazyColumn {
            items(notifications, key = { it.id }) { notification ->
                NotificationCard(
                    notification = notification,
                    colors = colors,
                    formattedTime = formatTime(notification.createdAt),
                    onClick = { onNotificationClick(notification) },
                    onDismiss = { onNotificationDismiss(notification.id) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    colors: NotificationColors,
    formattedTime: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val dark = colors.darkGray
    val green = colors.hiveGreen
    val (icon, iconColor) = when (notification.type) {
        NotificationType.JOIN_REQUEST -> Icons.Default.Person to green
        NotificationType.REQUEST_APPROVED -> Icons.Default.CheckCircle to green
        NotificationType.REQUEST_DENIED -> Icons.Default.Close to Color(0xFFE53935)
        NotificationType.MEMBER_REMOVED -> Icons.Default.Close to Color(0xFFE53935)
        NotificationType.MENTION -> Icons.Default.AlternateEmail to green
        NotificationType.PAYMENT_SUBMITTED -> Icons.Outlined.ReceiptLong to green
        NotificationType.PAYMENT_CONFIRMED -> Icons.Default.CheckCircle to green
        NotificationType.PAYMENT_REJECTED -> Icons.Default.Close to Color(0xFFE53935)
        NotificationType.POST_REMOVED -> Icons.Outlined.Delete to Color(0xFFE53935)
    }

    // Unread rows get a faint brand tint, as most apps' notification lists do.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (notification.isRead) Color.Transparent else green.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(notification.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = dark)
            Spacer(Modifier.height(2.dp))
            Text(
                notification.message,
                fontSize = 13.sp,
                color = dark.copy(alpha = 0.6f),
                lineHeight = 18.sp
            )
            if (formattedTime.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    formattedTime,
                    fontSize = 12.sp,
                    color = dark.copy(alpha = 0.4f)
                )
            }
        }

        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(green)
            )
        }

        IconButton(onClick = onDismiss) {
            Icon(
                Icons.Default.Close,
                "Dismiss",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
