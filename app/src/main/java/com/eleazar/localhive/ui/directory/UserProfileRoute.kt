package com.eleazar.localhive.ui.directory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.ui.blocking.BlockingViewModel

@Composable
fun UserProfileRoute(
    userId: String,
    onNavigateBack: () -> Unit,
    onMessageUser: (chatId: String) -> Unit,
    viewModel: DirectoryViewModel = hiltViewModel(),
    blockingViewModel: BlockingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val blocking by blockingViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        viewModel.loadUserProfile(userId)
    }

    UserProfileScreen(
        userId = userId,
        onNavigateBack = onNavigateBack,
        onMessageUser = onMessageUser,
        directoryState = uiState,
        isBlocked = blocking.isBlocked(userId),
        onBlock = {
            val user = uiState.selectedUser?.takeIf { it.id == userId }
            blockingViewModel.block(
                targetId = userId,
                name = user?.displayName?.takeIf { it.isNotBlank() } ?: user?.username.orEmpty(),
                photoUrl = user?.profileImageUrl
            )
        },
        onUnblock = { blockingViewModel.unblock(userId) }
    )
}
