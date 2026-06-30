package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUser(userId: String): Result<User>
    fun getEstateMembers(estateId: String): Flow<List<User>>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun saveUserLocally(user: User)
    suspend fun getLocalUser(userId: String): User?
}
