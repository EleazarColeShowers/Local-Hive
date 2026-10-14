package com.eleazar.localhive.ui.finance

import com.eleazar.localhive.domain.model.Levy
import com.eleazar.localhive.domain.model.PaymentStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DuesSummaryTest {
    private fun row(amount: Long, status: PaymentStatus?, toReview: Int = 0, title: String = "Security levy") =
        LevyRow(Levy(id = title, title = title, amount = amount), myStatus = status, toReview = toReview)

    @Test
    fun noLeviesMeansNothingToDo() {
        assertFalse(duesSummary(emptyList(), isTreasurer = false).needsAttention)
    }

    @Test
    fun unpaidAndRejectedLeviesAreAddedUp() {
        val summary = duesSummary(
            listOf(row(5000, null), row(2500, PaymentStatus.REJECTED, title = "Water"), row(9000, PaymentStatus.PAID)),
            isTreasurer = false
        )
        assertEquals("₦7,500 due", summary.headline)
        assertEquals("2 levies", summary.detail)
        assertTrue(summary.needsAttention)
    }

    @Test
    fun aSingleLevyIsNamed() {
        assertEquals("Security levy", duesSummary(listOf(row(5000, null)), isTreasurer = false).detail)
    }

    @Test
    fun treasurerSeesProofWaiting() {
        val summary = duesSummary(listOf(row(5000, PaymentStatus.PAID, toReview = 3)), isTreasurer = true)
        assertEquals("3 to review", summary.headline)
        assertTrue(summary.needsAttention)
    }

    @Test
    fun owingComesBeforeReviewing() {
        val summary = duesSummary(listOf(row(5000, null, toReview = 3)), isTreasurer = true)
        assertEquals("₦5,000 due", summary.headline)
    }

    @Test
    fun proofSentAndAllPaid() {
        assertEquals("Proof sent", duesSummary(listOf(row(5000, PaymentStatus.PENDING)), isTreasurer = false).headline)
        assertEquals("All paid", duesSummary(listOf(row(5000, PaymentStatus.PAID)), isTreasurer = false).headline)
    }
}
