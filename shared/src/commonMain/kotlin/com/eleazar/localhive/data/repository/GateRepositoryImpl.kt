package com.eleazar.localhive.data.repository

import com.eleazar.localhive.data.remote.LocalHiveApi
import com.eleazar.localhive.domain.GateRepository
import com.eleazar.localhive.domain.model.GUARD_CODE_LENGTH
import com.eleazar.localhive.domain.model.Guard
import com.eleazar.localhive.domain.model.GuardInvite
import com.eleazar.localhive.domain.model.GuardPost
import com.eleazar.localhive.domain.model.PASS_CODE_LENGTH
import com.eleazar.localhive.domain.model.PassStage
import com.eleazar.localhive.domain.model.User
import com.eleazar.localhive.domain.model.VisitorPass
import com.eleazar.localhive.domain.model.isValidCode
import com.eleazar.localhive.domain.model.normalizeCode
import com.eleazar.localhive.domain.model.randomCode
import com.eleazar.localhive.ui.util.nowMillis
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.code
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** How long an admin's guard code works for. */
private const val GUARD_CODE_LIFETIME_MILLIS = 24 * 60 * 60 * 1000L

/** Codes are random, so a clash with one already taken is rare; a few tries is plenty. */
private const val CODE_ATTEMPTS = 4

/**
 * Layout: `estates/{id}/visitorPasses/{code}`, `estates/{id}/guards/{uid}`, and the one-time
 * `guardCodes/{code}` that point at an estate. See firestore.rules for who may do what.
 */
class GateRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val api: LocalHiveApi
) : GateRepository {

    private fun estate(estateId: String) = firestore.collection("estates").document(estateId)
    private fun passes(estateId: String) = estate(estateId).collection("visitorPasses")
    private fun guards(estateId: String) = estate(estateId).collection("guards")
    private val guardCodes get() = firestore.collection("guardCodes")

    // ── Residents ──

    override fun observeMyPasses(estateId: String, residentId: String): Flow<List<VisitorPass>> =
        passes(estateId).where { "residentId" equalTo residentId }.snapshots
            .map { snapshot -> snapshot.documents.map { it.toVisitorPass(estateId) }.sortedByDescending { it.createdAt } }

    override suspend fun createPass(
        estateId: String,
        resident: User,
        visitorName: String,
        note: String,
        validFrom: Long,
        validUntil: Long
    ): Result<VisitorPass> = runCatching {
        val name = visitorName.trim()
        require(name.isNotEmpty()) { "Enter your visitor's name" }
        val pass = VisitorPass(
            estateId = estateId,
            residentId = resident.id,
            residentName = (resident.displayName ?: resident.username ?: "A resident").take(100),
            residentAddress = resident.address.orEmpty().take(200),
            visitorName = name.take(80),
            note = note.trim().take(200),
            validFrom = validFrom,
            validUntil = validUntil,
            createdAt = nowMillis()
        )
        // The code is the document's id. If it's taken, the rules see an update to someone
        // else's pass and refuse it, so a clash shows up as a refusal and we pick another.
        withFreshCode(PASS_CODE_LENGTH) { code ->
            passes(estateId).document(code).set(
                mapOf(
                    "code" to code,
                    "residentId" to pass.residentId,
                    "residentName" to pass.residentName,
                    "residentAddress" to pass.residentAddress,
                    "visitorName" to pass.visitorName,
                    "note" to pass.note,
                    "validFrom" to pass.validFrom,
                    "validUntil" to pass.validUntil,
                    "createdAt" to pass.createdAt,
                    "stage" to PassStage.EXPECTED.key
                )
            )
            pass.copy(code = code)
        }
    }

    override suspend fun cancelPass(estateId: String, code: String): Result<Unit> = runCatching {
        passes(estateId).document(code).update("stage" to PassStage.CANCELLED.key)
    }

    override suspend fun deletePass(estateId: String, code: String): Result<Unit> = runCatching {
        passes(estateId).document(code).delete()
    }

    // ── Guards ──

    override suspend fun findGuardPost(user: User): Result<GuardPost?> = runCatching {
        val estateId = user.guardEstateId ?: return@runCatching null
        if (!guards(estateId).document(user.id).get(Source.SERVER).exists) return@runCatching null
        val estate = estate(estateId).get(Source.SERVER)
        if (!estate.exists) return@runCatching null
        GuardPost(estateId, estate.string("name").orEmpty())
    }

    override fun observeStillGuard(estateId: String, guardId: String): Flow<Boolean> =
        combine(guards(estateId).document(guardId).snapshots, estate(estateId).snapshots) { guard, estate ->
            guard.exists && estate.exists
        }

    override suspend fun redeemGuardCode(code: String, user: User): Result<GuardPost> = runCatching {
        val clean = normalizeCode(code)
        if (!isValidCode(clean, GUARD_CODE_LENGTH)) throw Exception("A guard code is $GUARD_CODE_LENGTH letters and numbers")
        if (user.estateId != null) {
            throw Exception("You're a resident of an estate on LocalHive. Leave it in Settings before signing on as a guard.")
        }
        val lookup = guardCodes.document(clean).get(Source.SERVER)
        val estateId = lookup.takeIf { it.exists }?.string("estateId") ?: throw Exception("That guard code isn't valid")
        if (nowMillis() >= (lookup.long("expiresAt") ?: 0L)) {
            throw Exception("That guard code has expired. Ask the estate's admin for a new one.")
        }
        val estate = estate(estateId).get(Source.SERVER)
        if (!estate.exists) throw Exception("That estate no longer exists")

        // The code goes in the same batch it's used in, so it can't sign on a second guard.
        firestore.batch()
            .set(
                guards(estateId).document(user.id),
                mapOf(
                    "userId" to user.id,
                    "name" to (user.displayName ?: user.username ?: "Guard").take(100),
                    "code" to clean,
                    "addedAt" to nowMillis()
                )
            )
            .delete(guardCodes.document(clean))
            .set(firestore.users().document(user.id), mapOf("guardEstateId" to estateId), merge = true)
            .commit()
        GuardPost(estateId, estate.string("name").orEmpty())
    }

    override suspend fun leaveGuardPost(estateId: String, guardId: String): Result<Unit> = runCatching {
        firestore.batch()
            .delete(guards(estateId).document(guardId))
            .update(firestore.users().document(guardId), "guardEstateId" to FieldValue.delete)
            .commit()
    }

    override suspend fun lookUpPass(estateId: String, code: String): Result<VisitorPass?> = runCatching {
        val clean = normalizeCode(code)
        if (!isValidCode(clean, PASS_CODE_LENGTH)) return@runCatching null
        try {
            passes(estateId).document(clean).get(Source.SERVER)
                .takeIf { it.exists }
                ?.toVisitorPass(estateId)
        } catch (e: FirebaseFirestoreException) {
            // Guards may only read a pass that's usable now, and a missing one reads as refused
            // too. Either way, as far as the gate is concerned, there's no pass.
            if (e.code == FirestoreExceptionCode.PERMISSION_DENIED) null else throw e
        }
    }

    override fun observeInside(estateId: String): Flow<List<VisitorPass>> =
        passes(estateId).where { "stage" equalTo PassStage.INSIDE.key }.snapshots
            .map { snapshot -> snapshot.documents.map { it.toVisitorPass(estateId) }.sortedBy { it.checkedInAt } }

    override suspend fun checkIn(estateId: String, code: String, guardId: String, guardName: String): Result<Unit> =
        runCatching {
            passes(estateId).document(code).update(
                "stage" to PassStage.INSIDE.key,
                "checkedInAt" to nowMillis(),
                "checkedInBy" to guardId,
                "checkedInByName" to guardName.take(100)
            )
            api.notifyInBackground("type" to "visitor_arrived", "estateId" to estateId, "code" to code)
        }

    override suspend fun checkOut(estateId: String, code: String, guardId: String, guardName: String): Result<Unit> =
        runCatching {
            passes(estateId).document(code).update(
                "stage" to PassStage.LEFT.key,
                "checkedOutAt" to nowMillis(),
                "checkedOutBy" to guardId,
                "checkedOutByName" to guardName.take(100)
            )
            api.notifyInBackground("type" to "visitor_left", "estateId" to estateId, "code" to code)
        }

    // ── Admins ──

    override fun observeGuards(estateId: String): Flow<List<Guard>> =
        guards(estateId).snapshots
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    Guard(userId = doc.id, name = doc.string("name").orEmpty(), addedAt = doc.long("addedAt") ?: 0L)
                }.sortedBy { it.addedAt }
            }
            .catch { emit(emptyList()) }

    override suspend fun createGuardInvite(estateId: String, adminId: String): Result<GuardInvite> = runCatching {
        val now = nowMillis()
        val expiresAt = now + GUARD_CODE_LIFETIME_MILLIS
        withFreshCode(GUARD_CODE_LENGTH) { code ->
            guardCodes.document(code).set(
                mapOf("estateId" to estateId, "createdBy" to adminId, "createdAt" to now, "expiresAt" to expiresAt)
            )
            GuardInvite(code, expiresAt)
        }
    }

    override suspend fun removeGuard(estateId: String, guardId: String): Result<Unit> = runCatching {
        guards(estateId).document(guardId).delete()
    }

    /** Runs [write] with a new random code, trying another when the rules refuse one that's taken. */
    private suspend fun <T> withFreshCode(length: Int, write: suspend (code: String) -> T): T {
        var lastError: Exception? = null
        repeat(CODE_ATTEMPTS) {
            try {
                return write(randomCode(length))
            } catch (e: FirebaseFirestoreException) {
                if (e.code != FirestoreExceptionCode.PERMISSION_DENIED) throw e
                lastError = e
            }
        }
        throw lastError ?: Exception("Couldn't make a code")
    }

    private fun DocumentSnapshot.toVisitorPass(estateId: String) = VisitorPass(
        code = id,
        estateId = estateId,
        residentId = string("residentId").orEmpty(),
        residentName = string("residentName").orEmpty(),
        residentAddress = string("residentAddress").orEmpty(),
        visitorName = string("visitorName").orEmpty(),
        note = string("note").orEmpty(),
        validFrom = long("validFrom") ?: 0L,
        validUntil = long("validUntil") ?: 0L,
        createdAt = long("createdAt") ?: 0L,
        stage = PassStage.fromKey(string("stage")),
        checkedInAt = long("checkedInAt") ?: 0L,
        checkedInByName = string("checkedInByName").orEmpty(),
        checkedOutAt = long("checkedOutAt") ?: 0L,
        checkedOutByName = string("checkedOutByName").orEmpty()
    )
}
