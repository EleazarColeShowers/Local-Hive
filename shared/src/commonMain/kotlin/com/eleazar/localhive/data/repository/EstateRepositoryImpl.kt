package com.eleazar.localhive.data.repository

import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.NotificationRepository
import com.eleazar.localhive.domain.OwnerMustHandOverException
import com.eleazar.localhive.domain.model.AppNotification
import com.eleazar.localhive.domain.model.Estate
import com.eleazar.localhive.domain.model.JoinMode
import com.eleazar.localhive.domain.model.JoinRequest
import com.eleazar.localhive.domain.model.NotificationType
import com.eleazar.localhive.ui.util.nowMillis
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Invite codes are kept off the estate document, which anyone signed in can read so that
 * estates can be found and requested. Instead each estate has `estates/{id}/private/invite`
 * (members and admins only) and a lookup at `inviteCodes/{code}` that can be fetched only by
 * someone who already knows the code. Firestore rules check these, so joining and approval
 * are written as batches the rules can verify in one go.
 */
class EstateRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val notificationRepository: NotificationRepository
) : EstateRepository {

    private val estates get() = firestore.collection("estates")
    private val inviteCodes get() = firestore.collection("inviteCodes")

    override suspend fun createEstate(name: String, address: String, adminId: String): Result<Estate> = runCatching {
        val inviteCode = unusedInviteCode()
        val ref = estates.document
        val estate = Estate(
            id = ref.id,
            name = name.trim(),
            address = address,
            adminIds = listOf(adminId),
            ownerId = adminId,
            inviteCode = inviteCode,
            memberCount = 1,
            createdAt = nowMillis()
        )
        firestore.batch()
            .set(ref, estate.toMap())
            .set(inviteRef(ref.id), mapOf("code" to inviteCode))
            .set(inviteCodes.document(inviteCode), mapOf("estateId" to ref.id))
            .set(firestore.users().document(adminId), mapOf("estateId" to ref.id), merge = true)
            .commit()
        estate
    }

    override suspend fun joinEstate(inviteCode: String, userId: String): Result<Estate> = runCatching {
        val code = inviteCode.trim().uppercase()
        if (code.isEmpty()) throw Exception("Enter an invite code")
        val lookup = inviteCodes.document(code).get()
        val estateId = lookup.takeIf { it.exists }?.string("estateId") ?: throw Exception("That invite code isn't valid")
        val estate = mapDocToEstate(estates.document(estateId).get()) ?: throw Exception("Estate not found")
        if (!estate.joinMode.acceptsCodes) {
            throw Exception("${estate.name} isn't taking invite codes right now. Find it and ask to join instead.")
        }

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
                    "requestedAt" to nowMillis(),
                    "status" to "approved",
                    "inviteCode" to code
                )
            )
            .set(firestore.users().document(userId), mapOf("estateId" to estateId), merge = true)
            .update(estates.document(estateId), "memberCount" to FieldValue.increment(1))
            .commit()
        estate.copy(memberCount = estate.memberCount + 1, inviteCode = code)
    }

    override suspend fun getEstate(estateId: String): Result<Estate> = runCatching {
        val doc = estates.document(estateId).get()
        if (!doc.exists) throw Exception("Estate not found")
        val estate = mapDocToEstate(doc) ?: throw Exception("Estate not found")
        // Readable by members and admins only; anyone else just gets the estate without it.
        val code = runCatching { inviteRef(estateId).get().string("code") }.getOrNull()
        if (code != null) estate.copy(inviteCode = code) else estate
    }

    override suspend fun estateExists(estateId: String): Boolean? = try {
        estates.document(estateId).get(Source.SERVER).exists
    } catch (e: Exception) {
        null
    }

    override fun observeEstate(estateId: String): Flow<Estate?> =
        estates.document(estateId).snapshots
            .map { snapshot -> snapshot.takeIf { it.exists }?.let { mapDocToEstate(it) } }
            .catch { emit(null) }

    override suspend fun leaveEstate(estateId: String, userId: String): Result<Unit> = runCatching {
        val estateRef = estates.document(estateId)
        val estate = estateRef.get(Source.SERVER).takeIf { it.exists }?.let { mapDocToEstate(it) }

        val estateUpdates = mutableListOf<Pair<String, Any?>>("memberCount" to FieldValue.increment(-1))
        if (estate != null && estate.isOwner(userId)) {
            val successor = estate.adminIds.firstOrNull { it != userId }
            when {
                successor != null -> {
                    estateUpdates += "ownerId" to successor
                    estateUpdates += "adminIds" to FieldValue.arrayRemove(userId)
                }
                // Last one out keeps admin, so they can come straight back to an empty estate.
                estate.memberCount > 1 -> throw OwnerMustHandOverException()
            }
        } else if (estate != null && estate.isAdmin(userId)) {
            estateUpdates += "adminIds" to FieldValue.arrayRemove(userId)
        }
        if (estate != null && estate.isTreasurer(userId)) estateUpdates += "treasurerId" to ""

        // Dropping the request too means coming back needs a fresh approval or code.
        firestore.batch()
            .update(firestore.users().document(userId), "estateId" to FieldValue.delete)
            .update(estateRef, *estateUpdates.toTypedArray())
            .delete(requestRef(estateId, userId))
            .commit()
    }

    override suspend fun searchEstates(query: String): Result<List<Estate>> = runCatching {
        // Searches nameLower so "sunrise" finds "Sunrise Estate".
        val prefix = query.trim().lowercase()
        val snapshot = if (prefix.isEmpty()) {
            estates.orderBy("nameLower").limit(20).get()
        } else {
            estates.orderBy("nameLower").startAt(prefix).endAt(prefix + "\uf8ff").limit(20).get()
        }
        snapshot.documents.mapNotNull { mapDocToEstate(it) }
    }

    override suspend fun findSimilarEstate(name: String, address: String): Result<Estate?> = runCatching {
        val wanted = address.comparableAddress()
        estates.where { "nameLower" equalTo name.trim().lowercase() }.limit(10).get()
            .documents.mapNotNull { mapDocToEstate(it) }
            .firstOrNull { it.address.comparableAddress().sameAddressAs(wanted) }
    }

    override suspend fun requestToJoin(
        estateId: String,
        userId: String,
        userName: String,
        userPhotoUrl: String
    ): Result<Unit> = runCatching {
        val estate = getEstate(estateId).getOrThrow()
        if (!estate.joinMode.acceptsRequests) {
            throw Exception("${estate.name} is invite only. Ask a member for the invite code.")
        }
        requestRef(estateId, userId).set(
            mapOf(
                "id" to "${estateId}_${userId}",
                "userId" to userId,
                "userName" to userName,
                "userPhotoUrl" to userPhotoUrl,
                "estateId" to estateId,
                "estateName" to estate.name,
                "requestedAt" to nowMillis(),
                "status" to "pending"
            )
        )
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
    }

    override suspend fun cancelRequest(estateId: String, userId: String): Result<Unit> = runCatching {
        requestRef(estateId, userId).delete()
    }

    override fun getPendingRequests(estateId: String): Flow<List<JoinRequest>> =
        firestore.collection("joinRequests")
            .where { "estateId" equalTo estateId }
            .where { "status" equalTo "pending" }
            .snapshots
            .map { snapshot -> snapshot.documents.mapNotNull { mapDocToRequest(it) } }
            .catch { emit(emptyList()) }

    override fun getUserJoinRequest(userId: String): Flow<JoinRequest?> =
        firestore.collection("joinRequests")
            .where { "userId" equalTo userId }
            .where { "status" equalTo "pending" }
            .limit(1)
            .snapshots
            .map { snapshot -> snapshot.documents.firstOrNull()?.let { mapDocToRequest(it) } }
            .catch { emit(null) }

    override suspend fun approveRequest(estateId: String, userId: String): Result<Unit> = runCatching {
        // The rules let an admin set a requester's estateId only alongside approving
        // that same request, so these go in one batch.
        firestore.batch()
            .update(requestRef(estateId, userId), "status" to "approved", "reviewedAt" to nowMillis())
            .update(firestore.users().document(userId), "estateId" to estateId)
            .update(estates.document(estateId), "memberCount" to FieldValue.increment(1))
            .commit()
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
    }

    override suspend fun denyRequest(estateId: String, userId: String): Result<Unit> = runCatching {
        requestRef(estateId, userId).update("status" to "denied", "reviewedAt" to nowMillis())
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
    }

    override suspend fun promoteToAdmin(estateId: String, userId: String): Result<Unit> = runCatching {
        estates.document(estateId).update("adminIds" to FieldValue.arrayUnion(userId))
    }

    override suspend fun removeAdmin(estateId: String, userId: String): Result<Unit> = runCatching {
        val estate = getEstate(estateId).getOrThrow()
        if (estate.isOwner(userId)) throw Exception("The estate's owner can't have admin removed")
        estates.document(estateId).update("adminIds" to FieldValue.arrayRemove(userId))
    }

    override suspend fun removeMember(estateId: String, userId: String): Result<Unit> = runCatching {
        val estate = estates.document(estateId).get(Source.SERVER).takeIf { it.exists }?.let { mapDocToEstate(it) }
            ?: throw Exception("Estate not found")
        if (estate.isAdmin(userId)) throw Exception("Remove their admin role first")
        val estateUpdates = mutableListOf<Pair<String, Any?>>("memberCount" to FieldValue.increment(-1))
        if (estate.isTreasurer(userId)) estateUpdates += "treasurerId" to ""
        // Dropping the request too means coming back needs a fresh approval or code.
        firestore.batch()
            .update(firestore.users().document(userId), "estateId" to FieldValue.delete)
            .update(estates.document(estateId), *estateUpdates.toTypedArray())
            .delete(requestRef(estateId, userId))
            .commit()
        notificationRepository.createNotification(
            AppNotification(
                recipientId = userId,
                type = NotificationType.MEMBER_REMOVED,
                title = "Removed from ${estate.name}",
                message = "An admin removed you from ${estate.name}. You can ask to join again or join another estate.",
                estateId = estateId
            )
        )
    }

    override suspend fun setTreasurer(estateId: String, userId: String?): Result<Unit> = runCatching {
        estates.document(estateId).update("treasurerId" to userId.orEmpty())
    }

    override suspend fun updateEstateDetails(estateId: String, name: String, address: String): Result<Unit> = runCatching {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw Exception("Give the estate a name")
        estates.document(estateId).update(
            "name" to trimmed,
            "nameLower" to trimmed.lowercase(),
            "address" to address.trim()
        )
    }

    override suspend fun setJoinMode(estateId: String, mode: JoinMode): Result<Unit> = runCatching {
        estates.document(estateId).update("joinMode" to mode.key)
    }

    override fun observeInviteCode(estateId: String): Flow<String?> =
        inviteRef(estateId).snapshots
            .map { snapshot -> snapshot.takeIf { it.exists }?.string("code") }
            .catch { emit(null) }

    override suspend fun regenerateInviteCode(estateId: String): Result<String> = runCatching {
        val old = runCatching { inviteRef(estateId).get(Source.SERVER).string("code") }.getOrNull()
        val code = unusedInviteCode()
        // The new lookup and the estate's copy of it land together, and the old lookup goes in
        // the same batch, so there's never a moment with two working codes or none.
        val batch = firestore.batch()
            .set(inviteRef(estateId), mapOf("code" to code))
            .set(inviteCodes.document(code), mapOf("estateId" to estateId))
        if (!old.isNullOrBlank()) batch.delete(inviteCodes.document(old))
        batch.commit()
        code
    }

    override suspend fun transferOwnership(estateId: String, newOwnerId: String): Result<Unit> = runCatching {
        val estate = estates.document(estateId).get(Source.SERVER).takeIf { it.exists }?.let { mapDocToEstate(it) }
            ?: throw Exception("Estate not found")
        if (estate.isOwner(newOwnerId)) return@runCatching
        estates.document(estateId).update(
            "ownerId" to newOwnerId,
            "adminIds" to FieldValue.arrayUnion(newOwnerId)
        )
        notificationRepository.createNotification(
            AppNotification(
                recipientId = newOwnerId,
                type = NotificationType.OWNERSHIP_TRANSFERRED,
                title = "You now own ${estate.name}",
                message = "Ownership of ${estate.name} was handed to you. You're an admin for as long as you stay, " +
                    "and only you can hand it on.",
                estateId = estateId
            )
        )
    }

    private fun mapDocToEstate(doc: DocumentSnapshot): Estate? = runCatching {
        val adminIds = doc.stringList("adminIds") ?: listOfNotNull(doc.string("adminId"))
        Estate(
            id = doc.id,
            name = doc.string("name").orEmpty(),
            address = doc.string("address").orEmpty(),
            adminIds = adminIds,
            // Estates from before ownership was tracked: their first admin created them.
            ownerId = doc.string("ownerId") ?: adminIds.firstOrNull().orEmpty(),
            treasurerId = doc.string("treasurerId").orEmpty(),
            inviteCode = doc.string("inviteCode").orEmpty(),
            memberCount = doc.long("memberCount")?.toInt() ?: 0,
            createdAt = doc.long("createdAt") ?: 0L,
            joinMode = JoinMode.fromKey(doc.string("joinMode"))
        )
    }.getOrNull()

    private fun mapDocToRequest(doc: DocumentSnapshot): JoinRequest? = runCatching {
        JoinRequest(
            id = doc.id,
            userId = doc.string("userId").orEmpty(),
            userName = doc.string("userName").orEmpty(),
            userPhotoUrl = doc.string("userPhotoUrl").orEmpty(),
            estateId = doc.string("estateId").orEmpty(),
            estateName = doc.string("estateName").orEmpty(),
            requestedAt = doc.long("requestedAt") ?: 0L,
            status = doc.string("status") ?: "pending"
        )
    }.getOrNull()

    private fun requestRef(estateId: String, userId: String) =
        firestore.collection("joinRequests").document("${estateId}_${userId}")

    private fun inviteRef(estateId: String) =
        estates.document(estateId).collection("private").document("invite")

    /** A code no other estate holds; codes can't be overwritten, so a clash would fail the create. */
    private suspend fun unusedInviteCode(): String {
        repeat(5) {
            val code = generateInviteCode()
            if (!inviteCodes.document(code).get().exists) return code
        }
        throw Exception("Couldn't generate an invite code, please try again")
    }

    private fun generateInviteCode(): String {
        // No 0/O or 1/I, so a code read aloud or off a screen can't be mistyped.
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    /** Lowercase words only, so "12, Admiralty Way." and "12 admiralty way" compare equal. */
    private fun String.comparableAddress(): String =
        lowercase().map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("")
            .split(' ').filter { it.isNotEmpty() }.joinToString(" ")

    /** Equal, or one is the other with more detail ("12 admiralty way" vs "12 admiralty way lekki lagos"). */
    private fun String.sameAddressAs(other: String): Boolean =
        isNotEmpty() && other.isNotEmpty() && (" $this ".contains(" $other ") || " $other ".contains(" $this "))

    private fun Estate.toMap(): Map<String, Any?> = mapOf(
        "name" to name,
        "nameLower" to name.lowercase(),
        "address" to address,
        "adminIds" to adminIds,
        "ownerId" to ownerId,
        "memberCount" to memberCount,
        "createdAt" to createdAt,
        "joinMode" to joinMode.key
    )
}
