package com.eleazar.localhive

import com.eleazar.localhive.domain.model.Expense
import com.eleazar.localhive.domain.model.Levy
import com.eleazar.localhive.domain.model.LevyPayment
import com.eleazar.localhive.ui.finance.StatementEntry
import com.eleazar.localhive.ui.finance.statementCsv
import com.eleazar.localhive.ui.finance.totals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatementTest {

    private val levy = Levy(id = "levy1234", title = "Security, October", amount = 5_000)
    private val payment = LevyPayment(userId = "user9876", userName = "Ada", levyId = "levy1234", amount = 5_000, reviewedAt = 1_000)
    private val expense = Expense(id = "e1", title = "=SUM(A1)", amount = 2_000, spentAt = 2_000)

    @Test
    fun totalsSubtractSpendingFromCollections() {
        val totals = listOf(StatementEntry.income(levy, payment), StatementEntry.expense(expense)).totals()
        assertEquals(5_000, totals.collected)
        assertEquals(2_000, totals.spent)
        assertEquals(3_000, totals.balance)
    }

    @Test
    fun receiptNumberIsStableForAResidentAndLevy() {
        assertEquals("LH-LEVYUSER", payment.receiptNumber)
        assertEquals(payment.receiptNumber, payment.copy(reviewedAt = 99).receiptNumber)
    }

    @Test
    fun csvQuotesCommasAndDefusesFormulas() {
        val csv = statementCsv(listOf(StatementEntry.income(levy, payment), StatementEntry.expense(expense)), "Oak Estate", "All time")
        assertTrue("\"Security, October\"" in csv, csv)
        assertTrue("'=SUM(A1)" in csv, csv)
        // Negative amounts stay numbers, so spreadsheets can still add them up.
        assertTrue(",-2000," in csv, csv)
        assertTrue("Balance,,3000," in csv, csv)
    }
}
