package com.eleazar.localhive.ui.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FeedRoute(
    onPostClick: (String) -> Unit = {},
    onCreatePost: () -> Unit = {},
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FeedScreen(
        uiState = uiState,
        onPostClick = onPostClick,
        onCreatePost = onCreatePost,
        onLike = viewModel::toggleLike,
        onLoadMore = viewModel::loadMorePosts
    )
}
