package com.eleazar.localhive.ui.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.ui.report.rememberReporter

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
        onAddComment = { content, mentions, parentId -> viewModel.addComment(postId, content, mentions, parentId) },
        onUserClick = onOpenProfile,
        onUpdateStatus = { status -> viewModel.updateIncidentStatus(postId, status) },
        onEditPost = { onEditPost(postId) },
        onDeletePost = { reason -> viewModel.deletePost(postId, reason) },
        onRespond = { viewModel.respondToEmergency(postId) },
        onMarkSafe = { viewModel.markSafe(postId) },
        onMarkFalseAlarm = { viewModel.markFalseAlarm(postId) },
        onClearFalseAlarm = { viewModel.clearFalseAlarm(postId) },
        onDeleteComment = { commentId -> viewModel.deleteComment(postId, commentId) },
        onEditComment = { commentId, content, mentions -> viewModel.editComment(postId, commentId, content, mentions) },
        onReport = rememberReporter()
    )
}
