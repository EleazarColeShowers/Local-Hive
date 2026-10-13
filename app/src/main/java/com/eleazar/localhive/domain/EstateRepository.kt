package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.Estate
import com.eleazar.localhive.domain.model.JoinRequest
import kotlinx.coroutines.flow.Flow

interface EstateRepository {
    suspend fun createEstate(name: String, address: String, adminId: String): Result<Estate>
    suspend fun joinEstate(inviteCode: String, userId: String): Result<Estate>
    suspend fun getEstate(estateId: String): Result<Estate>
    /** True or false when the server says so; null when it couldn't be reached. */
    suspend fun estateExists(estateId: String): Boolean?
    /** Live estate document (without the invite code); null once it's gone or unreadable. */
    fun observeEstate(estateId: String): Flow<Estate?>
    /**
     * Takes [userId] out of the estate. An admin also loses admin; the owner hands ownership
     * to another admin, and fails with [OwnerMustHandOverException] if there's nobody to take it.
     */
    suspend fun leaveEstate(estateId: String, userId: String): Result<Unit>
    suspend fun searchEstates(query: String): Result<List<Estate>>
    suspend fun requestToJoin(estateId: String, userId: String, userName: String, userPhotoUrl: String): Result<Unit>
    suspend fun cancelRequest(estateId: String, userId: String): Result<Unit>
    fun getPendingRequests(estateId: String): Flow<List<JoinRequest>>
    fun getUserJoinRequest(userId: String): Flow<JoinRequest?>
    suspend fun approveRequest(estateId: String, userId: String): Result<Unit>
    suspend fun denyRequest(estateId: String, userId: String): Result<Unit>
    suspend fun promoteToAdmin(estateId: String, userId: String): Result<Unit>
    /** Fails for the owner, who stays an admin for as long as they're in the estate. */
    suspend fun removeAdmin(estateId: String, userId: String): Result<Unit>
    /** An admin taking [userId] out of the estate. Admins have to lose admin first. */
    suspend fun removeMember(estateId: String, userId: String): Result<Unit>
}

/** The owner tried to leave while other members remain but no other admin can take over. */
class OwnerMustHandOverException : Exception("Make another member an admin before you leave, so the estate isn't left without one.")
