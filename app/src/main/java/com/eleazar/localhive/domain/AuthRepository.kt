package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.User

interface AuthRepository {
    suspend fun signup(email: String, username: String, password: String): Result<User>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun logout()
    fun getCurrentUserId(): String?
    suspend fun getUserProfile(userId: String): Result<User>
    suspend fun saveUserProfile(user: User): Result<Unit>
}
