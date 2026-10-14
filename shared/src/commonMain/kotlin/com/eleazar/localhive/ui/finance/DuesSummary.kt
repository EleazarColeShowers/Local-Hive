package com.eleazar.localhive.ui.finance

import com.eleazar.localhive.domain.model.PaymentStatus
import com.eleazar.localhive.ui.util.formatDate
import com.eleazar.localhive.ui.util.formatNaira

/** Where the user stands on the estate's dues, in a few words for the feed's at-a-glance card. */
data class DuesSummary(
    val headline: String,
    val detail: String,
    /** Something for the user to do: money owed, or proof waiting for the treasurer. */
    val needsAttention: Boolean
)

/**
 * Owing comes first, since it's the user's own money. The treasurer then sees proof waiting for
 * them; everyone else, proof still being checked, or that they're all paid up.
 */
fun duesSummary(levies: List<LevyRow>, isTreasurer: Boolean): DuesSummary {
    if (levies.isEmpty()) return DuesSummary("No dues", "Nothing has been set", needsAttention = false)

    val owed = levies.filter { it.myStatus == null || it.myStatus == PaymentStatus.REJECTED }
    if (owed.isNotEmpty()) {
        val total = owed.sumOf { it.levy.amount }
        val next = owed.filter { it.levy.dueDate > 0L }.minByOrNull { it.levy.dueDate }
        val detail = when {
            owed.size > 1 -> "${owed.size} levies" + (next?.let { " · next due ${formatDate(it.levy.dueDate, "MMM d")}" } ?: "")
            next != null -> "${owed[0].levy.title.ifBlank { "Levy" }} · due ${formatDate(next.levy.dueDate, "MMM d")}"
            else -> owed[0].levy.title.ifBlank { "Levy" }
        }
        return DuesSummary("${formatNaira(total)} due", detail, needsAttention = true)
    }

    val toReview = levies.sumOf { it.toReview }
    if (isTreasurer && toReview > 0) {
        return DuesSummary(
            if (toReview == 1) "1 to review" else "$toReview to review",
            "Proof of payment waiting",
            needsAttention = true
        )
    }
    if (levies.any { it.myStatus == PaymentStatus.PENDING }) {
        return DuesSummary("Proof sent", "The treasurer is checking it", needsAttention = false)
    }
    return DuesSummary("All paid", "You're up to date", needsAttention = false)
}
