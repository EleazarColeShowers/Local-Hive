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
import com.eleazar.localhive.domain.model.IncidentStatus
import com.eleazar.localhive.domain.model.PostCategory
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.CheckCircle
import com.eleazar.localhive.ui.components.readable
import com.eleazar.localhive.ui.theme.AlertRed
import com.eleazar.localhive.ui.theme.HiveGreen
import com.eleazar.localhive.ui.components.Avatar
import com.eleazar.localhive.ui.components.LinkifiedText
import com.eleazar.localhive.ui.components.MentionSuggestionList
import com.eleazar.localhive.ui.components.activeMentionQuery
import com.eleazar.localhive.ui.components.insertMention
import com.eleazar.localhive.ui.components.mentionName
import com.eleazar.localhive.ui.components.mentionSuggestions
import com.eleazar.localhive.ui.components.mentionsIn
import androidx.compose.ui.text.input.TextFieldValue
import com.eleazar.localhive.ui.components.PostCard
import com.eleazar.localhive.ui.components.RoleBadge
import com.eleazar.localhive.ui.components.ScreenGutter
import com.eleazar.localhive.ui.util.relativeTime

private const val MAX_COMMENT_LENGTH = 1000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    uiState: FeedUiState,
    onNavigateBack: () -> Unit,
    onLike: (postId: String) -> Unit,
    onAddComment: (content: String, mentions: Map<String, String>) -> Unit,
    onUserClick: (userId: String) -> Unit = {},
    onUpdateStatus: (IncidentStatus) -> Unit = {},
    onEditPost: () -> Unit = {},
    onDeletePost: (reason: String) -> Unit = {},
    onRespond: () -> Unit = {},
    onMarkSafe: () -> Unit = {},
) {
    var draft by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val commentText = draft.text
    var picked by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    val mentionQuery = activeMentionQuery(draft)
    val suggestions = mentionQuery?.let { mentionSuggestions(uiState.members, it.query) }.orEmpty()
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
                Column(modifier = Modifier.navigationBarsPadding().imePadding()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    if (mentionQuery != null && suggestions.isNotEmpty()) {
                        MentionSuggestionList(suggestions, onPick = { member ->
                            draft = insertMention(draft, mentionQuery, member)
                            picked = picked + (member.id to member.mentionName)
                        })
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        TextField(
                            value = draft,
                            // Matches the comment rule in firestore.rules.
                            onValueChange = { if (it.text.length <= MAX_COMMENT_LENGTH) draft = it },
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
                                    onAddComment(commentText.trim(), mentionsIn(commentText, picked))
                                    draft = TextFieldValue()
                                    picked = emptyMap()
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
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (uiState.postMissing) {
                item(key = "missing") {
                    Text(
                        "This post has been deleted.",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = ScreenGutter, vertical = 24.dp)
                    )
                }
            }
            uiState.selectedPost?.takeIf { !uiState.postMissing }?.let { post ->
                val isMine = post.authorId == uiState.currentUserId
                item(key = "post") {
                    PostCard(
                        post = post,
                        currentUserId = uiState.currentUserId ?: "",
                        onLike = { onLike(post.id) },
                        onCommentClick = {},
                        onClick = {},
                        authorIsAdmin = post.authorId in uiState.adminIds,
                        playVideo = true,
                        onMentionClick = onUserClick,
                        onEdit = onEditPost.takeIf { isMine },
                        onDelete = onDeletePost.takeIf { isMine || uiState.isAdmin },
                        selectableText = true
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                val isLiveEmergency = post.category == PostCategory.EMERGENCY.key &&
                    post.incidentStatus != IncidentStatus.RESOLVED.key
                if (isLiveEmergency) {
                    item(key = "sos") {
                        SosFollowUp(
                            isMine = isMine,
                            authorName = post.authorName.ifBlank { "They" },
                            responding = uiState.currentUserId in post.responders,
                            onRespond = onRespond,
                            onMarkSafe = onMarkSafe
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                // The reporter and the estate's admins move a report or emergency along.
                val canUpdate = isMine || uiState.isAdmin
                if (PostCategory.fromKey(post.category)?.isIncident == true && canUpdate) {
                    item(key = "status") {
                        IncidentStatusPicker(
                            current = IncidentStatus.fromKey(post.incidentStatus),
                            onPick = onUpdateStatus
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }

            item(key = "comments-header") {
                Text(
                    if (uiState.comments.isEmpty()) "No comments yet" else "Comments · ${uiState.comments.size}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (uiState.comments.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = ScreenGutter, end = ScreenGutter, top = 16.dp, bottom = 8.dp)
                )
            }
            items(uiState.comments, key = { it.id }) { comment ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Avatar(imageUrl = null, name = comment.authorName, size = 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                            LinkifiedText(
                                comment.content,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                linkColor = accent,
                                mentions = comment.mentions,
                                onMentionClick = onUserClick,
                                selectable = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentStatusPicker(current: IncidentStatus?, onPick: (IncidentStatus) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = ScreenGutter, vertical = 12.dp)) {
        Text(
            "Update status",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IncidentStatus.entries.forEach { status ->
                FilterChip(
                    selected = status == current,
                    onClick = { if (status != current) onPick(status) },
                    label = { Text(status.label) }
                )
            }
        }
    }
}

/**
 * The two answers an SOS needs: neighbours saying they're on their way, and the person who
 * raised it saying they're safe.
 */
@Composable
private fun SosFollowUp(
    isMine: Boolean,
    authorName: String,
    responding: Boolean,
    onRespond: () -> Unit,
    onMarkSafe: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = ScreenGutter, vertical = 12.dp)) {
        when {
            isMine -> {
                Button(
                    onClick = onMarkSafe,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = HiveGreen, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("I'm safe now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text(
                    "Closes the alert and lets everyone know you're OK.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            responding -> {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = HiveGreen.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = readable(HiveGreen))
                        Spacer(Modifier.width(8.dp))
                        Text("You're on your way", fontWeight = FontWeight.Bold, color = readable(HiveGreen))
                    }
                }
                Text(
                    "$authorName knows you're coming.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            else -> {
                Button(
                    onClick = onRespond,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("I'm coming to help", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text(
                    "$authorName will be told you're on your way.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
