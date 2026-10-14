package com.eleazar.localhive.ui.messages

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.domain.model.Chat
import com.eleazar.localhive.ui.blocking.BlockingViewModel

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

    LaunchedEffect(chatId) {
        viewModel.openChat(chatId)
    }

    val chat = uiState.chats.find { it.id == chatId }

    // Re-runs as messages arrive, so a conversation being read never re-badges itself.
    LaunchedEffect(chatId, uiState.messages.firstOrNull()?.id, chat?.unreadFor(uiState.currentUserId)) {
        viewModel.markChatRead(chatId)
    }

    val otherUserId = chat?.otherParticipant(uiState.currentUserId)
        ?: Chat.participantsOf(chatId).firstOrNull { it != uiState.currentUserId }
        ?: ""
    val otherDeleted = chat?.deletedParticipants?.contains(otherUserId) == true
    val peer = uiState.peers[otherUserId].takeUnless { otherDeleted }
    val otherName = peer?.name
        ?: chat?.participantNames?.get(otherUserId)?.takeIf { it.isNotBlank() }
        ?: "Neighbour"

    ChatDetailScreen(
        otherName = otherName,
        otherPhotoUrl = peer?.photoUrl,
        messages = uiState.messages,
        currentUserId = uiState.currentUserId,
        isLoading = uiState.isLoadingMessages,
        isLoadingMoreMessages = uiState.isLoadingMoreMessages,
        unsentDraft = uiState.unsentDraft,
        onNavigateBack = onNavigateBack,
        onSendMessage = { content -> viewModel.sendMessage(chatId, content) },
        onLoadOlderMessages = { viewModel.loadOlderMessages(chatId) },
        onUnsentDraftShown = viewModel::consumeUnsentDraft,
        unsentReason = uiState.unsentReason,
        isBlocked = blocking.isBlocked(otherUserId),
        otherDeleted = otherDeleted,
        onBlock = otherUserId.takeIf { it.isNotBlank() }?.let { id ->
            { blockingViewModel.block(id, otherName, peer?.photoUrl) }
        },
        onUnblock = { blockingViewModel.unblock(otherUserId) },
        onOpenProfile = otherUserId.takeIf { it.isNotBlank() }?.let { id -> { onOpenProfile(id) } },
        colors = ChatDetailColors(hiveGreen = MaterialTheme.colorScheme.primary)
    )
}
