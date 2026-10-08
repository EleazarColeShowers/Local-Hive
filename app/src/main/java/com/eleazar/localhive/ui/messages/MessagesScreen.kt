package com.eleazar.localhive.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.R
import com.eleazar.localhive.domain.model.Chat
import com.eleazar.localhive.ui.components.Avatar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessagesScreen(
    onChatClick: (String) -> Unit = {},
    viewModel: MessagesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Messages", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 12.dp))

        if (uiState.chats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No messages yet", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text("Start chatting with a neighbor from the Directory", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.chats, key = { it.id }) { chat ->
                    ChatItem(
                        chat = chat,
                        currentUserId = uiState.currentUserId ?: "",
                        onClick = { onChatClick(chat.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatItem(chat: Chat, currentUserId: String, onClick: () -> Unit) {
    val otherUserId = chat.participants.firstOrNull { it != currentUserId } ?: ""
    val otherName = chat.participantNames[otherUserId] ?: "Neighbor"
    val unread = chat.unreadFor(currentUserId)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(imageUrl = null, name = otherName, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(otherName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (chat.lastMessageTime > 0) {
                        Text(
                            formatChatTime(chat.lastMessageTime),
                            fontSize = 11.sp,
                            color = if (unread > 0) colorResource(id = R.color.hivegreen)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chat.lastMessage.isNotBlank()) {
                        Text(
                            chat.lastMessage,
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            fontWeight = if (unread > 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (unread > 0) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    if (unread > 0) {
                        Spacer(Modifier.width(8.dp))
                        Badge(containerColor = colorResource(id = R.color.hivegreen)) {
                            Text(if (unread > 99) "99+" else "$unread")
                        }
                    }
                }
            }
        }
    }
}

private fun formatChatTime(millis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - millis
    return when {
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
    }
}
