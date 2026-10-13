package com.eleazar.localhive.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.Chat
import com.eleazar.localhive.ui.components.Avatar
import com.eleazar.localhive.ui.components.EmptyState
import com.eleazar.localhive.ui.components.ScreenGutter
import com.eleazar.localhive.ui.components.ScreenHeader
import com.eleazar.localhive.ui.theme.HiveGreen
import com.eleazar.localhive.ui.util.relativeTime

/** What the UI knows about the other person in a conversation. */
@Immutable
data class ChatPeer(
    val name: String? = null,
    val photoUrl: String? = null
)

data class MessagesColors(
    val hiveGreen: Color = HiveGreen
)

@Composable
fun rememberMessagesColors(): MessagesColors {
    val colorScheme = MaterialTheme.colorScheme
    return MessagesColors(
        hiveGreen = colorScheme.primary
    )
}

private const val AVATAR_SIZE = 52
private val RowPadding = 12.dp

/**
 * Bare UI for the messages list screen.
 * Free of ViewModel, Android Context, or navigation side effects.
 */
@Composable
fun MessagesScreen(
    chats: List<Chat>,
    peers: Map<String, ChatPeer>,
    currentUserId: String?,
    isLoading: Boolean,
    error: String?,
    onChatClick: (chatId: String) -> Unit,
    onNewMessage: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    colors: MessagesColors = rememberMessagesColors(),
    formatTime: (Long) -> String = { relativeTime(it) }
) {
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Messages",
            subtitle = if (chats.isNotEmpty()) "Chats with your neighbours" else null,
            action = {
                IconButton(onClick = onNewMessage) {
                    Icon(Icons.Outlined.Edit, contentDescription = "New message", tint = colors.hiveGreen)
                }
            }
        )

        when {
            isLoading && chats.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.hiveGreen)
            }
            error != null && chats.isEmpty() -> EmptyState(
                icon = Icons.Outlined.CloudOff,
                title = "Couldn't load your messages",
                message = "Check your connection and try again.",
                actionLabel = "Try again",
                onAction = onRetry
            )
            chats.isEmpty() -> EmptyState(
                icon = Icons.AutoMirrored.Outlined.Chat,
                title = "No conversations yet",
                message = "Say hello to someone in your estate. Your chats will show up here.",
                actionLabel = "Find neighbours",
                onAction = onNewMessage
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(chats, key = { it.id }) { chat ->
                    val otherId = chat.otherParticipant(currentUserId)
                    ChatRow(
                        chat = chat,
                        // A cached profile would keep showing a deleted account's old name and photo.
                        peer = peers[otherId].takeUnless { otherId in chat.deletedParticipants },
                        fallbackName = chat.participantNames[otherId],
                        currentUserId = currentUserId ?: "",
                        colors = colors,
                        formatTime = formatTime,
                        onClick = { onChatClick(chat.id) }
                    )
                    // Inset to the text column so dividers line up under names, not avatars.
                    HorizontalDivider(
                        modifier = Modifier.padding(start = ScreenGutter + AVATAR_SIZE.dp + RowPadding),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatRow(
    chat: Chat,
    peer: ChatPeer?,
    fallbackName: String?,
    currentUserId: String,
    colors: MessagesColors,
    formatTime: (Long) -> String,
    onClick: () -> Unit
) {
    val name = peer?.name ?: fallbackName?.takeIf { it.isNotBlank() } ?: "Neighbour"
    val unread = chat.unreadFor(currentUserId)
    val hasUnread = unread > 0
    val muted = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
    val preview = if (chat.lastMessageSenderId == currentUserId) "You: ${chat.lastMessage}" else chat.lastMessage

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = ScreenGutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(imageUrl = peer?.photoUrl, name = name, size = AVATAR_SIZE.dp)
        Spacer(Modifier.width(RowPadding))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    modifier = Modifier.weight(1f),
                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatTime(chat.lastMessageTime),
                    fontSize = 12.sp,
                    fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (hasUnread) colors.hiveGreen else muted
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = preview,
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                    color = if (hasUnread) MaterialTheme.colorScheme.onBackground else muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (hasUnread) {
                    Spacer(Modifier.width(8.dp))
                    Badge(containerColor = colors.hiveGreen, contentColor = Color.White) {
                        Text(if (unread > 99) "99+" else "$unread", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
