package com.eleazar.localhive.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.ui.draw.clip
import com.eleazar.localhive.domain.model.Comment
import com.eleazar.localhive.ui.components.Avatar
import com.eleazar.localhive.ui.platform.rememberPlatformActions
import com.eleazar.localhive.ui.components.LinkifiedText
import androidx.compose.material.icons.outlined.Flag
import com.eleazar.localhive.domain.model.Report
import com.eleazar.localhive.domain.model.ReportReason
import com.eleazar.localhive.ui.report.ReportDialog
import com.eleazar.localhive.ui.components.RoleBadge
import com.eleazar.localhive.ui.components.ScreenGutter
import com.eleazar.localhive.ui.util.relativeTime
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.eleazar.localhive.ui.components.ImageViewer

internal data class CommentLine(val comment: Comment, val isReply: Boolean)

/** Each comment in order, followed by its replies, oldest first. */
internal fun commentThreads(comments: List<Comment>): List<CommentLine> {
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
internal fun CommentRow(
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
    var viewingImage by remember { mutableStateOf(false) }
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
                    if (comment.content.isNotBlank()) {
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
            }
            comment.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Photo in comment",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .widthIn(max = 240.dp)
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .combinedClickable(
                            onClickLabel = "View photo",
                            onLongClickLabel = "Comment options",
                            onClick = { viewingImage = true },
                            onLongClick = { menuOpen = true }
                        )
                )
                if (viewingImage) ImageViewer(imageUrl, onDismiss = { viewingImage = false })
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
                if (comment.content.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text("Copy") },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            platform.copyToClipboard("Comment", comment.content, "Copied")
                        }
                    )
                }
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
