package com.eleazar.localhive.domain.model

/**
 * A charge set by the treasurer, e.g. a security levy. Amounts are whole naira. Most are owed by
 * every member; a [perFlat] one is owed by every flat in every building, [amount] each.
 */
data class Levy(
    val id: String = "",
    val title: String = "",
    val amount: Long = 0L,
    val dueDate: Long = 0L,
    val note: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val perFlat: Boolean = false,
    /** A payment has been confirmed, so the levy can no longer be deleted. */
    val hasPayments: Boolean = false
)

/** A building in the estate, for dues charged per flat. Flats are numbered 1 to [flats]. */
data class Building(
    val id: String = "",
    val name: String = "",
    val flats: Int = 1
)

/** Which flat a member lives in. Their own pick; the treasurer or an admin can correct it. */
data class Residence(
    val userId: String = "",
    val buildingId: String = "",
    val buildingName: String = "",
    val flat: Int = 0,
    val userName: String = ""
) {
    val flatId: String get() = flatId(buildingId, flat)
}

/** How a flat is keyed on a per-flat levy: "<buildingId>_<flat>". */
fun flatId(buildingId: String, flat: Int): String = "${buildingId}_$flat"

/**
 * One flat's payment towards a per-flat levy. One proof can cover several flats; they share
 * [groupId], and the treasurer confirms or rejects them together.
 */
data class FlatPayment(
    val buildingId: String = "",
    val buildingName: String = "",
    val flat: Int = 0,
    val levyId: String = "",
    val amount: Long = 0L,
    /** A [PaymentStatus] key. */
    val status: String = "",
    /** Blank for cash the treasurer recorded. */
    val payerId: String = "",
    val payerName: String = "",
    val groupId: String = "",
    val submittedAt: Long = 0L,
    val reviewedAt: Long = 0L,
    val confirmedByName: String = "",
    /** Why the treasurer took back a confirmation, if they did. */
    val undoReason: String = ""
) {
    val flatId: String get() = flatId(buildingId, flat)
    val paymentStatus: PaymentStatus? get() = PaymentStatus.fromKey(status)
    val receivedAt: Long get() = reviewedAt.takeIf { it > 0 } ?: submittedAt
}

/** "No. 12 · Flats 1, 3" for the flats some payments cover. Matches the Worker's wording. */
fun flatSummary(payments: List<FlatPayment>): String =
    payments.groupBy { it.buildingName.ifBlank { "Building" } }
        .entries.joinToString("; ") { (name, list) ->
            val numbers = list.map { it.flat }.sorted()
            "$name · ${if (numbers.size == 1) "Flat" else "Flats"} ${numbers.joinToString(", ")}"
        }

/** The screenshot behind one or more [FlatPayment]s. Only the payer and the treasurer can see it. */
data class FlatProof(
    val groupId: String = "",
    val payerId: String = "",
    val payerName: String = "",
    val buildingId: String = "",
    val flatIds: List<String> = emptyList(),
    val amount: Long = 0L,
    val proofUrl: String = "",
    val submittedAt: Long = 0L
)

/** One thing the treasurer did to the books. Every member can read the log; nobody can change it. */
data class FinanceLogEntry(
    val id: String = "",
    val action: String = "",
    val summary: String = "",
    val amount: Long = 0L,
    val byName: String = "",
    val at: Long = 0L
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
    val reviewedAt: Long = 0L,
    /** The treasurer who confirmed it, for the receipt. Blank on payments confirmed before receipts. */
    val confirmedByName: String = "",
    /** Why the treasurer took back a confirmation, if they did. */
    val undoReason: String = ""
) {
    val paymentStatus: PaymentStatus? get() = PaymentStatus.fromKey(status)

    /** When the money counts as received: when the treasurer confirmed it. */
    val receivedAt: Long get() = reviewedAt.takeIf { it > 0 } ?: submittedAt

    /** Stable for a resident and levy, so a receipt shared twice carries the same number. */
    val receiptNumber: String
        get() = "LH-" + (levyId.take(4) + userId.take(4)).uppercase()
}

/**
 * Where residents send their dues. Kept off the public estate document; only members can read it.
 * [updatedByName] and [updatedAt] are shown to everyone, so a change of account is never silent.
 */
data class BankDetails(
    val bankName: String = "",
    val accountNumber: String = "",
    val accountName: String = "",
    val updatedByName: String = "",
    val updatedAt: Long = 0L
)

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
