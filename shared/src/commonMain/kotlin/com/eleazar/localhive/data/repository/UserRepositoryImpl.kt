package com.eleazar.localhive.data.repository

import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.BlockedUser
import com.eleazar.localhive.domain.model.User
import com.eleazar.localhive.ui.util.nowMillis
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class UserRepositoryImpl(
    private val firestore: FirebaseFirestore
) : UserRepository {

    override suspend fun getUser(userId: String, cacheFirst: Boolean): Result<User> {
        return try {
            val ref = firestore.users().document(userId)
            val cached = if (cacheFirst) {
                runCatching { ref.get(Source.CACHE) }.getOrNull()?.takeIf { it.exists }
            } else {
                null
            }
            val doc = cached ?: ref.get()
            if (!doc.exists) throw Exception("User not found")
            Result.success(doc.toUser())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeUser(userId: String): Flow<User?> =
        firestore.users().document(userId).snapshots
            .map { snapshot -> snapshot.takeIf { it.exists }?.toUser() }
            .catch { emit(null) }

    override fun getEstateMembers(estateId: String): Flow<List<User>> =
        firestore.users()
            .where { "estateId" equalTo estateId }
            .snapshots
            .map { snapshot -> snapshot.documents.map { it.toUser() } }

    override suspend fun updateUser(user: User): Result<Unit> = runCatching {
        firestore.users().document(user.id).set(user.toNonNullMap(), merge = true)
    }

    override suspend fun updateFcmToken(userId: String, token: String): Result<Unit> = runCatching {
        firestore.users().document(userId).set(mapOf("fcmToken" to token), merge = true)
    }

    override suspend fun setLightAlerts(userId: String, enabled: Boolean): Result<Unit> = runCatching {
        firestore.users().document(userId).set(mapOf("lightAlerts" to enabled), merge = true)
    }

    override suspend fun clearFcmToken(userId: String): Result<Unit> = runCatching {
        firestore.users().document(userId).update("fcmToken" to FieldValue.delete)
    }

    override fun observeBlockedUsers(userId: String): Flow<List<BlockedUser>> =
        blocksOf(userId)
            .orderBy("blockedAt", Direction.DESCENDING)
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    BlockedUser(
                        id = doc.id,
                        name = doc.string("name").orEmpty(),
                        photoUrl = doc.string("photoUrl"),
                        blockedAt = doc.long("blockedAt") ?: 0L
                    )
                }
            }
            .catch { emit(emptyList()) }

    override suspend fun blockUser(userId: String, blocked: BlockedUser): Result<Unit> = runCatching {
        blocksOf(userId).document(blocked.id).set(
            mapOf(
                "name" to blocked.name,
                "photoUrl" to blocked.photoUrl,
                "blockedAt" to nowMillis()
            )
        )
    }

    override suspend fun unblockUser(userId: String, blockedId: String): Result<Unit> = runCatching {
        blocksOf(userId).document(blockedId).delete()
    }

    private fun blocksOf(userId: String) = firestore.users().document(userId).collection("blocks")
}
