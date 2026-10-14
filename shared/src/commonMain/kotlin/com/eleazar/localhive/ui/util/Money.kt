package com.eleazar.localhive.ui.util

/** Whole naira with thousands separators: 1500000 -> "₦1,500,000". */
fun formatNaira(amount: Long): String {
    val digits = kotlin.math.abs(amount).toString()
    val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
    return (if (amount < 0) "-₦" else "₦") + grouped
}

/** Reads an amount typed by a person ("5,000", "₦5000", " 5 000 ") as whole naira, or null. */
fun parseNaira(text: String): Long? {
    val digits = text.filter { it.isDigit() }
    if (digits.isEmpty() || digits.length > 12) return null
    return digits.toLongOrNull()
}
