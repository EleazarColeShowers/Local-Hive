package com.eleazar.localhive.data.authentication

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun signup(email: String, username: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("User is null")
            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email,
                username = username
            )
            firestore.collection("users").document(user.id).set(user.toFullMap()).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("User is null")
            val profileResult = getUserProfile(firebaseUser.uid)
            profileResult.fold(
                onSuccess = { Result.success(it) },
                onFailure = { Result.success(firebaseUser.toUser()) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    override fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid

    override suspend fun getUserProfile(userId: String): Result<User> {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            val user = doc.toObject(User::class.java)?.copy(id = doc.id)
                ?: throw Exception("User profile not found")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveUserProfile(user: User): Result<Unit> {
        return try {
            firestore.collection("users").document(user.id)
                .set(user.toNonNullMap(), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun FirebaseUser.toUser() = User(id = uid, email = email, displayName = displayName)

    private fun User.toFullMap() = mapOf(
        "email" to email,
        "displayName" to displayName,
        "username" to username,
        "profileImageUrl" to profileImageUrl,
        "bio" to bio,
        "estateId" to estateId,
        "address" to address,
        "occupation" to occupation,
        "phone" to phone
    )

    private fun User.toNonNullMap(): Map<String, Any> = buildMap {
        email?.let { put("email", it) }
        displayName?.let { put("displayName", it) }
        username?.let { put("username", it) }
        profileImageUrl?.let { put("profileImageUrl", it) }
        bio?.let { put("bio", it) }
        estateId?.let { put("estateId", it) }
        address?.let { put("address", it) }
        occupation?.let { put("occupation", it) }
        phone?.let { put("phone", it) }
    }
}
