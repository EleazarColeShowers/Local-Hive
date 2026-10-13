package com.eleazar.localhive.data.repository

import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.NotificationRepository
import com.eleazar.localhive.domain.OwnerMustHandOverException
import com.eleazar.localhive.domain.model.AppNotification
import com.eleazar.localhive.domain.model.Estate
import com.eleazar.localhive.domain.model.JoinRequest
import com.eleazar.localhive.domain.model.NotificationType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Invite codes are kept off the estate document, which anyone signed in can read so that
 * estates can be found and requested. Instead each estate has `estates/{id}/private/invite`
 * (members and admins only) and a lookup at `inviteCodes/{code}` that can be fetched only by
 * someone who already knows the code. Firestore rules check these, so joining and approval
 * are written as batches the rules can verify in one go.
 */
class EstateRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val notificationRepository: NotificationRepository
) : EstateRepository {

    override suspend fun createEstate(name: String, address: String, adminId: String): Result<Estate> {
        return try {
            val inviteCode = unusedInviteCode()
            val ref = firestore.collection("estates").document()
            val estate = Estate(
                id = ref.id,
                name = name,
                address = address,
                adminIds = listOf(adminId),
                ownerId = adminId,
                inviteCode = inviteCode,
                memberCount = 1,
                createdAt = System.currentTimeMillis()
            )
            firestore.batch()
                .set(ref, estate.toMap())
                .set(inviteRef(ref.id), mapOf("code" to inviteCode))
                .set(firestore.collection("inviteCodes").document(inviteCode), mapOf("estateId" to ref.id))
                .set(firestore.collection("users").document(adminId), mapOf("estateId" to ref.id), SetOptions.merge())
                .commit()
                .await()
            Result.success(estate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinEstate(inviteCode: String, userId: String): Result<Estate> {
        return try {
            val code = inviteCode.trim().uppercase()
            if (code.isEmpty()) throw Exception("Enter an invite code")
            val lookup = firestore.collection("inviteCodes").document(code).get().await()
            val estateId = lookup.getString("estateId") ?: throw Exception("That invite code isn't valid")
            val estate = mapDocToEstate(firestore.collection("estates").document(estateId).get().await())
                ?: throw Exception("Estate not found")

            // The approved request carrying the code is what the rules accept as permission
            // to set estateId, so all three writes must land together.
            firestore.batch()
                .set(
                    requestRef(estateId, userId),
                    mapOf(
                        "id" to "${estateId}_${userId}",
                        "userId" to userId,
                        "estateId" to estateId,
                        "estateName" to estate.name,
                        "requestedAt" to System.currentTimeMillis(),
                        "status" to "approved",
                        "inviteCode" to code
                    )
                )
                .set(firestore.collection("users").document(userId), mapOf("estateId" to estateId), SetOptions.merge())
                .update(firestore.collection("estates").document(estateId), "memberCount", FieldValue.increment(1))
                .commit()
                .await()
            Result.success(estate.copy(memberCount = estate.memberCount + 1, inviteCode = code))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEstate(estateId: String): Result<Estate> {
        return try {
            val doc = firestore.collection("estates").document(estateId).get().await()
            if (!doc.exists()) throw Exception("Estate not found")
            val estate = mapDocToEstate(doc) ?: throw Exception("Estate not found")
            // Readable by members and admins only; anyone else just gets the estate without it.
            val code = runCatching { inviteRef(estateId).get().await().getString("code") }.getOrNull()
            Result.success(if (code != null) estate.copy(inviteCode = code) else estate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun estateExists(estateId: String): Boolean? = try {
        firestore.collection("estates").document(estateId).get(Source.SERVER).await().exists()
    } catch (e: Exception) {
        null
    }

    override fun observeEstate(estateId: String): Flow<Estate?> = callbackFlow {
        val sub = firestore.collection("estates").document(estateId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(null); return@addSnapshotListener }
                trySend(snapshot?.takeIf { it.exists() }?.let { mapDocToEstate(it) })
            }
        awaitClose { sub.remove() }
    }

    override suspend fun leaveEstate(estateId: String, userId: String): Result<Unit> {
        return try {
            val estateRef = firestore.collection("estates").document(estateId)
            val estate = mapDocToEstate(estateRef.get(Source.SERVER).await())

            val estateUpdates = mutableMapOf<String, Any>("memberCount" to FieldValue.increment(-1))
            if (estate != null && estate.isOwner(userId)) {
                val successor = estate.adminIds.firstOrNull { it != userId }
                when {
                    successor != null -> {
                        estateUpdates["ownerId"] = successor
                        estateUpdates["adminIds"] = FieldValue.arrayRemove(userId)
                    }
                    // Last one out keeps admin, so they can come straight back to an empty estate.
                    estate.memberCount > 1 -> throw OwnerMustHandOverException()
                }
            } else if (estate != null && estate.isAdmin(userId)) {
                estateUpdates["adminIds"] = FieldValue.arrayRemove(userId)
            }

            // Dropping the request too means coming back needs a fresh approval or code.
            firestore.batch()
                .update(firestore.collection("users").document(userId), "estateId", FieldValue.delete())
                .update(estateRef, estateUpdates)
                .delete(requestRef(estateId, userId))
                .commit()
                .await()
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
            requestRef(estateId, userId).set(data).await()
            estate.adminIds.forEach { adminId ->
                notificationRepository.createNotification(
                    AppNotification(
                        recipientId = adminId,
                        type = NotificationType.JOIN_REQUEST,
                        title = "New join request",
                        message = "${userName.ifBlank { "Someone" }} wants to join ${estate.name}",
                        estateId = estateId,
                        relatedUserId = userId
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelRequest(estateId: String, userId: String): Result<Unit> {
        return try {
            requestRef(estateId, userId).delete().await()
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
            // The rules let an admin set a requester's estateId only alongside approving
            // that same request, so these go in one batch.
            firestore.batch()
                .update(requestRef(estateId, userId), mapOf("status" to "approved", "reviewedAt" to System.currentTimeMillis()))
                .update(firestore.collection("users").document(userId), "estateId", estateId)
                .update(firestore.collection("estates").document(estateId), "memberCount", FieldValue.increment(1))
                .commit()
                .await()
            val estate = getEstate(estateId).getOrNull()
            notificationRepository.createNotification(
                AppNotification(
                    recipientId = userId,
                    type = NotificationType.REQUEST_APPROVED,
                    title = "Request approved",
                    message = "Your request to join ${estate?.name ?: "the estate"} has been approved",
                    estateId = estateId
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun denyRequest(estateId: String, userId: String): Result<Unit> {
        return try {
            requestRef(estateId, userId)
                .update(mapOf("status" to "denied", "reviewedAt" to System.currentTimeMillis())).await()
            val estate = getEstate(estateId).getOrNull()
            notificationRepository.createNotification(
                AppNotification(
                    recipientId = userId,
                    type = NotificationType.REQUEST_DENIED,
                    title = "Request denied",
                    message = "Your request to join ${estate?.name ?: "the estate"} was denied",
                    estateId = estateId
                )
            )
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

    override suspend fun removeAdmin(estateId: String, userId: String): Result<Unit> {
        return try {
            val estate = getEstate(estateId).getOrThrow()
            if (estate.isOwner(userId)) throw Exception("The estate's owner can't have admin removed")
            firestore.collection("estates").document(estateId)
                .update("adminIds", FieldValue.arrayRemove(userId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeMember(estateId: String, userId: String): Result<Unit> {
        return try {
            val estate = mapDocToEstate(firestore.collection("estates").document(estateId).get(Source.SERVER).await())
                ?: throw Exception("Estate not found")
            if (estate.isAdmin(userId)) throw Exception("Remove their admin role first")
            // Dropping the request too means coming back needs a fresh approval or code.
            firestore.batch()
                .update(firestore.collection("users").document(userId), "estateId", FieldValue.delete())
                .update(firestore.collection("estates").document(estateId), "memberCount", FieldValue.increment(-1))
                .delete(requestRef(estateId, userId))
                .commit()
                .await()
            notificationRepository.createNotification(
                AppNotification(
                    recipientId = userId,
                    type = NotificationType.MEMBER_REMOVED,
                    title = "Removed from ${estate.name}",
                    message = "An admin removed you from ${estate.name}. You can ask to join again or join another estate.",
                    estateId = estateId
                )
            )
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
                // Estates from before ownership was tracked: their first admin created them.
                ownerId = doc.getString("ownerId") ?: adminIds.firstOrNull().orEmpty(),
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

    private fun requestRef(estateId: String, userId: String) =
        firestore.collection("joinRequests").document("${estateId}_${userId}")

    private fun inviteRef(estateId: String) =
        firestore.collection("estates").document(estateId).collection("private").document("invite")

    /** A code no other estate holds; codes can't be overwritten, so a clash would fail the create. */
    private suspend fun unusedInviteCode(): String {
        repeat(5) {
            val code = generateInviteCode()
            if (!firestore.collection("inviteCodes").document(code).get().await().exists()) return code
        }
        throw Exception("Couldn't generate an invite code, please try again")
    }

    private fun generateInviteCode(): String {
        // No 0/O or 1/I, so a code read aloud or off a screen can't be mistyped.
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun Estate.toMap() = mapOf(
        "name" to name,
        "address" to address,
        "adminIds" to adminIds,
        "ownerId" to ownerId,
        "memberCount" to memberCount,
        "createdAt" to createdAt
    )
}
