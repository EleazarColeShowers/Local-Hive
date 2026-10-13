package com.eleazar.localhive.ui.messages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.colorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.R

@Composable
fun MessagesRoute(
    onChatClick: (String) -> Unit = {},
    onNewMessage: () -> Unit = {},
    viewModel: MessagesViewModel = hiltViewModel()
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
        colors = MessagesColors(hiveGreen = colorResource(id = R.color.hivegreen))
    )
}
