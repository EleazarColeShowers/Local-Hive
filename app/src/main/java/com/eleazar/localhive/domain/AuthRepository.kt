package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.User

interface AuthRepository {
    suspend fun signup(email: String, password: String): Result<User>
    suspend fun login(email: String, password: String): Result<User>
}