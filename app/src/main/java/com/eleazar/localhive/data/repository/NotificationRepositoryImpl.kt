package com.eleazar.localhive.data.repository

import com.eleazar.localhive.domain.NotificationRepository
import com.eleazar.localhive.domain.model.AppNotification
import com.eleazar.localhive.domain.model.NotificationType
import com.google.firebase.firestore.DocumentSnapshot
import com.eleazar.localhive.data.remote.api.LocalHiveApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val api: LocalHiveApi
) : NotificationRepository {

    override fun observeNotifications(userId: String): Flow<List<AppNotification>> = callbackFlow {
        val sub = firestore.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val notifications = snapshot?.documents?.mapNotNull { mapDocToNotification(it) } ?: emptyList()
                trySend(notifications)
            }
        awaitClose { sub.remove() }
    }

    override suspend fun createNotification(notification: AppNotification): Result<Unit> {
        return try {
            val ref = firestore.collection("notifications").document()
            val data = mapOf(
                "recipientId" to notification.recipientId,
                // Rules require this to be the writer; the Worker checks it before pushing.
                "senderId" to auth.currentUser?.uid,
                "type" to notification.type.name,
                "title" to notification.title,
                "message" to notification.message,
                "estateId" to notification.estateId,
                "relatedUserId" to notification.relatedUserId,
                "createdAt" to System.currentTimeMillis(),
                "isRead" to false
            )
            ref.set(data).await()
            api.notifyInBackground("type" to "notification", "notificationId" to ref.id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            firestore.collection("notifications").document(notificationId)
                .update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAllAsRead(userId: String): Result<Unit> {
        return try {
            val snapshot = firestore.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .whereEqualTo("isRead", false)
                .get().await()
            val batch = firestore.batch()
            snapshot.documents.forEach { batch.update(it.reference, "isRead", true) }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            firestore.collection("notifications").document(notificationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapDocToNotification(doc: DocumentSnapshot): AppNotification? {
        return try {
            AppNotification(
                id = doc.id,
                recipientId = doc.getString("recipientId") ?: "",
                type = runCatching { NotificationType.valueOf(doc.getString("type") ?: "") }
                    .getOrDefault(NotificationType.JOIN_REQUEST),
                title = doc.getString("title") ?: "",
                message = doc.getString("message") ?: "",
                estateId = doc.getString("estateId"),
                relatedUserId = doc.getString("relatedUserId"),
                createdAt = doc.getLong("createdAt") ?: 0L,
                isRead = doc.getBoolean("isRead") ?: false
            )
        } catch (e: Exception) { null }
    }
}
