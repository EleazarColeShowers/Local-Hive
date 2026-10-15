package com.eleazar.localhive.data.repository

import com.eleazar.localhive.data.platform.KeyValueStore
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** How long the saved directory is trusted before it's refreshed from the server. */
private const val DIRECTORY_MAX_AGE_MILLIS = 12 * 60 * 60 * 1000L

class UserRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val store: KeyValueStore
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

    override fun observeDirectory(estateId: String, forceRefresh: Boolean): Flow<List<User>> = flow {
        val idsKey = "directory_ids_$estateId"
        val fetchedAtKey = "directory_fetched_at_$estateId"
        val query = firestore.users().where { "estateId" equalTo estateId }

        // Who was in the estate at the last refresh. The cache can still hold people who have
        // since left, still marked with this estate, so it's filtered down to these.
        val savedIds = store.getString(idsKey)?.split(',')?.filter { it.isNotBlank() }?.toSet()
        val cached = savedIds?.let { ids ->
            runCatching { query.get(Source.CACHE) }.getOrNull()
                ?.documents?.map { it.toUser() }?.filter { it.id in ids }
        }
        if (!cached.isNullOrEmpty()) emit(cached)

        val fetchedAt = store.getString(fetchedAtKey)?.toLongOrNull() ?: 0L
        val stale = cached == null || cached.size < savedIds.orEmpty().size ||
            nowMillis() - fetchedAt > DIRECTORY_MAX_AGE_MILLIS
        if (!forceRefresh && !stale) return@flow

        val fresh = try {
            query.get(Source.SERVER).documents.map { it.toUser() }
        } catch (e: Exception) {
            // Offline: the saved copy is better than an error, if there is one.
            if (!cached.isNullOrEmpty()) return@flow else throw e
        }
        store.putString(idsKey, fresh.joinToString(",") { it.id })
        store.putString(fetchedAtKey, nowMillis().toString())
        emit(fresh)
    }

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
