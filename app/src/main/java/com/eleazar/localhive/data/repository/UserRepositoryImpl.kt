package com.eleazar.localhive.data.repository

import com.eleazar.localhive.data.local.dao.UserDao
import com.eleazar.localhive.data.local.mapper.toDomain
import com.eleazar.localhive.data.local.mapper.toEntity
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.BlockedUser
import com.eleazar.localhive.domain.model.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao
) : UserRepository {

    override suspend fun getUser(userId: String, cacheFirst: Boolean): Result<User> {
        return try {
            val ref = firestore.collection("users").document(userId)
            val cached = if (cacheFirst) {
                runCatching { ref.get(Source.CACHE).await() }.getOrNull()?.takeIf { it.exists() }
            } else {
                null
            }
            val doc = cached ?: ref.get().await()
            val user = doc.toObject(User::class.java)?.copy(id = doc.id)
                ?: throw Exception("User not found")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(null); return@addSnapshotListener }
                val user = snapshot?.toObject(User::class.java)?.copy(id = snapshot.id)
                trySend(user)
            }
        awaitClose { listener.remove() }
    }

    override fun getEstateMembers(estateId: String): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .whereEqualTo("estateId", estateId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(users)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun updateUser(user: User): Result<Unit> {
        return try {
            firestore.collection("users").document(user.id)
                .set(user.toNonNullMap(), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateFcmToken(userId: String, token: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .set(mapOf("fcmToken" to token), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearFcmToken(userId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .update("fcmToken", FieldValue.delete()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeBlockedUsers(userId: String): Flow<List<BlockedUser>> = callbackFlow {
        val listener = blocksOf(userId)
            .orderBy("blockedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val blocked = snapshot?.documents?.map { doc ->
                    BlockedUser(
                        id = doc.id,
                        name = doc.getString("name").orEmpty(),
                        photoUrl = doc.getString("photoUrl"),
                        blockedAt = doc.getLong("blockedAt") ?: 0L
                    )
                } ?: emptyList()
                trySend(blocked)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun blockUser(userId: String, blocked: BlockedUser): Result<Unit> {
        return try {
            blocksOf(userId).document(blocked.id).set(
                mapOf(
                    "name" to blocked.name,
                    "photoUrl" to blocked.photoUrl,
                    "blockedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unblockUser(userId: String, blockedId: String): Result<Unit> {
        return try {
            blocksOf(userId).document(blockedId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun blocksOf(userId: String) =
        firestore.collection("users").document(userId).collection("blocks")

    override suspend fun saveUserLocally(user: User) {
        userDao.insertUser(user.toEntity())
    }

    override suspend fun getLocalUser(userId: String): User? {
        return userDao.getUser(userId)?.toDomain()
    }

    private fun User.toNonNullMap(): Map<String, Any> = buildMap {
        email?.let { put("email", it) }
        displayName?.let { put("displayName", it) }
        username?.let { put("username", it) }
        profileImageUrl?.let { put("profileImageUrl", it) }
        bio?.let { put("bio", it) }
        estateId?.let { put("estateId", it) }
        address?.let { put("address", it) }
        occupation?.let { put("occupation", it) }
        phone?.let { put("phone", it) }
    }
}
