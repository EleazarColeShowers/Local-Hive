package com.eleazar.localhive.ui.finance

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
import com.eleazar.localhive.domain.model.flatId

/**
 * One levy in the list: the current user's own standing, plus totals the treasurer can see. On a
 * per-flat levy, [myStatus] is the user's flat's, and the counts are of flats.
 */
data class LevyRow(
    val levy: Levy,
    val myStatus: PaymentStatus? = null,
    /** Treasurer only, like [toReview]. */
    val paidCount: Int = 0,
    val toReview: Int = 0,
    /** Per-flat levies: every flat in the estate, for "3 of 20 flats paid". */
    val totalFlats: Int = 0,
    /** Per-flat levies: the user hasn't said which flat they live in, so nothing is owed yet. */
    val needsFlat: Boolean = false
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

/** One flat on a per-flat levy: where it stands, and who lives there as far as the app knows. */
data class FlatRow(
    val building: Building,
    val flat: Int,
    val payment: FlatPayment?,
    val residents: List<String>
) {
    val status: PaymentStatus? get() = payment?.paymentStatus
}

/** One building on a per-flat levy, for the treasurer. */
data class BuildingRow(val building: Building, val flats: List<FlatRow>) {
    val paid: Int get() = flats.count { it.status == PaymentStatus.PAID }
    val waiting: Int get() = flats.count { it.status == PaymentStatus.PENDING }
}

/** One proof on a per-flat levy, with the flats it covers, for the treasurer to check. */
data class ProofToReview(val proof: FlatProof?, val payments: List<FlatPayment>) {
    val groupId: String get() = payments.first().groupId
    val payerName: String get() = payments.first().payerName.ifBlank { proof?.payerName.orEmpty() }.ifBlank { "Cash" }
    val total: Long get() = payments.sumOf { it.amount }
}

/** Everything the dues, levy, and expense screens render. */
data class FinanceUiState(
    val isLoading: Boolean = true,
    /** Pull to refresh is reconnecting the lists. */
    val isRefreshing: Boolean = false,
    val currentUserId: String? = null,
    val estateId: String? = null,
    /** For receipts. */
    val estateName: String = "",
    /** Where to pay; null until the treasurer adds them. */
    val bank: BankDetails? = null,
    val isTreasurer: Boolean = false,
    /** Admins can manage buildings too. */
    val isAdmin: Boolean = false,
    /** Blank when the estate has no treasurer. */
    val treasurerName: String? = null,
    /** Everyone a levy is owed by: the estate's members, treasurer included. */
    val memberCount: Int = 0,
    val levies: List<LevyRow> = emptyList(),
    val expenses: List<Expense> = emptyList(),

    /** The estate's buildings, for per-flat dues. */
    val buildings: List<Building> = emptyList(),
    /** Where current members live, those who have said. */
    val residents: List<Residence> = emptyList(),
    /** Where the user lives, once they've said. */
    val myResidence: Residence? = null,

    // The levy being looked at.
    val levy: Levy? = null,
    val levyMissing: Boolean = false,
    val myPayment: LevyPayment? = null,
    /** Treasurer only: every member, those with proof to check first. */
    val memberPayments: List<MemberPayment> = emptyList(),
    /** Per-flat levy: every flat in the user's building (or for the treasurer, every flat). */
    val flatPayments: List<FlatPayment> = emptyList(),
    /** Per-flat levy: proofs the user sent, or for the treasurer, every proof. */
    val flatProofs: List<FlatProof> = emptyList(),
    /** Per-flat levy: the flats in their building the user is about to send proof for. */
    val selectedFlats: Set<Int> = emptySet(),

    /** Treasurer only: every confirmed payment and expense, newest first, for the statement. */
    val statement: List<StatementEntry> = emptyList(),
    /** Everything the treasurer has done to the books, newest first. */
    val financeLog: List<FinanceLogEntry> = emptyList(),

    val isSaving: Boolean = false,
    val error: String? = null,
    /** A one-off confirmation to show, e.g. "Reminder sent to 4 people". */
    val message: String? = null,
    /** A form saved, so its screen should close. */
    val saved: Boolean = false
) {
    /** The treasurer and admins look after the list of buildings. */
    val canManageBuildings: Boolean get() = isTreasurer || isAdmin

    /** The building the user lives in, if they've said and it still exists. */
    val myBuilding: Building? get() = myResidence?.let { r -> buildings.firstOrNull { it.id == r.buildingId } }

    /** Every flat in the estate. */
    val totalFlats: Int get() = buildings.sumOf { it.flats }

    /** Per-flat levy, for the treasurer: each building with each of its flats. */
    val buildingRows: List<BuildingRow>
        get() {
            val byFlat = flatPayments.associateBy { it.flatId }
            val livingIn = residents.groupBy { it.flatId }
            return buildings.map { b ->
                BuildingRow(b, (1..b.flats).map { n ->
                    val id = flatId(b.id, n)
                    FlatRow(b, n, byFlat[id], livingIn[id].orEmpty().map { it.userName.ifBlank { "A neighbour" } })
                })
            }
        }

    /** Per-flat levy: the flats in the user's building, theirs first. */
    val myBuildingFlats: List<FlatRow>
        get() {
            val building = myBuilding ?: return emptyList()
            val byFlat = flatPayments.associateBy { it.flatId }
            val livingIn = residents.groupBy { it.flatId }
            return (1..building.flats).map { n ->
                val id = flatId(building.id, n)
                FlatRow(building, n, byFlat[id], livingIn[id].orEmpty().map { it.userName.ifBlank { "A neighbour" } })
            }.sortedBy { if (it.flat == myResidence?.flat) 0 else 1 }
        }

    /** Per-flat levy, for the treasurer: proofs waiting to be checked, oldest first. */
    val proofsToReview: List<ProofToReview>
        get() = flatPayments.filter { it.paymentStatus == PaymentStatus.PENDING }
            .groupBy { it.groupId }
            .map { (group, payments) -> ProofToReview(flatProofs.firstOrNull { it.groupId == group }, payments.sortedBy { it.flat }) }
            .sortedBy { it.payments.first().submittedAt }
}
