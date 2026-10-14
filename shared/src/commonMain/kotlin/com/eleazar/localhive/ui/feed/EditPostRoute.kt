package com.eleazar.localhive.ui.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/** The composer, opened on one of the user's own posts. */
@Composable
fun EditPostRoute(
    postId: String,
    onNavigateBack: () -> Unit,
    viewModel: FeedViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(postId) { viewModel.observePost(postId) }

    LaunchedEffect(uiState.postCreated, uiState.postMissing) {
        if (uiState.postCreated) viewModel.consumePostCreated()
        // Saved, or deleted while it was open: either way there's nothing left to edit.
        if (uiState.postCreated || uiState.postMissing) onNavigateBack()
    }

    val post = uiState.selectedPost?.takeIf { it.id == postId }
    if (post == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // Keyed so the fields start from this post's text, not whatever was loaded before it.
    key(post.id) {
        CreatePostScreen(
            uiState = uiState,
            pickedMedia = post.imageUrl ?: post.videoThumbnailUrl,
            pickedIsVideo = post.videoUrl != null,
            onPickMedia = {},
            onRemoveMedia = {},
            onPost = { content, _, mentions, location -> viewModel.editPost(postId, content, mentions, location) },
            onNavigateBack = onNavigateBack,
            initialContent = post.content,
            editing = true,
            initialCategoryKey = post.category.ifBlank { null },
            initialLocation = post.location,
            initialMentions = post.mentions
        )
    }
}
