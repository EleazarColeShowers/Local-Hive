package com.eleazar.localhive.data.repository

import com.eleazar.localhive.data.remote.LocalHiveApi
import com.eleazar.localhive.domain.NotificationRepository
import com.eleazar.localhive.domain.model.AppNotification
import com.eleazar.localhive.domain.model.NotificationType
import com.eleazar.localhive.ui.util.nowMillis
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotificationRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val api: LocalHiveApi
) : NotificationRepository {

    private val notifications get() = firestore.collection("notifications")

    override fun observeNotifications(userId: String): Flow<List<AppNotification>> =
        notifications
            .where { "recipientId" equalTo userId }
            .orderBy("createdAt", Direction.DESCENDING)
            .limit(50)
            .snapshots
            .map { snapshot -> snapshot.documents.mapNotNull { mapDocToNotification(it) } }

    override suspend fun createNotification(notification: AppNotification): Result<Unit> = runCatching {
        val ref = notifications.document
        ref.set(
            mapOf(
                "recipientId" to notification.recipientId,
                // Rules require this to be the writer; the Worker checks it before pushing.
                "senderId" to auth.currentUser?.uid,
                "type" to notification.type.name,
                "title" to notification.title,
                "message" to notification.message,
                "estateId" to notification.estateId,
                "relatedUserId" to notification.relatedUserId,
                "postId" to notification.postId,
                "commentId" to notification.commentId,
                "levyId" to notification.levyId,
                "createdAt" to nowMillis(),
                "isRead" to false
            )
        )
        api.notifyInBackground("type" to "notification", "notificationId" to ref.id)
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> = runCatching {
        notifications.document(notificationId).update("isRead" to true)
    }

    override suspend fun markAllAsRead(userId: String): Result<Unit> = runCatching {
        val snapshot = notifications
            .where { "recipientId" equalTo userId }
            .where { "isRead" equalTo false }
            .get()
        val batch = firestore.batch()
        snapshot.documents.forEach { batch.update(it.reference, "isRead" to true) }
        batch.commit()
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> = runCatching {
        notifications.document(notificationId).delete()
    }

    private fun mapDocToNotification(doc: DocumentSnapshot): AppNotification? = runCatching {
        AppNotification(
            id = doc.id,
            recipientId = doc.string("recipientId").orEmpty(),
            type = runCatching { NotificationType.valueOf(doc.string("type").orEmpty()) }
                .getOrDefault(NotificationType.JOIN_REQUEST),
            title = doc.string("title").orEmpty(),
            message = doc.string("message").orEmpty(),
            estateId = doc.string("estateId"),
            relatedUserId = doc.string("relatedUserId"),
            postId = doc.string("postId"),
            commentId = doc.string("commentId"),
            levyId = doc.string("levyId"),
            createdAt = doc.long("createdAt") ?: 0L,
            isRead = doc.boolean("isRead") ?: false
        )
    }.getOrNull()
}
