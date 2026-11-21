package com.eleazar.localhive.ui.feed

import com.eleazar.localhive.domain.NotificationRepository
import com.eleazar.localhive.domain.model.AppNotification
import com.eleazar.localhive.domain.model.Comment
import com.eleazar.localhive.domain.model.NotificationType
import com.eleazar.localhive.domain.model.Post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The in-app notifications the feed sends: tags, comments, and posts an admin removed. Each is
 * best-effort, sent after the post or comment is saved: one failing shouldn't undo it or block
 * the others.
 */
internal class FeedNotifier(
    private val notifications: NotificationRepository,
    private val scope: CoroutineScope
) {
    /** Tells each tagged member, except the sender [me], in the app and by push. */
    fun mentioned(
        userIds: Collection<String>,
        me: String,
        myName: String,
        estateId: String,
        postId: String,
        commentId: String?,
        content: String
    ) {
        val title = if (commentId == null) "$myName tagged you in a post" else "$myName tagged you in a comment"
        userIds.filter { it != me }.forEach { recipientId ->
            send(
                AppNotification(
                    recipientId = recipientId,
                    type = NotificationType.MENTION,
                    title = title,
                    message = content.take(140),
                    estateId = estateId,
                    relatedUserId = me,
                    postId = postId,
                    commentId = commentId
                )
            )
        }
    }

    /**
     * Tells the post's author about a new comment, and for a reply, the author of the comment it
     * answers. Anyone tagged in it already hears through their mention, so isn't told twice.
     */
    fun commented(
        comment: Comment,
        commentId: String,
        estateId: String,
        me: String,
        myName: String,
        postAuthorId: String?,
        parentAuthorId: String?
    ) {
        val recipients = buildMap {
            parentAuthorId?.let { put(it, "$myName replied to your comment") }
            // The post's author hears about every comment on it, a reply included.
            postAuthorId?.let { if (!containsKey(it)) put(it, "$myName commented on your post") }
        }.filterKeys { it != me && it !in comment.mentions }
        recipients.forEach { (recipientId, title) ->
            send(
                AppNotification(
                    recipientId = recipientId,
                    type = NotificationType.COMMENT,
                    title = title,
                    message = comment.content.take(140).ifBlank { "📷 Photo" },
                    estateId = estateId,
                    relatedUserId = me,
                    postId = comment.postId,
                    commentId = commentId
                )
            )
        }
    }

    /** Tells an author that an admin removed their post, with the admin's [reason] if they gave one. */
    fun postRemoved(post: Post, reason: String, adminId: String?) {
        val text = post.content.trim()
        val excerpt = text.take(80).let { if (text.length > 80) "$it…" else it }
        send(
            AppNotification(
                recipientId = post.authorId,
                type = NotificationType.POST_REMOVED,
                title = "An admin removed your post",
                message = when {
                    reason.isNotBlank() -> "Reason: ${reason.trim()}"
                    excerpt.isNotBlank() -> "\"$excerpt\""
                    else -> "It no longer appears in the feed."
                },
                estateId = post.estateId,
                relatedUserId = adminId
            )
        )
    }

    private fun send(notification: AppNotification) {
        scope.launch { notifications.createNotification(notification) }
    }
}
