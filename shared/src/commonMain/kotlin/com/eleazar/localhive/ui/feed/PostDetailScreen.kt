package com.eleazar.localhive.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.ui.draw.clip
import com.eleazar.localhive.domain.model.Comment
import com.eleazar.localhive.ui.components.Avatar
import com.eleazar.localhive.ui.components.CharactersLeft
import com.eleazar.localhive.ui.platform.rememberPlatformActions
import com.eleazar.localhive.ui.components.LinkifiedText
import com.eleazar.localhive.ui.components.MentionSuggestionList
import com.eleazar.localhive.ui.components.activeMentionQuery
import com.eleazar.localhive.ui.components.insertMention
import com.eleazar.localhive.ui.components.mentionName
import com.eleazar.localhive.ui.components.mentionSuggestions
import com.eleazar.localhive.ui.components.mentionsIn
import androidx.compose.ui.text.input.TextFieldValue
import com.eleazar.localhive.ui.components.PostCard
import androidx.compose.material.icons.outlined.Flag
import com.eleazar.localhive.domain.model.Report
import com.eleazar.localhive.domain.model.ReportReason
import com.eleazar.localhive.ui.report.ReportDialog
import com.eleazar.localhive.ui.components.RoleBadge
import com.eleazar.localhive.ui.components.ScreenGutter
import com.eleazar.localhive.ui.util.relativeTime
import com.eleazar.localhive.ui.util.nowMillis
import com.eleazar.localhive.domain.model.isEditable
import androidx.compose.material.icons.outlined.Edit

private const val MAX_COMMENT_LENGTH = 1000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    uiState: FeedUiState,
    onNavigateBack: () -> Unit,
    onLike: (postId: String) -> Unit,
    /** Posts a comment, or a reply under the comment [parentId] when it's set. */
    onAddComment: (content: String, mentions: Map<String, String>, parentId: String?) -> Unit,
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
                                val editing = editingComment
                                if (commentText.isNotBlank() && editing != null) {
                                    onEditComment(editing.id, commentText.trim(), mentionsIn(commentText, picked))
                                    cancelEdit()
                                } else if (commentText.isNotBlank()) {
                                    // Replies all hang off the top-level comment, one level deep.
                                    val parentId = replyingTo?.let { it.parentId ?: it.id }
                                    onAddComment(commentText.trim(), mentionsIn(commentText, picked), parentId)
                                    draft = TextFieldValue()
                                    picked = emptyMap()
                                    replyingTo = null
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

private data class CommentLine(val comment: Comment, val isReply: Boolean)

/** Each comment in order, followed by its replies, oldest first. */
private fun commentThreads(comments: List<Comment>): List<CommentLine> {
    val ids = comments.mapTo(HashSet()) { it.id }
    val replies = comments.filter { it.parentId != null && it.parentId in ids }.groupBy { it.parentId }
    return buildList {
        comments.filter { it.parentId == null || it.parentId !in ids }.forEach { top ->
            add(CommentLine(top, isReply = false))
            replies[top.id].orEmpty().forEach { add(CommentLine(it, isReply = true)) }
        }
    }
}

/** One comment. Its menu button, or a long press, offers to copy or report it, and to delete it for those allowed to. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommentRow(
    comment: Comment,
    isReply: Boolean,
    onReply: (() -> Unit)?,
    authorIsAdmin: Boolean,
    canDelete: Boolean,
    onUserClick: (userId: String) -> Unit,
    onDelete: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onReport: ((ReportReason, details: String) -> Unit)? = null
) {
    val accent = MaterialTheme.colorScheme.primary
    val platform = rememberPlatformActions()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Replies sit in under the comment they answer, lined up with its text.
            .padding(start = if (isReply) ScreenGutter + 44.dp else ScreenGutter, end = ScreenGutter, top = if (isReply) 2.dp else 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Avatar(imageUrl = null, name = comment.authorName, size = if (isReply) 26.dp else 34.dp)
        Spacer(Modifier.width(10.dp))
        // The bubble wraps its text; the space it leaves keeps the menu button at the right edge.
        Column(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .combinedClickable(
                        onClickLabel = null,
                        onLongClickLabel = "Comment options",
                        onClick = {},
                        onLongClick = { menuOpen = true }
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            comment.authorName.ifBlank { "Neighbour" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (authorIsAdmin) {
                            Spacer(Modifier.width(6.dp))
                            RoleBadge()
                        }
                        val time = relativeTime(comment.createdAt)
                        if (time.isNotEmpty()) {
                            Text(
                                "  ·  $time" + if (comment.editedAt > 0L) " · edited" else "",
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
                        onMentionClick = onUserClick
                    )
                }
            }
            if (onReply != null) {
                Text(
                    "Reply",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onReply)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
        // A visible way in to the same menu as the long press, which people don't find on their own.
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "Comment options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (onReply != null) {
                    DropdownMenuItem(
                        text = { Text("Reply") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Reply, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onReply()
                        }
                    )
                }
                if (onEdit != null) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Copy") },
                    leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        platform.copyToClipboard("Comment", comment.content, "Copied")
                    }
                )
                if (onReport != null) {
                    DropdownMenuItem(
                        text = { Text("Report") },
                        leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            reporting = true
                        }
                    )
                }
                if (canDelete) {
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuOpen = false
                            confirmDelete = true
                        }
                    )
                }
            }
        }
    }
    if (reporting && onReport != null) {
        ReportDialog(title = "Report comment", onSubmit = onReport, onDismiss = { reporting = false })
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this comment?") },
            text = { Text("It'll be removed for everyone. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
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

/** Marks an emergency a false alarm, after saying what that means, or (for admins) takes it back. */
@Composable
private fun FalseAlarmAction(
    marked: Boolean,
    isMine: Boolean,
    authorName: String,
    onMark: () -> Unit,
    onUndo: () -> Unit
) {
    var confirming by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (marked) "Marked as a false alarm" else "Wasn't a real emergency?",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = { confirming = true }) {
            Text(
                if (marked) "Undo" else "Mark as false alarm",
                color = if (marked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
    if (confirming) {
        val (title, body) = when {
            marked -> "Not a false alarm?" to
                "The false-alarm mark comes off, and it stops counting against $authorName."
            isMine -> "Mark your alert as a false alarm?" to
                "It closes, everyone is told no help is needed, and it shows as a false alarm. " +
                "Two false alarms in 30 days and your alerts go out without the siren."
            else -> "Mark this as a false alarm?" to
                "It closes, everyone is told no help is needed, and it shows as a false alarm. " +
                "Two false alarms in 30 days and $authorName's alerts go out without the siren."
        }
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = {
                TextButton(onClick = { confirming = false; if (marked) onUndo() else onMark() }) {
                    Text(
                        if (marked) "Undo" else "Mark false alarm",
                        color = if (marked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancel") } }
        )
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
