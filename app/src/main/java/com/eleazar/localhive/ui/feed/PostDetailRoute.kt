package com.eleazar.localhive.ui.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PostDetailRoute(
    postId: String,
    onNavigateBack: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(postId) {
        viewModel.loadPostDetail(postId)
    }

    PostDetailScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onLike = viewModel::toggleLike,
        onAddComment = { content -> viewModel.addComment(postId, content) }
    )
}
