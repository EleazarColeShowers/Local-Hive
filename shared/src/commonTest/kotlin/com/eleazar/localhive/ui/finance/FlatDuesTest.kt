package com.eleazar.localhive.ui.finance

import com.eleazar.localhive.domain.model.Building
import com.eleazar.localhive.domain.model.FlatPayment
import com.eleazar.localhive.domain.model.Levy
import com.eleazar.localhive.domain.model.PaymentStatus
import com.eleazar.localhive.domain.model.Residence
import com.eleazar.localhive.domain.model.flatSummary
import kotlin.test.Test
import kotlin.test.assertEquals

class FlatDuesTest {
    private val levy = Levy(id = "F1abc", title = "Water", amount = 2000, perFlat = true)
    private val no12 = Building(id = "B12", name = "No. 12", flats = 4)
    private val no14 = Building(id = "B14", name = "No. 14", flats = 2)

    private fun paid(flat: Int, group: String, building: Building = no12, status: PaymentStatus = PaymentStatus.PAID) = FlatPayment(
        buildingId = building.id, buildingName = building.name, flat = flat, levyId = levy.id, amount = 2000,
        status = status.key, payerId = "ada", payerName = "Ada", groupId = group, submittedAt = 10, reviewedAt = 20
    )

    @Test
    fun flatsAreNamedLikeAReceipt() {
        assertEquals("No. 12 · Flats 1, 3", flatSummary(listOf(paid(3, "g"), paid(1, "g"))))
        assertEquals("No. 14 · Flat 2", flatSummary(listOf(paid(2, "g", no14))))
    }

    @Test
    fun oneProofForSeveralFlatsIsOneStatementLine() {
        val entry = StatementEntry.flatIncome(levy, listOf(paid(1, "group1234"), paid(3, "group1234")))
        assertEquals(4000, entry.amount)
        assertEquals("Ada · No. 12 · Flats 1, 3", entry.member)
        assertEquals("LH-F1AB1234", entry.receiptNumber)
    }

    @Test
    fun theTreasurerSeesEveryFlatOfEveryBuilding() {
        val state = FinanceUiState(
            buildings = listOf(no12, no14),
            residents = listOf(Residence("bayo", "B12", "No. 12", 2, "Bayo")),
            flatPayments = listOf(paid(1, "g1"), paid(2, "g2", status = PaymentStatus.PENDING))
        )
        val rows = state.buildingRows
        assertEquals(listOf(4, 2), rows.map { it.flats.size })
        assertEquals(1, rows[0].paid)
        assertEquals(1, rows[0].waiting)
        assertEquals(listOf("Bayo"), rows[0].flats[1].residents)
        assertEquals(6, state.totalFlats)
        assertEquals(listOf("g2"), state.proofsToReview.map { it.groupId })
    }

    @Test
    fun aResidentSeesTheirOwnBuildingWithTheirFlatFirst() {
        val state = FinanceUiState(
            buildings = listOf(no12, no14),
            myResidence = Residence("chi", "B12", "No. 12", 3, "Chi"),
            flatPayments = listOf(paid(1, "g1"))
        )
        assertEquals(listOf(3, 1, 2, 4), state.myBuildingFlats.map { it.flat })
        assertEquals(PaymentStatus.PAID, state.myBuildingFlats.first { it.flat == 1 }.status)
    }

    @Test
    fun aFlatReceiptCoversEveryFlatItPaidFor() {
        val receipt = flatReceipt(levy, listOf(paid(1, "group1234"), paid(2, "group1234")))
        assertEquals(4000, receipt.amount)
        assertEquals("No. 12 · Flats 1, 2", receipt.covers)
        val text = receiptText("Amali", receipt)
        assertEquals(true, "Covers: No. 12 · Flats 1, 2" in text)
    }
}
