package com.eleazar.localhive.ui.finance

import com.eleazar.localhive.domain.model.BankDetails
import com.eleazar.localhive.domain.model.Expense
import com.eleazar.localhive.domain.model.Levy
import com.eleazar.localhive.domain.model.LevyPayment
import com.eleazar.localhive.domain.model.PaymentStatus

/** One levy in the list: the current user's own standing, plus totals the treasurer can see. */
data class LevyRow(
    val levy: Levy,
    val myStatus: PaymentStatus? = null,
    /** Treasurer only, like [toReview]. */
    val paidCount: Int = 0,
    val toReview: Int = 0
)

/** One member on a levy, for the treasurer: who they are and where they stand. */
data class MemberPayment(
    val userId: String,
    val name: String,
    val photoUrl: String?,
    val payment: LevyPayment?
) {
    val status: PaymentStatus? get() = payment?.paymentStatus
}

/** Everything the dues, levy, and expense screens render. */
data class FinanceUiState(
    val isLoading: Boolean = true,
    val currentUserId: String? = null,
    val estateId: String? = null,
    /** For receipts. */
    val estateName: String = "",
    /** Where to pay; null until the treasurer adds them. */
    val bank: BankDetails? = null,
    val isTreasurer: Boolean = false,
    /** Blank when the estate has no treasurer. */
    val treasurerName: String? = null,
    /** Everyone a levy is owed by: the estate's members, treasurer included. */
    val memberCount: Int = 0,
    val levies: List<LevyRow> = emptyList(),
    val expenses: List<Expense> = emptyList(),

    // The levy being looked at.
    val levy: Levy? = null,
    val levyMissing: Boolean = false,
    val myPayment: LevyPayment? = null,
    /** Treasurer only: every member, those with proof to check first. */
    val memberPayments: List<MemberPayment> = emptyList(),

    /** Treasurer only: every confirmed payment and expense, newest first, for the statement. */
    val statement: List<StatementEntry> = emptyList(),

    val isSaving: Boolean = false,
    val error: String? = null,
    /** A one-off confirmation to show, e.g. "Reminder sent to 4 people". */
    val message: String? = null,
    /** A form saved, so its screen should close. */
    val saved: Boolean = false
)
