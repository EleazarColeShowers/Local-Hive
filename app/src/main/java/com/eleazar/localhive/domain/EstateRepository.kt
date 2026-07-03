package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.Estate
import com.eleazar.localhive.domain.model.JoinRequest
import kotlinx.coroutines.flow.Flow

interface EstateRepository {
    suspend fun createEstate(name: String, address: String, adminId: String): Result<Estate>
    suspend fun joinEstate(inviteCode: String, userId: String): Result<Estate>
    suspend fun getEstate(estateId: String): Result<Estate>
    suspend fun leaveEstate(estateId: String, userId: String): Result<Unit>
    suspend fun searchEstates(query: String): Result<List<Estate>>
    suspend fun requestToJoin(estateId: String, userId: String, userName: String, userPhotoUrl: String): Result<Unit>
    suspend fun cancelRequest(estateId: String, userId: String): Result<Unit>
    fun getPendingRequests(estateId: String): Flow<List<JoinRequest>>
    fun getUserJoinRequest(userId: String): Flow<JoinRequest?>
    suspend fun approveRequest(estateId: String, userId: String): Result<Unit>
    suspend fun denyRequest(estateId: String, userId: String): Result<Unit>
    suspend fun promoteToAdmin(estateId: String, userId: String): Result<Unit>
}
