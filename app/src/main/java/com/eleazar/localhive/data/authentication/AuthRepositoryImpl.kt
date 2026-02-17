package com.eleazar.localhive.data.authentication

import com.google.firebase.auth.FirebaseAuth
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.model.User
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth
): AuthRepository {
    override suspend fun signup(
        email: String,
        password: String
    ): Result<User> {
        TODO("Not yet implemented")
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user?.toUser() ?: throw Exception("User is null")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    private fun FirebaseUser.toUser(): User {
        return User(
            id = uid,
            email = email,
            displayName = displayName
        )
    }


}