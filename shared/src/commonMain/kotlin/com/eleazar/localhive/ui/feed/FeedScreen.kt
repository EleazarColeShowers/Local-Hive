package com.eleazar.localhive.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eleazar.localhive.ui.components.ComposePrompt
import com.eleazar.localhive.ui.components.EmptyState
import com.eleazar.localhive.ui.components.PostCard
import com.eleazar.localhive.ui.components.ScreenGutter
import com.eleazar.localhive.ui.components.ScreenHeader
import com.eleazar.localhive.domain.model.PostCategory
import com.eleazar.localhive.ui.util.greeting
import com.eleazar.localhive.ui.util.nowMillis

/** How long an announcement stays pinned to the top of the feed. */
private const val PIN_WINDOW_MILLIS = 7 * 24 * 60 * 60 * 1000L

@Composable
fun FeedScreen(
    uiState: FeedUiState,
    onPostClick: (String) -> Unit,
    onCreatePost: () -> Unit,
    onLike: (postId: String) -> Unit,
    onLoadMore: () -> Unit,
) {
    val loadMore by rememberUpdatedState(onLoadMore)
    val listState = rememberLazyListState()
    val accent = MaterialTheme.colorScheme.primary
    // The effect below outlives recompositions, so read posts through this rather than a stale capture.
    val posts by rememberUpdatedState(uiState.posts)

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { lastVisible ->
                if (posts.isNotEmpty() && lastVisible >= listState.layoutInfo.totalItemsCount - 3) {
                    loadMore()
                }
            }
    }

    val firstName = uiState.currentUserName?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() }
    val estateLine = uiState.estateName?.let { name ->
        when (val others = (uiState.memberCount - 1).coerceAtLeast(0)) {
            0 -> name
            1 -> "$name · 1 neighbour"
            else -> "$name · $others neighbours"
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            ScreenHeader(
                title = if (firstName != null) "${greeting()}, $firstName" else greeting(),
                subtitle = estateLine
            )
        }
        item(key = "compose") {
            ComposePrompt(
                name = uiState.currentUserName,
                photoUrl = uiState.currentUserImageUrl,
                onClick = onCreatePost,
                modifier = Modifier.padding(horizontal = ScreenGutter)
            )
        }

        when {
            uiState.isLoading && uiState.posts.isEmpty() -> item(key = "loading") {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent)
                }
            }
            uiState.posts.isEmpty() -> item(key = "empty") {
                EmptyState(
                    icon = Icons.Outlined.Forum,
                    title = "No posts yet",
                    message = "Be the first to share something with your neighbours.",
                    modifier = Modifier.padding(top = 48.dp)
                )
            }
            else -> {
                // The newest recent announcement sits above everything; the rest stay in date order.
                val pinned = uiState.posts.firstOrNull {
                    it.category == PostCategory.ANNOUNCEMENT.key && nowMillis() - it.createdAt < PIN_WINDOW_MILLIS
                }
                val ordered = listOfNotNull(pinned) + uiState.posts.filter { it.id != pinned?.id }
                items(ordered, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        currentUserId = uiState.currentUserId ?: "",
                        onLike = { onLike(post.id) },
                        onCommentClick = { onPostClick(post.id) },
                        onClick = { onPostClick(post.id) },
                        authorIsAdmin = post.authorId in uiState.adminIds,
                        pinned = post.id == pinned?.id,
                        modifier = Modifier.padding(horizontal = ScreenGutter)
                    )
                }
                if (uiState.isLoadingMore) {
                    item(key = "more") {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = accent, strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }
    }
}
