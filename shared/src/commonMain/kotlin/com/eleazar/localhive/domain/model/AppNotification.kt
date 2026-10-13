package com.eleazar.localhive.domain.model

enum class NotificationType {
    JOIN_REQUEST,
    REQUEST_APPROVED,
    REQUEST_DENIED,
    MEMBER_REMOVED
}

data class AppNotification(
    val id: String = "",
    val recipientId: String = "",
    val type: NotificationType = NotificationType.JOIN_REQUEST,
    val title: String = "",
    val message: String = "",
    val estateId: String? = null,
    val relatedUserId: String? = null,
    val createdAt: Long = 0L,
    val isRead: Boolean = false
)
