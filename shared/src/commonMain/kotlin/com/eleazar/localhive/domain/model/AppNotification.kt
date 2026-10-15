package com.eleazar.localhive.domain.model

enum class NotificationType {
    JOIN_REQUEST,
    REQUEST_APPROVED,
    REQUEST_DENIED,
    MEMBER_REMOVED,
    /** Someone tagged the recipient in a post or comment. */
    MENTION,
    /** A resident sent the treasurer proof of a levy payment. */
    PAYMENT_SUBMITTED,
    /** The treasurer confirmed the recipient's payment. */
    PAYMENT_CONFIRMED,
    /** The treasurer didn't accept the recipient's proof of payment. */
    PAYMENT_REJECTED,
    /** An admin deleted the recipient's post; [AppNotification.message] says why, if they gave a reason. */
    POST_REMOVED,
    /** The estate's owner handed ownership to the recipient. */
    OWNERSHIP_TRANSFERRED
}

data class AppNotification(
    val id: String = "",
    val recipientId: String = "",
    val type: NotificationType = NotificationType.JOIN_REQUEST,
    val title: String = "",
    val message: String = "",
    val estateId: String? = null,
    val relatedUserId: String? = null,
    /** The post a [NotificationType.MENTION] points to. */
    val postId: String? = null,
    /** Set when the mention was in a comment on [postId]. */
    val commentId: String? = null,
    /** The levy a payment notification is about. */
    val levyId: String? = null,
    val createdAt: Long = 0L,
    val isRead: Boolean = false
)
