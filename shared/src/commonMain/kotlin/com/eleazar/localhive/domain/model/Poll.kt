package com.eleazar.localhive.domain.model

/**
 * A question an admin puts to the estate. Each member votes once, and votes can't be changed;
 * only the tallies are shared, never who voted for what.
 */
data class Poll(
    val id: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val createdBy: String = "",
    val createdByName: String = "",
    val createdAt: Long = 0L,
    /** When voting stops, or 0 to stay open until an admin closes it. */
    val closesAt: Long = 0L,
    val closed: Boolean = false,
    /** Votes per option, in the same order as [options]. */
    val counts: List<Int> = emptyList(),
    val voteCount: Int = 0
) {
    fun isOpen(now: Long): Boolean = !closed && (closesAt == 0L || now < closesAt)
}
