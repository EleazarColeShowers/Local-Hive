package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.BlockedUser
import com.eleazar.localhive.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /**
     * [cacheFirst] answers from the on-device Firestore cache when the profile is there,
     * skipping the network round trip; use it where slightly stale data is fine.
     */
    suspend fun getUser(userId: String, cacheFirst: Boolean = false): Result<User>
    fun observeUser(userId: String): Flow<User?>
    /** Every member, live. Each time it starts it reads them all from the server; prefer [observeDirectory]. */
    fun getEstateMembers(estateId: String): Flow<List<User>>
    /**
     * Every member, from this phone's saved copy when there is one, refreshed from the server only
     * when the copy is over a few hours old, looks incomplete, or [forceRefresh] is set. Slightly
     * stale is fine for browsing neighbours, and it saves reading the whole estate on every visit.
     */
    fun observeDirectory(estateId: String, forceRefresh: Boolean = false): Flow<List<User>>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun updateFcmToken(userId: String, token: String): Result<Unit>
    /** Whether the user gets a push when a neighbour reports the light is back. */
    suspend fun setLightAlerts(userId: String, enabled: Boolean): Result<Unit>
    /** Stops pushes for [userId] reaching this device, e.g. when they log out of it. */
    suspend fun clearFcmToken(userId: String): Result<Unit>
    /** People [userId] has blocked, newest first. */
    fun observeBlockedUsers(userId: String): Flow<List<BlockedUser>>
    suspend fun blockUser(userId: String, blocked: BlockedUser): Result<Unit>
    suspend fun unblockUser(userId: String, blockedId: String): Result<Unit>
}
