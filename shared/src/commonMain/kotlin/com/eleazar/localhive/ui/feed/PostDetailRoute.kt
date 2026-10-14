package com.eleazar.localhive.ui.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PostDetailRoute(
    postId: String,
    onNavigateBack: () -> Unit,
    onOpenProfile: (userId: String) -> Unit = {},
    onEditPost: (postId: String) -> Unit = {},
    viewModel: FeedViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(postId) {
        viewModel.loadPostDetail(postId)
    }

    LaunchedEffect(uiState.postDeleted) {
        if (uiState.postDeleted) {
            viewModel.consumePostDeleted()
            onNavigateBack()
        }
    }

    PostDetailScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onLike = viewModel::toggleLike,
        onAddComment = { content, mentions -> viewModel.addComment(postId, content, mentions) },
        onUserClick = onOpenProfile,
        onUpdateStatus = { status -> viewModel.updateIncidentStatus(postId, status) },
        onEditPost = { onEditPost(postId) },
        onDeletePost = { reason -> viewModel.deletePost(postId, reason) },
        onRespond = { viewModel.respondToEmergency(postId) },
        onMarkSafe = { viewModel.markSafe(postId) }
    )
}
