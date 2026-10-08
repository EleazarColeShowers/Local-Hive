package com.eleazar.localhive.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.R
import com.eleazar.localhive.ui.components.PostCard

@Composable
fun FeedScreen(
    onPostClick: (String) -> Unit = {},
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { lastVisible ->
                if (uiState.posts.isNotEmpty() && lastVisible >= uiState.posts.size - 3) {
                    viewModel.loadMorePosts()
                }
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.posts.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colorResource(id = R.color.hivegreen)
                )
            }
            uiState.posts.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No posts yet", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text("Be the first to share something!", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            currentUserId = uiState.currentUserId ?: "",
                            onLike = { viewModel.toggleLike(post.id) },
                            onCommentClick = { onPostClick(post.id) },
                            onClick = { onPostClick(post.id) }
                        )
                    }
                    if (uiState.isLoadingMore) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = colorResource(id = R.color.hivegreen)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
