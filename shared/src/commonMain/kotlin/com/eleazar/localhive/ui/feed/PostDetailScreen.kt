package com.eleazar.localhive.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.background
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import com.eleazar.localhive.domain.model.Comment
import com.eleazar.localhive.ui.components.CharactersLeft
import com.eleazar.localhive.ui.components.MentionSuggestionList
import com.eleazar.localhive.ui.components.activeMentionQuery
import com.eleazar.localhive.ui.components.insertMention
import com.eleazar.localhive.ui.components.mentionName
import com.eleazar.localhive.ui.components.mentionSuggestions
import com.eleazar.localhive.ui.components.mentionsIn
import androidx.compose.ui.text.input.TextFieldValue
import com.eleazar.localhive.ui.components.PostCard
import com.eleazar.localhive.domain.model.Report
import com.eleazar.localhive.domain.model.ReportReason
import com.eleazar.localhive.ui.components.ScreenGutter
import com.eleazar.localhive.ui.util.nowMillis
import com.eleazar.localhive.domain.model.isEditable
import androidx.compose.material.icons.outlined.Image
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.eleazar.localhive.ui.platform.MediaKind
import com.eleazar.localhive.ui.platform.rememberMediaPicker

private const val MAX_COMMENT_LENGTH = 1000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    uiState: FeedUiState,
    onNavigateBack: () -> Unit,
    onLike: (postId: String) -> Unit,
    /** Posts a comment, or a reply under the comment [parentId] when it's set, with the photo at [imageUri] if picked. */
    onAddComment: (content: String, mentions: Map<String, String>, parentId: String?, imageUri: String?) -> Unit,
    onUserClick: (userId: String) -> Unit = {},
    onUpdateStatus: (IncidentStatus) -> Unit = {},
    onEditPost: () -> Unit = {},
    onDeletePost: (reason: String) -> Unit = {},
    onRespond: () -> Unit = {},
    onMarkSafe: () -> Unit = {},
    onMarkFalseAlarm: () -> Unit = {},
    onClearFalseAlarm: () -> Unit = {},
    onDeleteComment: (commentId: String) -> Unit = {},
    /** Saves the user's changes to their own comment. */
    onEditComment: (commentId: String, content: String, mentions: Map<String, String>) -> Unit = { _, _, _ -> },
    onReport: (Report) -> Unit = {},
) {
    var draft by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val commentText = draft.text
    var picked by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    val mentionQuery = activeMentionQuery(draft)
    val suggestions = mentionQuery?.let { mentionSuggestions(uiState.members, it.query) }.orEmpty()
    val accent = MaterialTheme.colorScheme.primary
    var replyingTo by remember { mutableStateOf<Comment?>(null) }
    // The user's own comment being changed in the composer, instead of a new one being written.
    var editingComment by remember { mutableStateOf<Comment?>(null) }
    // A reply that's lost its comment (deleted, or blocked) is shown on its own instead.
    val threads = remember(uiState.comments) { commentThreads(uiState.comments) }
    val composerFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    // A photo to go with the next comment. Its draft is kept until the upload and save succeed,
    // so a failed upload doesn't lose what the user wrote.
    var photoUri by rememberSaveable { mutableStateOf<String?>(null) }
    val pickPhoto = rememberMediaPicker(MediaKind.IMAGE) { photoUri = it.uri }
    var awaitingSend by remember { mutableStateOf(false) }
    var postedBeforeSend by remember { mutableStateOf(0) }
    LaunchedEffect(awaitingSend, uiState.sendingComment, uiState.commentsPosted) {
        if (awaitingSend && !uiState.sendingComment) {
            if (uiState.commentsPosted > postedBeforeSend) {
                draft = TextFieldValue()
                picked = emptyMap()
                photoUri = null
                replyingTo = null
            }
            awaitingSend = false
        }
    }
    val canSend = (commentText.isNotBlank() || (photoUri != null && editingComment == null)) && !uiState.sendingComment
    LaunchedEffect(replyingTo?.id, editingComment?.id) {
        if (replyingTo != null || editingComment != null) runCatching { composerFocus.requestFocus() }
    }

    fun startEdit(comment: Comment) {
        replyingTo = null
        editingComment = comment
        draft = TextFieldValue(comment.content, TextRange(comment.content.length))
        picked = comment.mentions
    }

    fun cancelEdit() {
        editingComment = null
        draft = TextFieldValue()
        picked = emptyMap()
    }

    // Starts a reply with an @ of the person being answered, so they're told about it and it
    // reads as an answer to them even where replies aren't shown nested.
    fun startReply(comment: Comment) {
        if (editingComment != null) cancelEdit()
        replyingTo = comment
        val member = uiState.members.firstOrNull { it.id == comment.authorId && it.id != uiState.currentUserId }
        val name = member?.mentionName?.takeIf { it.isNotEmpty() } ?: return
        if ("@$name" !in draft.text) {
            val text = "@$name " + draft.text.trimStart()
            draft = TextFieldValue(text, TextRange(text.length))
            picked = picked + (member.id to name)
        }
    }

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
                    editingComment?.let {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Editing your comment",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { cancelEdit() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel edit", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    replyingTo?.let { target ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Replying to ${target.authorName.ifBlank { "Neighbour" }}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel reply", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    CharactersLeft(
                        commentText.length,
                        MAX_COMMENT_LENGTH,
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 76.dp, top = 6.dp)
                    )
                    photoUri?.takeIf { editingComment == null }?.let { uri ->
                        Box(modifier = Modifier.padding(start = 16.dp, top = 8.dp).size(84.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize().clip(RoundedCornerShape(12.dp))
                            )
                            if (uiState.sendingComment) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.45f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                                }
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp)
                                ) {
                                    IconButton(onClick = { photoUri = null }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
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
                        // A comment's photo stays as it was posted, so there's none to add while editing.
                        if (editingComment == null) {
                            IconButton(
                                onClick = pickPhoto,
                                enabled = !uiState.sendingComment,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Image,
                                    contentDescription = if (photoUri == null) "Add a photo" else "Change photo",
                                    tint = accent
                                )
                            }
                        }
                        TextField(
                            value = draft,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            // Matches the comment rule in firestore.rules.
                            onValueChange = { if (it.text.length <= MAX_COMMENT_LENGTH) draft = it },
                            placeholder = { Text(if (replyingTo != null) "Write a reply" else "Add a comment") },
                            modifier = Modifier.weight(1f).focusRequester(composerFocus),
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
                                // Put the keyboard away once the comment is sent, so the post is visible again.
                                keyboard?.hide()
                                focusManager.clearFocus()
                                val editing = editingComment
                                val photo = photoUri
                                // Replies all hang off the top-level comment, one level deep.
                                val parentId = replyingTo?.let { it.parentId ?: it.id }
                                if (commentText.isNotBlank() && editing != null) {
                                    onEditComment(editing.id, commentText.trim(), mentionsIn(commentText, picked))
                                    cancelEdit()
                                } else if (photo != null) {
                                    // Cleared once the photo is up and the comment saved.
                                    postedBeforeSend = uiState.commentsPosted
                                    awaitingSend = true
                                    onAddComment(commentText.trim(), mentionsIn(commentText, picked), parentId, photo)
                                } else if (commentText.isNotBlank()) {
                                    onAddComment(commentText.trim(), mentionsIn(commentText, picked), parentId, null)
                                    draft = TextFieldValue()
                                    picked = emptyMap()
                                    replyingTo = null
                                }
                            },
                            enabled = canSend,
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
                        authorIsTreasurer = post.authorId == uiState.treasurerId,
                        playVideo = true,
                        onMentionClick = onUserClick,
                        onEdit = onEditPost.takeIf { isMine && isEditable(post.createdAt, nowMillis()) },
                        onDelete = onDeletePost.takeIf { isMine || uiState.isAdmin },
                        onReport = { reason: ReportReason, details: String -> onReport(Report.of(post, reason, details)) }
                            .takeIf { !isMine },
                        selectableText = true
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                val isEmergency = post.category == PostCategory.EMERGENCY.key
                val isLiveEmergency = isEmergency && post.incidentStatus != IncidentStatus.RESOLVED.key
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
                // The sender owns up to a false alarm, or an admin calls it one; only an admin takes it back.
                val canMark = isEmergency && !post.falseAlarm && canUpdate
                val canUndo = isEmergency && post.falseAlarm && uiState.isAdmin
                if (canMark || canUndo) {
                    item(key = "false-alarm") {
                        FalseAlarmAction(
                            marked = post.falseAlarm,
                            isMine = isMine,
                            authorName = post.authorName.ifBlank { "the sender" },
                            onMark = onMarkFalseAlarm,
                            onUndo = onClearFalseAlarm
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }

            item(key = "comments-header") {
                Text(
                    if (uiState.comments.isEmpty()) "No comments yet. Be the first to reply." else "Comments · ${uiState.comments.size}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (uiState.comments.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = ScreenGutter, end = ScreenGutter, top = 16.dp, bottom = 8.dp)
                )
            }
            items(threads, key = { it.comment.id }) { (comment, isReply) ->
                val me = uiState.currentUserId
                CommentRow(
                    comment = comment,
                    isReply = isReply,
                    onReply = { startReply(comment) }.takeIf { me != null },
                    authorIsAdmin = comment.authorId in uiState.adminIds,
                    // Matches who the rules let delete a comment.
                    canDelete = me != null && (
                        comment.authorId == me || uiState.selectedPost?.authorId == me || uiState.isAdmin
                    ),
                    onUserClick = onUserClick,
                    onDelete = { onDeleteComment(comment.id) },
                    onEdit = { startEdit(comment) }.takeIf {
                        me != null && comment.authorId == me && isEditable(comment.createdAt, nowMillis())
                    },
                    onReport = { reason: ReportReason, details: String ->
                        onReport(Report.of(comment, uiState.selectedPost?.id.orEmpty(), reason, details))
                    }.takeIf { me != null && comment.authorId != me }
                )
            }
        }
    }
}
