package com.eleazar.localhive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.eleazar.localhive.domain.model.EmergencyType
import com.eleazar.localhive.domain.model.Post
import com.eleazar.localhive.domain.model.PostCategory
import com.eleazar.localhive.ui.theme.AlertRed
import com.eleazar.localhive.ui.theme.HoneyInk
import com.eleazar.localhive.ui.util.relativeTime

/** The familiar "liked" red, rather than a brand colour, so a like reads at a glance. */
private val LikedRed = Color(0xFFE53950)
private val AvatarSize = 40.dp
private val AvatarGap = 12.dp
private val MediaShape = RoundedCornerShape(12.dp)

/**
 * One post as a full-width row: avatar on the left, everything else in a column beside it.
 * Lists separate posts with a divider rather than spacing each one out as a card.
 */
@Composable
fun PostCard(
    post: Post,
    currentUserId: String,
    onLike: () -> Unit,
    onCommentClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** The author is currently an admin of the estate. */
    authorIsAdmin: Boolean = false,
    /** Shown at the top of the feed rather than in date order. */
    pinned: Boolean = false,
    /** Play a video post here; otherwise it shows a still that opens the post via [onClick]. */
    playVideo: Boolean = false,
    /** Opens a member tagged in the post. */
    onMentionClick: ((userId: String) -> Unit)? = null,
    /** Offered to the post's author. */
    onEdit: (() -> Unit)? = null,
    /**
     * Offered to the author and the estate's admins; asks before deleting. An admin removing
     * someone else's post can give a reason, which is passed on to its author.
     */
    onDelete: ((reason: String) -> Unit)? = null
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val isLiked = currentUserId in post.likedBy
    val category = PostCategory.fromKey(post.category)
    val isAnnouncement = category == PostCategory.ANNOUNCEMENT
    val isEmergency = category == PostCategory.EMERGENCY
    val alertRed = readable(AlertRed)
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val honey = if (dark) MaterialTheme.colorScheme.secondary else HoneyInk
    val content = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            // The faintest honey wash, so an announcement is findable without shouting.
            .background(
                when {
                    isEmergency -> alertRed.copy(alpha = if (dark) 0.12f else 0.06f)
                    isAnnouncement -> honey.copy(alpha = if (dark) 0.08f else 0.05f)
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
    ) {
        if (isAnnouncement) {
            // Lined up like the rest of the post: icon over the avatar, words over the text.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                Box(modifier = Modifier.width(AvatarSize), contentAlignment = Alignment.CenterEnd) {
                    Icon(
                        if (pinned) Icons.Filled.PushPin else PostCategory.ANNOUNCEMENT.icon,
                        contentDescription = null,
                        tint = honey,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(Modifier.width(AvatarGap))
                Text(
                    if (pinned) "Pinned announcement" else "Announcement",
                    color = honey,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (isEmergency) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                Box(modifier = Modifier.width(AvatarSize), contentAlignment = Alignment.CenterEnd) {
                    Icon(
                        EmergencyType.fromKey(post.emergencyType)?.icon ?: PostCategory.EMERGENCY.icon,
                        contentDescription = null,
                        tint = alertRed,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(Modifier.width(AvatarGap))
                Text(post.emergencyTitle, color = alertRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row {
            Avatar(imageUrl = post.authorImageUrl, name = post.authorName, size = AvatarSize)
            Spacer(Modifier.width(AvatarGap))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = post.authorName.ifBlank { "Neighbour" },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (authorIsAdmin) {
                        Spacer(Modifier.width(6.dp))
                        RoleBadge()
                    }
                    val time = relativeTime(post.createdAt)
                    if (time.isNotEmpty()) {
                        Text(text = " · $time", fontSize = 14.sp, color = muted, maxLines = 1)
                    }
                    if (post.editedAt > 0L) {
                        Text(text = " · edited", fontSize = 14.sp, color = muted, maxLines = 1)
                    }
                    if (onEdit != null || onDelete != null) {
                        Spacer(Modifier.weight(1f))
                        // Pulled up and right so the bigger touch target doesn't push the row down.
                        Box(modifier = Modifier.size(24.dp).wrapContentSize(unbounded = true)) {
                            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Post options", tint = muted, modifier = Modifier.size(20.dp))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                if (onEdit != null) {
                                    DropdownMenuItem(
                                        text = { Text("Edit") },
                                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                        onClick = { menuOpen = false; onEdit() }
                                    )
                                }
                                if (onDelete != null) {
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        },
                                        onClick = { menuOpen = false; confirmDelete = true }
                                    )
                                }
                            }
                        }
                    }
                }
                if (category != null && !isAnnouncement && !isEmergency) {
                    CategoryTag(category, modifier = Modifier.padding(top = 2.dp))
                }
                if (category?.isIncident == true) {
                    IncidentSummary(post, modifier = Modifier.padding(top = 4.dp))
                }

                if (post.content.isNotBlank()) {
                    LinkifiedText(
                        text = post.content,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        color = content,
                        linkColor = MaterialTheme.colorScheme.primary,
                        mentions = post.mentions,
                        onMentionClick = onMentionClick,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                post.imageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(MediaShape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MediaShape),
                        contentScale = ContentScale.Crop
                    )
                }

                post.videoUrl?.takeIf { it.isNotBlank() }?.let { videoUrl ->
                    Spacer(modifier = Modifier.height(10.dp))
                    if (playVideo) {
                        VideoPlayer(
                            url = videoUrl,
                            autoPlay = true,
                            modifier = Modifier.fillMaxWidth().clip(MediaShape)
                        )
                    } else {
                        VideoPoster(thumbnailUrl = post.videoThumbnailUrl)
                    }
                }

                // Pulled in by the action's own padding so the icons sit on the text's left edge.
                Row(
                    modifier = Modifier.padding(top = 2.dp).offset(x = -ActionPadding),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PostAction(
                        icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        count = post.likeCount,
                        tint = if (isLiked) LikedRed else muted,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        onClick = onLike
                    )
                    PostAction(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        count = post.commentCount,
                        tint = muted,
                        contentDescription = "Comments",
                        onClick = onCommentClick
                    )
                }
            }
        }
    }
    if (confirmDelete && onDelete != null) {
        ConfirmDeleteDialog(
            post = post,
            isOwnPost = post.authorId == currentUserId,
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false }
        )
    }
}

private val ActionPadding = 8.dp

/** Deleting can't be undone, and takes the comments with it, so it's always confirmed. */
@Composable
private fun ConfirmDeleteDialog(post: Post, isOwnPost: Boolean, onConfirm: (reason: String) -> Unit, onDismiss: () -> Unit) {
    var reason by remember { mutableStateOf("") }
    val author = post.authorName.ifBlank { "this neighbour" }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isOwnPost) "Delete your post?" else "Delete $author's post?") },
        text = {
            Column {
                Text(
                    if (isOwnPost) "It'll be removed from the feed for everyone, along with its comments. This can't be undone."
                    else "It'll disappear for everyone, along with its comments, and $author will be told an admin " +
                        "removed it. This can't be undone."
                )
                if (!isOwnPost) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it.take(200) },
                        label = { Text("Reason (optional)") },
                        placeholder = { Text("e.g. Selling isn't allowed in the feed") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm(reason) }) {
                Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** A video's still with a play button; taps fall through to the post, which opens it. */
@Composable
private fun VideoPoster(thumbnailUrl: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(MediaShape)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        }
        Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.55f), modifier = Modifier.size(52.dp)) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Play video",
                tint = Color.White,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun PostAction(
    icon: ImageVector,
    count: Int,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = Color.Transparent) {
        Row(
            modifier = Modifier.heightIn(min = 40.dp).padding(horizontal = ActionPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(19.dp))
            if (count > 0) {
                Spacer(Modifier.width(5.dp))
                Text(text = "$count", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
