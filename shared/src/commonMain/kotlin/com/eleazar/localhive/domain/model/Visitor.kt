package com.eleazar.localhive.domain.model

import kotlin.random.Random

/** Where a visitor pass is in its one visit. [key] is what's stored on the pass. */
enum class PassStage(val key: String) {
    /** Made by the resident; the visitor hasn't reached the gate yet. */
    EXPECTED("expected"),
    /** A guard checked the visitor in. */
    INSIDE("inside"),
    /** A guard checked the visitor out. The pass is used up. */
    LEFT("left"),
    /** The resident called the visit off. */
    CANCELLED("cancelled");

    companion object {
        fun fromKey(key: String?): PassStage = entries.firstOrNull { it.key == key } ?: EXPECTED
    }
}

/**
 * One visitor, one visit, on one day. A resident makes it and gives the [code] to their visitor;
 * a guard at the gate types the code in to check them in and later out.
 */
data class VisitorPass(
    val code: String = "",
    val estateId: String = "",
    val residentId: String = "",
    val residentName: String = "",
    /** The resident's house, as on their profile, so the guard knows where the visitor is going. */
    val residentAddress: String = "",
    val visitorName: String = "",
    val note: String = "",
    /** The pass works from [validFrom] until just before [validUntil]: one calendar day. */
    val validFrom: Long = 0L,
    val validUntil: Long = 0L,
    val createdAt: Long = 0L,
    val stage: PassStage = PassStage.EXPECTED,
    val checkedInAt: Long = 0L,
    val checkedInByName: String = "",
    val checkedOutAt: Long = 0L,
    val checkedOutByName: String = ""
) {
    /** Never used, and its day is over. */
    fun isExpired(now: Long): Boolean = stage == PassStage.EXPECTED && now >= validUntil

    /** A guard can check the visitor in right now. */
    fun canCheckIn(now: Long): Boolean = stage == PassStage.EXPECTED && now in validFrom until validUntil

    /** Still waiting on the visitor, or they're inside: worth showing at the top. */
    fun isCurrent(now: Long): Boolean = stage == PassStage.INSIDE || (stage == PassStage.EXPECTED && now < validUntil)
}

/** A security guard signed on to an estate's gate. */
data class Guard(
    val userId: String = "",
    val name: String = "",
    val addedAt: Long = 0L
)

/** The estate a guard works the gate at. */
data class GuardPost(
    val estateId: String,
    val estateName: String
)

/** A one-time code an admin gives a new guard, which stops working at [expiresAt]. */
data class GuardInvite(
    val code: String,
    val expiresAt: Long
)

/** No 0/O or 1/I, which get mixed up when read out at a gate. Matches the Firestore rules. */
private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

const val PASS_CODE_LENGTH = 6
const val GUARD_CODE_LENGTH = 8

fun randomCode(length: Int, random: Random = Random.Default): String =
    (1..length).map { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }.joinToString("")

/** What someone typed, as a code: upper case, without spaces or dashes, and only the allowed letters. */
fun normalizeCode(input: String): String =
    input.uppercase().filter { it in CODE_ALPHABET }

fun isValidCode(code: String, length: Int): Boolean =
    code.length == length && code.all { it in CODE_ALPHABET }
