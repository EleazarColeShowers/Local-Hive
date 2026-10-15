package com.eleazar.localhive.ui.finance

import com.eleazar.localhive.domain.model.Expense
import com.eleazar.localhive.domain.model.Levy
import com.eleazar.localhive.domain.model.LevyPayment
import com.eleazar.localhive.ui.util.formatDate

/** One line on the estate's statement: money in from a confirmed payment, or money out on an expense. */
data class StatementEntry(
    val id: String,
    val at: Long,
    /** The levy paid towards, or what the money was spent on. */
    val title: String,
    /** Who paid, for money in; blank for money out. */
    val member: String,
    /** Positive in, negative out. */
    val amount: Long,
    /** For money in. */
    val receiptNumber: String = ""
) {
    val isIncome: Boolean get() = amount >= 0

    companion object {
        fun income(levy: Levy, payment: LevyPayment) = StatementEntry(
            id = "in-${levy.id}-${payment.userId}",
            at = payment.receivedAt,
            title = levy.title,
            member = payment.userName,
            amount = payment.amount,
            receiptNumber = payment.receiptNumber
        )

        fun expense(expense: Expense) = StatementEntry(
            id = "out-${expense.id}",
            at = expense.spentAt,
            title = expense.title,
            member = "",
            amount = -expense.amount
        )
    }
}

/** The totals for a run of entries. [balance] is what's left: in minus out. */
data class StatementTotals(val collected: Long, val spent: Long) {
    val balance: Long get() = collected - spent
}

fun List<StatementEntry>.totals() = StatementTotals(
    collected = filter { it.isIncome }.sumOf { it.amount },
    spent = filter { !it.isIncome }.sumOf { -it.amount }
)

/** "2026-10" for any moment in October 2026, in the device's time zone. Sorts as dates do. */
fun monthKey(millis: Long): String = formatDate(millis, "yyyy-MM")

/**
 * The months the statement can show, newest first: the current one, even if it's empty, and
 * every month with an entry. Each is keyed by [monthKey] and holds one moment inside it, for labels.
 */
fun statementMonths(entries: List<StatementEntry>, now: Long): List<Pair<String, Long>> =
    (entries.map { it.at } + now)
        .groupBy(::monthKey)
        .map { (key, moments) -> key to moments.first() }
        .sortedByDescending { it.first }

/**
 * The entries as a spreadsheet, oldest first, ending with the totals. Amounts are plain
 * numbers so spreadsheets can add them up.
 */
fun statementCsv(entries: List<StatementEntry>, estateName: String, period: String): String {
    val rows = mutableListOf(
        listOf("$estateName statement", period),
        emptyList(),
        listOf("Date", "Type", "Description", "Member", "Amount (NGN)", "Receipt no.")
    )
    entries.sortedBy { it.at }.forEach { entry ->
        rows += listOf(
            formatDate(entry.at, "yyyy-MM-dd"),
            if (entry.isIncome) "Payment" else "Expense",
            entry.title,
            entry.member,
            entry.amount.toString(),
            entry.receiptNumber
        )
    }
    val totals = entries.totals()
    rows += emptyList<String>()
    rows += listOf("", "", "Total collected", "", totals.collected.toString(), "")
    rows += listOf("", "", "Total spent", "", (-totals.spent).toString(), "")
    rows += listOf("", "", "Balance", "", totals.balance.toString(), "")
    return rows.joinToString("\r\n") { row -> row.joinToString(",") { csvField(it) } }
}

/** Quoted when it has to be; a leading = + - or @ is defused so a spreadsheet won't run it as a formula. */
private fun csvField(value: String): String {
    val safe = if (value.firstOrNull() in setOf('=', '+', '-', '@') && value.toLongOrNull() == null) "'$value" else value
    return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
}
