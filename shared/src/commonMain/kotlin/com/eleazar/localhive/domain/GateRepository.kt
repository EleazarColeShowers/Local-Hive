package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.Guard
import com.eleazar.localhive.domain.model.GuardInvite
import com.eleazar.localhive.domain.model.GuardPost
import com.eleazar.localhive.domain.model.User
import com.eleazar.localhive.domain.model.VisitorPass
import kotlinx.coroutines.flow.Flow

/**
 * Visitors at the gate. Residents make passes for their visitors; security guards, who don't
 * live in the estate and so aren't members, check visitors in and out by the pass's code; admins
 * decide who the guards are. The Firestore rules hold each side to its part.
 */
interface GateRepository {

    // ── Residents ──

    /** The resident's own passes, newest first. */
    fun observeMyPasses(estateId: String, residentId: String): Flow<List<VisitorPass>>

    /** A pass for [visitorName] that works on the day running from [validFrom] to [validUntil]. */
    suspend fun createPass(
        estateId: String,
        resident: User,
        visitorName: String,
        note: String,
        validFrom: Long,
        validUntil: Long
    ): Result<VisitorPass>

    suspend fun cancelPass(estateId: String, code: String): Result<Unit>

    suspend fun deletePass(estateId: String, code: String): Result<Unit>

    // ── Guards ──

    /**
     * Where [user] is a guard, or null if they aren't one anywhere (including when an admin has
     * removed them). Fails only when it couldn't find out, such as offline.
     */
    suspend fun findGuardPost(user: User): Result<GuardPost?>

    /** Live: false once an admin removes the guard, or the estate is gone. */
    fun observeStillGuard(estateId: String, guardId: String): Flow<Boolean>

    /** Signs [user] on as a guard with a code from an admin. */
    suspend fun redeemGuardCode(code: String, user: User): Result<GuardPost>

    /** The guard stops working at [estateId]'s gate. */
    suspend fun leaveGuardPost(estateId: String, guardId: String): Result<Unit>

    /** The pass with this code, or null when there's no usable one: wrong code, expired, cancelled or used. */
    suspend fun lookUpPass(estateId: String, code: String): Result<VisitorPass?>

    /** Visitors checked in and not yet out, longest-staying first. */
    fun observeInside(estateId: String): Flow<List<VisitorPass>>

    suspend fun checkIn(estateId: String, code: String, guardId: String, guardName: String): Result<Unit>

    suspend fun checkOut(estateId: String, code: String, guardId: String, guardName: String): Result<Unit>

    // ── Admins ──

    fun observeGuards(estateId: String): Flow<List<Guard>>

    /** A fresh one-time code that signs one guard on within a day. */
    suspend fun createGuardInvite(estateId: String, adminId: String): Result<GuardInvite>

    suspend fun removeGuard(estateId: String, guardId: String): Result<Unit>
}
