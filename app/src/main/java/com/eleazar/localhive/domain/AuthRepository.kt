package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.User

interface AuthRepository {
    suspend fun signup(email: String, username: String, password: String): Result<User>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun logout()
    /**
     * Permanently deletes the signed-in account and everything the user made, after checking
     * [password]. Signs out on success.
     */
    suspend fun deleteAccount(password: String): Result<Unit>
    fun getCurrentUserId(): String?
    suspend fun getUserProfile(userId: String): Result<User>
    suspend fun saveUserProfile(user: User): Result<Unit>
}
