package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.Estate
import com.eleazar.localhive.domain.model.JoinMode
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
     * Takes [userId] out of the estate. An admin also loses admin, a treasurer stops being
     * treasurer, and the owner hands ownership
     * to another admin, and fails with [OwnerMustHandOverException] if there's nobody to take it.
     */
    suspend fun leaveEstate(estateId: String, userId: String): Result<Unit>
    suspend fun searchEstates(query: String): Result<List<Estate>>
    /** An existing estate with the same name (any case) at the same address, or null. */
    suspend fun findSimilarEstate(name: String, address: String): Result<Estate?>
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
    /**
     * An admin appointing [userId] as the estate's one treasurer, replacing any other; null
     * leaves the estate without one.
     */
    suspend fun setTreasurer(estateId: String, userId: String?): Result<Unit>

    /** An admin renaming the estate or correcting its address. */
    suspend fun updateEstateDetails(estateId: String, name: String, address: String): Result<Unit>
    suspend fun setJoinMode(estateId: String, mode: JoinMode): Result<Unit>
    /** The current invite code, live; null for anyone who isn't a member or admin. */
    fun observeInviteCode(estateId: String): Flow<String?>
    /** Retires the current invite code, so anyone holding it can no longer join, and returns the new one. */
    suspend fun regenerateInviteCode(estateId: String): Result<String>
    /**
     * The owner making [newOwnerId], who must be a member, the owner. They're made an admin if
     * they aren't one; the old owner stays an admin.
     */
    suspend fun transferOwnership(estateId: String, newOwnerId: String): Result<Unit>
}

/** The owner tried to leave while other members remain but no other admin can take over. */
class OwnerMustHandOverException : Exception("Make another member an admin before you leave, so the estate isn't left without one.")
