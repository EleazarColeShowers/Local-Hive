package com.eleazar.localhive.data.repository

import com.eleazar.localhive.data.local.dao.UserDao
import com.eleazar.localhive.data.local.mapper.toDomain
import com.eleazar.localhive.data.local.mapper.toEntity
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao
) : UserRepository {

    override suspend fun getUser(userId: String): Result<User> {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            val user = doc.toObject(User::class.java)?.copy(id = doc.id)
                ?: throw Exception("User not found")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getEstateMembers(estateId: String): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .whereEqualTo("estateId", estateId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(users)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun updateUser(user: User): Result<Unit> {
        return try {
            firestore.collection("users").document(user.id)
                .set(user.toNonNullMap(), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveUserLocally(user: User) {
        userDao.insertUser(user.toEntity())
    }

    override suspend fun getLocalUser(userId: String): User? {
        return userDao.getUser(userId)?.toDomain()
    }

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
