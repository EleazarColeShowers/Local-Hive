package com.eleazar.localhive.ui.messages

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MessagesRoute(
    onChatClick: (String) -> Unit = {},
    onNewMessage: () -> Unit = {},
    viewModel: MessagesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MessagesScreen(
        chats = uiState.chats,
        peers = uiState.peers,
        currentUserId = uiState.currentUserId,
        isLoading = uiState.isLoadingChats,
        error = uiState.chatsError,
        onChatClick = onChatClick,
        onNewMessage = onNewMessage,
        onRetry = viewModel::retryChats,
        colors = MessagesColors(hiveGreen = MaterialTheme.colorScheme.primary)
    )
}
