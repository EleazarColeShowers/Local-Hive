package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.BankDetails
import com.eleazar.localhive.domain.model.Building
import com.eleazar.localhive.domain.model.Expense
import com.eleazar.localhive.domain.model.FinanceLogEntry
import com.eleazar.localhive.domain.model.FlatPayment
import com.eleazar.localhive.domain.model.FlatProof
import com.eleazar.localhive.domain.model.Levy
import com.eleazar.localhive.domain.model.LevyPayment
import com.eleazar.localhive.domain.model.PaymentStatus
import com.eleazar.localhive.domain.model.Residence
import kotlinx.coroutines.flow.Flow

/**
 * The estate's dues and spending. Residents read levies and expenses and send proof of their own
 * payments; everything else is the treasurer's, which the security rules enforce. Every change the
 * treasurer makes to the books also goes in the finance log, in the same write.
 *
 * Where a method takes [byName], it's the treasurer's name, for the log and receipts.
 */
interface FinanceRepository {
    /** Every levy, per member and per flat, newest first. */
    fun observeLevies(estateId: String): Flow<List<Levy>>
    fun observeLevy(estateId: String, levyId: String): Flow<Levy?>
    /** Every payment on a per-member levy. Only the treasurer may read these. */
    fun observePayments(estateId: String, levyId: String): Flow<List<LevyPayment>>
    /** One resident's own payment on a per-member levy, or null before they've paid or sent proof. */
    fun observeMyPayment(estateId: String, levyId: String, userId: String): Flow<LevyPayment?>

    suspend fun createLevy(estateId: String, levy: Levy, byName: String): Result<String>
    /**
     * Deletes a levy nobody has paid yet, with any proof still waiting. One with a confirmed
     * payment stays, and fails with [LevyHasPaymentsException].
     */
    suspend fun deleteLevy(estateId: String, levy: Levy, byName: String): Result<Unit>

    /** A resident sending proof of payment on a per-member levy; tells the treasurer, if there is one. */
    suspend fun submitProof(
        estateId: String,
        levy: Levy,
        userId: String,
        userName: String,
        proofUrl: String,
        treasurerId: String
    ): Result<Unit>

    /**
     * The treasurer setting where a resident stands on a per-member levy: confirming proof,
     * rejecting it, recording a cash payment, or taking a confirmation back, which needs
     * [undoReason]. Tells the resident.
     */
    suspend fun setPaymentStatus(
        estateId: String,
        levy: Levy,
        userId: String,
        userName: String,
        status: PaymentStatus,
        existing: LevyPayment?,
        byName: String,
        undoReason: String? = null
    ): Result<Unit>

    /** Pushes a reminder to everyone yet to pay; returns how many it reached. At most once a day. */
    suspend fun remindUnpaid(estateId: String, levy: Levy): Result<Int>

    // ───── Buildings and flats ─────

    fun observeBuildings(estateId: String): Flow<List<Building>>
    /** Adds a building, or with an id, renames it or changes its number of flats. */
    suspend fun saveBuilding(estateId: String, building: Building, userId: String): Result<Unit>
    suspend fun deleteBuilding(estateId: String, buildingId: String): Result<Unit>

    /** Where every member who has said lives. */
    fun observeResidents(estateId: String): Flow<List<Residence>>
    /** Sets where [residence].userId lives: their own pick, or the treasurer's or an admin's correction. */
    suspend fun setResidence(estateId: String, residence: Residence, setBy: String): Result<Unit>
    suspend fun clearResidence(estateId: String, userId: String): Result<Unit>

    /** Every flat's payment on a per-flat levy. The treasurer only. */
    fun observeFlatPayments(estateId: String, levyId: String): Flow<List<FlatPayment>>
    /** The payments for flats in one building, which everyone living there can see. */
    fun observeBuildingPayments(estateId: String, levyId: String, buildingId: String): Flow<List<FlatPayment>>
    /** Proof on a per-flat levy: every one for the treasurer, or just [payerId]'s own. */
    fun observeFlatProofs(estateId: String, levyId: String, payerId: String?): Flow<List<FlatProof>>

    /** A resident sending one proof that covers [flats] in their building. Tells the treasurer. */
    suspend fun submitFlatProof(
        estateId: String,
        levy: Levy,
        building: Building,
        flats: List<Int>,
        payerId: String,
        payerName: String,
        proofUrl: String
    ): Result<Unit>

    /**
     * The treasurer confirming or rejecting every flat one proof covers, or taking a confirmation
     * back with [undoReason]. Tells the payer, and on confirming, everyone living in those flats.
     */
    suspend fun reviewFlatPayments(
        estateId: String,
        levy: Levy,
        payments: List<FlatPayment>,
        status: PaymentStatus,
        byName: String,
        undoReason: String? = null
    ): Result<Unit>

    /** The treasurer recording cash, or a transfer they saw, for one flat. */
    suspend fun recordFlatCash(estateId: String, levy: Levy, building: Building, flat: Int, byName: String): Result<Unit>

    // ───── Spending, bank details and the log ─────

    /** Newest spending first. */
    fun observeExpenses(estateId: String): Flow<List<Expense>>
    suspend fun addExpense(estateId: String, expense: Expense, byName: String): Result<Unit>
    /** Only within a day of adding it; after that an expense stays. */
    suspend fun deleteExpense(estateId: String, expense: Expense, byName: String): Result<Unit>

    /** Where to pay; null until the treasurer adds them. Members only. */
    fun observeBankDetails(estateId: String): Flow<BankDetails?>
    /** The treasurer setting where to pay; null removes them. */
    suspend fun setBankDetails(estateId: String, details: BankDetails?, updatedBy: String, byName: String): Result<Unit>

    /** Everything the treasurer has done to the books, newest first. Every member can read it. */
    fun observeFinanceLog(estateId: String): Flow<List<FinanceLogEntry>>
}

/** Thrown when deleting a levy someone has paid: it stays, so the payment stays on the books. */
class LevyHasPaymentsException : Exception("Someone has paid this levy, so it stays on the books. It can't be deleted.")

/** How long after adding an expense the treasurer can still delete it. Matches the security rules. */
const val EXPENSE_UNDO_WINDOW_MILLIS = 24L * 60 * 60 * 1000
