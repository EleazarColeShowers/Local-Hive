package com.eleazar.localhive.ui.messages

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.eleazar.localhive.ui.platform.rememberPlatformActions
import com.eleazar.localhive.ui.util.nowMillis
import kotlinx.coroutines.delay
import kotlin.math.abs
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.domain.model.Chat
import com.eleazar.localhive.ui.blocking.BlockingViewModel
import com.eleazar.localhive.domain.model.Report
import com.eleazar.localhive.domain.model.ReportReason
import com.eleazar.localhive.domain.model.ReportSubject
import com.eleazar.localhive.ui.report.rememberReporter

/** A typing mark further than this from the reader's clock is left over, or the clocks are far apart. */
private const val STALE_TYPING_MILLIS = 30_000L

@Composable
fun ChatDetailRoute(
    chatId: String,
    onNavigateBack: () -> Unit,
    onOpenProfile: (userId: String) -> Unit = {},
    viewModel: MessagesViewModel = koinViewModel(),
    blockingViewModel: BlockingViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val blocking by blockingViewModel.uiState.collectAsStateWithLifecycle()

    val platform = rememberPlatformActions()
    val report = rememberReporter()
    LaunchedEffect(chatId) {
        viewModel.openChat(chatId)
    }

    val chat = uiState.chats.find { it.id == chatId }

    // Re-runs as messages arrive, so a conversation being read never re-badges itself.
    LaunchedEffect(chatId, uiState.messages.firstOrNull()?.id, chat?.unreadFor(uiState.currentUserId)) {
        viewModel.markChatRead(chatId)
        // What's in the tray for this chat has now been read here.
        platform.clearChatNotifications(chatId)
    }

    val otherUserId = chat?.otherParticipant(uiState.currentUserId)
        ?: Chat.participantsOf(chatId).firstOrNull { it != uiState.currentUserId }
        ?: ""
    val otherDeleted = chat?.deletedParticipants?.contains(otherUserId) == true
    val peer = uiState.peers[otherUserId].takeUnless { otherDeleted }
    val otherName = peer?.name
        ?: chat?.participantNames?.get(otherUserId)?.takeIf { it.isNotBlank() }
        ?: "Neighbour"

    // Leaving with something half-typed shouldn't leave "typing…" showing on their side.
    DisposableEffect(chatId) {
        onDispose { viewModel.stopTyping(chatId) }
    }

    // Shown for a little longer than the other phone takes to renew it, and only for a fresh mark,
    // so one left behind by a phone that went to sleep mid-message doesn't linger.
    val otherTypingAt = chat?.typing?.get(otherUserId) ?: 0L
    var otherTyping by remember { mutableStateOf(false) }
    LaunchedEffect(otherTypingAt) {
        otherTyping = otherTypingAt > 0L && abs(nowMillis() - otherTypingAt) < STALE_TYPING_MILLIS
        if (otherTyping) {
            delay(TYPING_REFRESH_MILLIS + 2_000L)
            otherTyping = false
        }
    }

    ChatDetailScreen(
        otherName = otherName,
        otherPhotoUrl = peer?.photoUrl,
        messages = uiState.messages,
        currentUserId = uiState.currentUserId,
        isLoading = uiState.isLoadingMessages,
        isLoadingMoreMessages = uiState.isLoadingMoreMessages,
        unsentDraft = uiState.unsentDraft,
        onNavigateBack = onNavigateBack,
        onSendMessage = { content, replyTo -> viewModel.sendMessage(chatId, content, replyTo) },
        onLoadOlderMessages = { viewModel.loadOlderMessages(chatId) },
        onUnsentDraftShown = viewModel::consumeUnsentDraft,
        unsentReason = uiState.unsentReason,
        isBlocked = blocking.isBlocked(otherUserId),
        otherDeleted = otherDeleted,
        onBlock = otherUserId.takeIf { it.isNotBlank() }?.let { id ->
            { blockingViewModel.block(id, otherName, peer?.photoUrl) }
        },
        onUnblock = { blockingViewModel.unblock(otherUserId) },
        onReport = otherUserId.takeIf { it.isNotBlank() }?.let { id ->
            { reason: ReportReason, details: String ->
                report(
                    Report(
                        subject = ReportSubject.CHAT, reason = reason, details = details,
                        targetId = chatId, targetUserId = id,
                        // Their latest messages, in case they delete them.
                        excerpt = uiState.messages.filter { it.senderId == id }.take(10).reversed()
                            .joinToString("\n") { it.content }
                    )
                )
            }
        },
        onOpenProfile = otherUserId.takeIf { it.isNotBlank() }?.let { id -> { onOpenProfile(id) } },
        otherTyping = otherTyping && !otherDeleted && !blocking.isBlocked(otherUserId),
        seenUpTo = chat?.readUpTo?.get(otherUserId) ?: 0L,
        onDraftChanged = { text -> viewModel.onDraftChanged(chatId, text) },
        colors = ChatDetailColors(hiveGreen = MaterialTheme.colorScheme.primary)
    )
}
