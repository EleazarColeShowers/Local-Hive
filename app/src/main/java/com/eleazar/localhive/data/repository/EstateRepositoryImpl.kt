package com.eleazar.localhive.data.repository

import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.model.Estate
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class EstateRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : EstateRepository {

    override suspend fun createEstate(name: String, address: String, adminId: String): Result<Estate> {
        return try {
            val inviteCode = generateInviteCode()
            val ref = firestore.collection("estates").document()
            val estate = Estate(
                id = ref.id,
                name = name,
                address = address,
                adminId = adminId,
                inviteCode = inviteCode,
                memberCount = 1,
                createdAt = System.currentTimeMillis()
            )
            ref.set(estate.toMap()).await()
            firestore.collection("users").document(adminId)
                .set(mapOf("estateId" to ref.id), SetOptions.merge()).await()
            Result.success(estate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinEstate(inviteCode: String, userId: String): Result<Estate> {
        return try {
            val query = firestore.collection("estates")
                .whereEqualTo("inviteCode", inviteCode.uppercase())
                .limit(1)
                .get().await()
            val doc = query.documents.firstOrNull() ?: throw Exception("Invalid invite code")
            val estate = doc.toObject(Estate::class.java)?.copy(id = doc.id)
                ?: throw Exception("Estate not found")
            firestore.collection("estates").document(estate.id)
                .update("memberCount", estate.memberCount + 1).await()
            firestore.collection("users").document(userId)
                .set(mapOf("estateId" to estate.id), SetOptions.merge()).await()
            Result.success(estate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEstate(estateId: String): Result<Estate> {
        return try {
            val doc = firestore.collection("estates").document(estateId).get().await()
            val estate = doc.toObject(Estate::class.java)?.copy(id = doc.id)
                ?: throw Exception("Estate not found")
            Result.success(estate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun Estate.toMap() = mapOf(
        "name" to name,
        "address" to address,
        "adminId" to adminId,
        "inviteCode" to inviteCode,
        "memberCount" to memberCount,
        "createdAt" to createdAt
    )
}
