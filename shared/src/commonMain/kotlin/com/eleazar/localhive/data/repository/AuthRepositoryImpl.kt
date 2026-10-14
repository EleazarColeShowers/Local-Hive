package com.eleazar.localhive.data.repository

import com.eleazar.localhive.data.remote.ApiException
import com.eleazar.localhive.data.remote.LocalHiveApi
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.model.User
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.withTimeoutOrNull

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val api: LocalHiveApi
) : AuthRepository {

    override suspend fun signup(email: String, username: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password)
            val firebaseUser = result.user ?: throw Exception("User is null")
            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email,
                username = username
            )
            firestore.users().document(user.id).set(user.toFullMap())
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password)
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

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        // So pushes for this account stop coming to a phone someone else may sign in on next.
        // Must happen while still signed in; offline, it gives up rather than hold up logging out.
        firebaseAuth.currentUser?.uid?.let { uid ->
            withTimeoutOrNull(5_000) {
                runCatching {
                    firestore.users().document(uid).update("fcmToken" to FieldValue.delete)
                }
            }
        }
        firebaseAuth.signOut()
    }

    override suspend fun deleteAccount(password: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser ?: throw Exception("You're not signed in")
            val email = user.email ?: throw Exception("This account has no email to confirm with")
            try {
                user.reauthenticate(EmailAuthProvider.credential(email, password))
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                throw Exception("That password isn't right")
            }
            try {
                api.deleteAccount()
            } catch (e: ApiException) {
                throw Exception(e.message)
            }
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid

    override suspend fun getUserProfile(userId: String): Result<User> {
        return try {
            val doc = firestore.users().document(userId).get()
            if (!doc.exists) throw Exception("User profile not found")
            Result.success(doc.toUser())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveUserProfile(user: User): Result<Unit> {
        return try {
            firestore.users().document(user.id).set(user.toNonNullMap(), merge = true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun FirebaseUser.toUser() = User(id = uid, email = email, displayName = displayName)

    private fun User.toFullMap(): Map<String, Any?> = mapOf(
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
}
