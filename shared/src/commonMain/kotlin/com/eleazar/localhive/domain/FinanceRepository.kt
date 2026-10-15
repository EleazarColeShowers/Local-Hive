package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.BankDetails
import com.eleazar.localhive.domain.model.Expense
import com.eleazar.localhive.domain.model.Levy
import com.eleazar.localhive.domain.model.LevyPayment
import com.eleazar.localhive.domain.model.PaymentStatus
import kotlinx.coroutines.flow.Flow

/**
 * The estate's dues and spending. Residents read levies and expenses and send proof of their own
 * payments; everything else is the treasurer's, which the security rules enforce.
 */
interface FinanceRepository {
    /** Newest first. */
    fun observeLevies(estateId: String): Flow<List<Levy>>
    fun observeLevy(estateId: String, levyId: String): Flow<Levy?>
    /** Every payment on a levy. Only the treasurer may read these. */
    fun observePayments(estateId: String, levyId: String): Flow<List<LevyPayment>>
    /** One resident's own payment on a levy, or null before they've paid or sent proof. */
    fun observeMyPayment(estateId: String, levyId: String, userId: String): Flow<LevyPayment?>

    suspend fun createLevy(estateId: String, levy: Levy): Result<String>
    /** Deletes the levy and every payment recorded against it. */
    suspend fun deleteLevy(estateId: String, levyId: String): Result<Unit>

    /** A resident sending proof of payment; tells the treasurer, if there is one. */
    suspend fun submitProof(
        estateId: String,
        levy: Levy,
        userId: String,
        userName: String,
        proofUrl: String,
        treasurerId: String
    ): Result<Unit>

    /**
     * The treasurer setting where a resident stands: confirming proof, rejecting it, or
     * recording a cash payment that came with no proof. Tells the resident. [treasurerName]
     * goes on the receipt of a confirmed payment.
     */
    suspend fun setPaymentStatus(
        estateId: String,
        levy: Levy,
        userId: String,
        userName: String,
        status: PaymentStatus,
        existing: LevyPayment?,
        treasurerName: String
    ): Result<Unit>

    /** Pushes a reminder to everyone yet to pay; returns how many it reached. At most once a day. */
    suspend fun remindUnpaid(estateId: String, levyId: String): Result<Int>

    /** Newest spending first. */
    fun observeExpenses(estateId: String): Flow<List<Expense>>
    suspend fun addExpense(estateId: String, expense: Expense): Result<Unit>
    suspend fun deleteExpense(estateId: String, expenseId: String): Result<Unit>

    /** Where to pay; null until the treasurer adds them. Members only. */
    fun observeBankDetails(estateId: String): Flow<BankDetails?>
    /** The treasurer setting where to pay; null removes them. */
    suspend fun setBankDetails(estateId: String, details: BankDetails?, updatedBy: String): Result<Unit>
}
