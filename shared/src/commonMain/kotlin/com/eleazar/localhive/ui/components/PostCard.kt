package com.eleazar.localhive.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.eleazar.localhive.domain.model.Post
import com.eleazar.localhive.domain.model.PostCategory
import com.eleazar.localhive.ui.theme.DarkGray
import com.eleazar.localhive.ui.theme.HoneyGold
import com.eleazar.localhive.ui.theme.HoneyInk
import com.eleazar.localhive.ui.theme.WarmInk
import androidx.compose.ui.graphics.luminance
import com.eleazar.localhive.ui.util.relativeTime

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
    pinned: Boolean = false
) {
    val isLiked = currentUserId in post.likedBy
    val category = PostCategory.fromKey(post.category)
    val isAnnouncement = category == PostCategory.ANNOUNCEMENT
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    // Announcements stay a normal white card so the text reads as easily as any post;
    // a honey border and header strip are what mark them out.
    val honey = if (dark) MaterialTheme.colorScheme.secondary else HoneyGold
    val style = if (isAnnouncement) {
        PostStyle(
            container = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, honey.copy(alpha = if (dark) 0.55f else 0.7f)),
            content = MaterialTheme.colorScheme.onSurface,
            accent = if (dark) honey else HoneyInk,
            liked = if (dark) honey else HoneyInk
        )
    } else {
        PostStyle(
            container = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
            content = MaterialTheme.colorScheme.onSurface,
            accent = MaterialTheme.colorScheme.primary,
            liked = if (dark) MaterialTheme.colorScheme.secondary else HoneyInk
        )
    }
    val muted = style.content.copy(alpha = 0.55f)

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = style.container,
        contentColor = style.content,
        border = style.border
    ) {
        Column {
            if (isAnnouncement) {
                AnnouncementBanner(
                    pinned = pinned,
                    color = style.accent,
                    badge = honey,
                    onBadge = if (dark) DarkGray else WarmInk,
                    background = honey.copy(alpha = if (dark) 0.14f else 0.16f)
                )
            }
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = if (isAnnouncement) 14.dp else 16.dp, bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(
                        imageUrl = post.authorImageUrl,
                        name = post.authorName,
                        size = 42.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.authorName.ifBlank { "Neighbour" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = style.content,
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
                                Text(text = "  ·  $time", fontSize = 13.sp, color = muted, maxLines = 1)
                            }
                        }
                        if (category != null && !isAnnouncement) {
                            Spacer(Modifier.height(4.dp))
                            CategoryTag(category)
                        }
                    }
                }

                if (post.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = post.content,
                        fontSize = if (isAnnouncement) 17.sp else 16.sp,
                        lineHeight = if (isAnnouncement) 25.sp else 23.sp,
                        fontWeight = if (isAnnouncement) FontWeight.SemiBold else FontWeight.Normal,
                        color = style.content
                    )
                }

                post.imageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                    Spacer(modifier = Modifier.height(12.dp))
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                // Pulled in by the action's own padding so the icons sit on the text's left edge.
                Row(
                    modifier = Modifier.offset(x = -ActionPadding),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PostAction(
                        icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        count = post.likeCount,
                        tint = if (isLiked) style.liked else muted,
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
}

private val ActionPadding = 8.dp

/** Colours one post card is drawn with; announcements swap the whole set. */
private data class PostStyle(
    val container: Color,
    val border: BorderStroke?,
    val content: Color,
    val accent: Color,
    val liked: Color
)

@Composable
private fun AnnouncementBanner(pinned: Boolean, color: Color, badge: Color, onBadge: Color, background: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = badge, modifier = Modifier.size(28.dp)) {
            Icon(
                PostCategory.ANNOUNCEMENT.icon,
                contentDescription = null,
                tint = onBadge,
                modifier = Modifier.padding(6.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "ANNOUNCEMENT",
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            modifier = Modifier.weight(1f)
        )
        if (pinned) {
            Icon(Icons.Filled.PushPin, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text("Pinned", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = "$count", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}
