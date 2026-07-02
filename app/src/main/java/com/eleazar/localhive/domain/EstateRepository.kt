package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.Estate

interface EstateRepository {
    suspend fun createEstate(name: String, address: String, adminId: String): Result<Estate>
    suspend fun joinEstate(inviteCode: String, userId: String): Result<Estate>
    suspend fun getEstate(estateId: String): Result<Estate>
    suspend fun leaveEstate(estateId: String, userId: String): Result<Unit>
}
