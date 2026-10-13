package com.eleazar.localhive.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.ui.components.Avatar
import com.eleazar.localhive.ui.components.PostCard
import com.eleazar.localhive.ui.components.RoleBadge
import com.eleazar.localhive.ui.components.ScreenGutter
import com.eleazar.localhive.ui.util.relativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    uiState: FeedUiState,
    onNavigateBack: () -> Unit,
    onLike: (postId: String) -> Unit,
    onAddComment: (content: String) -> Unit,
) {
    var commentText by rememberSaveable { mutableStateOf("") }
    val accent = MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Post", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        TextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Add a comment") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = accent
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    onAddComment(commentText.trim())
                                    commentText = ""
                                }
                            },
                            enabled = commentText.isNotBlank(),
                            shape = CircleShape,
                            modifier = Modifier.padding(bottom = 2.dp).size(52.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = accent,
                                contentColor = Color.White,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Post comment", modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.selectedPost?.let { post ->
                item(key = "post") {
                    PostCard(
                        post = post,
                        currentUserId = uiState.currentUserId ?: "",
                        onLike = { onLike(post.id) },
                        onCommentClick = {},
                        onClick = {},
                        authorIsAdmin = post.authorId in uiState.adminIds
                    )
                }
            }

            item(key = "comments-header") {
                Text(
                    if (uiState.comments.isEmpty()) "No comments yet" else "Comments · ${uiState.comments.size}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (uiState.comments.isEmpty()) 0.6f else 1f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(uiState.comments, key = { it.id }) { comment ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Avatar(imageUrl = null, name = comment.authorName, size = 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    comment.authorName.ifBlank { "Neighbour" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (comment.authorId in uiState.adminIds) {
                                    Spacer(Modifier.width(6.dp))
                                    RoleBadge()
                                }
                                val time = relativeTime(comment.createdAt)
                                if (time.isNotEmpty()) {
                                    Text(
                                        "  ·  $time",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(comment.content, fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
