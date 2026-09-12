package com.eleazar.localhive.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eleazar.localhive.R
import com.eleazar.localhive.domain.model.AppNotification
import com.eleazar.localhive.domain.model.NotificationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateToEstateAdmin: (String) -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dark = colorResource(id = R.color.darkgray)
    val green = colorResource(id = R.color.hivegreen)

    Column(modifier = Modifier.fillMaxSize()) {
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
            if (uiState.unreadCount > 0) {
                TextButton(onClick = { viewModel.markAllAsRead() }) {
                    Text("Mark all read", color = green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        HorizontalDivider(color = dark.copy(alpha = 0.08f))

        if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    uiState.error ?: "Something went wrong",
                    color = dark.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }
            return
        }

        if (uiState.notifications.isEmpty()) {
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

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.notifications, key = { it.id }) { notification ->
                NotificationCard(
                    notification = notification,
                    dark = dark,
                    green = green,
                    onClick = {
                        if (!notification.isRead) viewModel.markAsRead(notification.id)
                        if (notification.type == NotificationType.JOIN_REQUEST && notification.estateId != null) {
                            onNavigateToEstateAdmin(notification.estateId)
                        }
                    },
                    onDismiss = { viewModel.deleteNotification(notification.id) }
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    dark: Color,
    green: Color,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val (icon, iconColor) = when (notification.type) {
        NotificationType.JOIN_REQUEST -> Icons.Default.Person to green
        NotificationType.REQUEST_APPROVED -> Icons.Default.CheckCircle to green
        NotificationType.REQUEST_DENIED -> Icons.Default.Close to Color(0xFFE53935)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color.White else colorResource(id = R.color.lightgold)
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
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
                Spacer(Modifier.height(4.dp))
                Text(
                    formatNotificationTime(notification.createdAt),
                    fontSize = 12.sp,
                    color = dark.copy(alpha = 0.4f)
                )
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

            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Close,
                    "Dismiss",
                    tint = dark.copy(alpha = 0.2f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

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
