package com.eleazar.localhive.data.repository

import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.model.Estate
import com.eleazar.localhive.domain.model.JoinRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
                adminIds = listOf(adminId),
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
            val estate = mapDocToEstate(doc) ?: throw Exception("Estate not found")
            firestore.collection("estates").document(estate.id)
                .update("memberCount", FieldValue.increment(1)).await()
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
            val estate = mapDocToEstate(doc) ?: throw Exception("Estate not found")
            Result.success(estate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveEstate(estateId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .update("estateId", FieldValue.delete()).await()
            firestore.collection("estates").document(estateId)
                .update("memberCount", FieldValue.increment(-1)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchEstates(query: String): Result<List<Estate>> {
        return try {
            val snapshot = if (query.isBlank()) {
                firestore.collection("estates").orderBy("name").limit(20).get().await()
            } else {
                val end = query.trimEnd() + ""
                firestore.collection("estates")
                    .orderBy("name")
                    .startAt(query)
                    .endAt(end)
                    .limit(20)
                    .get().await()
            }
            val estates = snapshot.documents.mapNotNull { mapDocToEstate(it) }
            Result.success(estates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun requestToJoin(
        estateId: String,
        userId: String,
        userName: String,
        userPhotoUrl: String
    ): Result<Unit> {
        return try {
            val estate = getEstate(estateId).getOrThrow()
            val docId = "${estateId}_${userId}"
            val data = mapOf(
                "id" to docId,
                "userId" to userId,
                "userName" to userName,
                "userPhotoUrl" to userPhotoUrl,
                "estateId" to estateId,
                "estateName" to estate.name,
                "requestedAt" to System.currentTimeMillis(),
                "status" to "pending"
            )
            firestore.collection("joinRequests").document(docId).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelRequest(estateId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection("joinRequests").document("${estateId}_${userId}").delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getPendingRequests(estateId: String): Flow<List<JoinRequest>> = callbackFlow {
        val sub = firestore.collection("joinRequests")
            .whereEqualTo("estateId", estateId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val requests = snapshot?.documents?.mapNotNull { mapDocToRequest(it) } ?: emptyList()
                trySend(requests)
            }
        awaitClose { sub.remove() }
    }

    override fun getUserJoinRequest(userId: String): Flow<JoinRequest?> = callbackFlow {
        val sub = firestore.collection("joinRequests")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "pending")
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(null); return@addSnapshotListener }
                val request = snapshot?.documents?.firstOrNull()?.let { mapDocToRequest(it) }
                trySend(request)
            }
        awaitClose { sub.remove() }
    }

    override suspend fun approveRequest(estateId: String, userId: String): Result<Unit> {
        return try {
            val docId = "${estateId}_${userId}"
            firestore.collection("joinRequests").document(docId)
                .update("status", "approved").await()
            firestore.collection("estates").document(estateId)
                .update("memberCount", FieldValue.increment(1)).await()
            firestore.collection("users").document(userId)
                .set(mapOf("estateId" to estateId), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun denyRequest(estateId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection("joinRequests").document("${estateId}_${userId}")
                .update("status", "denied").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun promoteToAdmin(estateId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection("estates").document(estateId)
                .update("adminIds", FieldValue.arrayUnion(userId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapDocToEstate(doc: DocumentSnapshot): Estate? {
        return try {
            val adminIds = (doc.get("adminIds") as? List<*>)?.filterIsInstance<String>()
                ?: listOfNotNull(doc.getString("adminId"))
            Estate(
                id = doc.id,
                name = doc.getString("name") ?: "",
                address = doc.getString("address") ?: "",
                adminIds = adminIds,
                inviteCode = doc.getString("inviteCode") ?: "",
                memberCount = doc.getLong("memberCount")?.toInt() ?: 0,
                createdAt = doc.getLong("createdAt") ?: 0L
            )
        } catch (e: Exception) { null }
    }

    private fun mapDocToRequest(doc: DocumentSnapshot): JoinRequest? {
        return try {
            JoinRequest(
                id = doc.id,
                userId = doc.getString("userId") ?: "",
                userName = doc.getString("userName") ?: "",
                userPhotoUrl = doc.getString("userPhotoUrl") ?: "",
                estateId = doc.getString("estateId") ?: "",
                estateName = doc.getString("estateName") ?: "",
                requestedAt = doc.getLong("requestedAt") ?: 0L,
                status = doc.getString("status") ?: "pending"
            )
        } catch (e: Exception) { null }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun Estate.toMap() = mapOf(
        "name" to name,
        "address" to address,
        "adminIds" to adminIds,
        "inviteCode" to inviteCode,
        "memberCount" to memberCount,
        "createdAt" to createdAt
    )
}
