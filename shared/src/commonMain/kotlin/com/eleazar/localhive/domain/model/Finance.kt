package com.eleazar.localhive.domain.model

/**
 * A charge every resident of the estate owes, set by the treasurer, e.g. a security levy.
 * Amounts are whole naira.
 */
data class Levy(
    val id: String = "",
    val title: String = "",
    val amount: Long = 0L,
    val dueDate: Long = 0L,
    val note: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0L
)

/** Where one resident stands on one levy. Stored lower-case, as [key]. */
enum class PaymentStatus(val key: String) {
    /** The resident sent proof; the treasurer hasn't looked at it yet. */
    PENDING("pending"),
    PAID("paid"),
    /** The treasurer didn't accept the proof; the resident can send another. */
    REJECTED("rejected");

    companion object {
        fun fromKey(key: String?): PaymentStatus? = entries.firstOrNull { it.key == key }
    }
}

/** One resident's payment towards a levy. Its document id is the resident's user id. */
data class LevyPayment(
    val userId: String = "",
    val userName: String = "",
    val levyId: String = "",
    val amount: Long = 0L,
    /** A [PaymentStatus] key. */
    val status: String = "",
    /** A screenshot of the transfer, when the resident sent one; cash payments have none. */
    val proofUrl: String? = null,
    val submittedAt: Long = 0L,
    val reviewedAt: Long = 0L
) {
    val paymentStatus: PaymentStatus? get() = PaymentStatus.fromKey(status)
}

/** Money the treasurer spent on the estate's behalf. Every member can see these. */
data class Expense(
    val id: String = "",
    val title: String = "",
    val amount: Long = 0L,
    val spentAt: Long = 0L,
    val note: String = "",
    val receiptUrl: String? = null,
    val createdBy: String = "",
    val createdAt: Long = 0L
)
