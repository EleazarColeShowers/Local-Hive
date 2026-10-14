package com.eleazar.localhive.ui.util

/** Wall-clock time in epoch millis. */
expect fun nowMillis(): Long

/** Formats [millis] in the device's locale and time zone, using a CLDR [pattern] such as "MMM d". */
expect fun formatDate(millis: Long, pattern: String): String

/** Midnight, in the device's time zone, of the day containing [millis]. */
expect fun startOfDay(millis: Long): Long

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR

/** Compact age for list rows: "now", "5m", "3h", "Yesterday", then a date. */
fun relativeTime(millis: Long, now: Long = nowMillis()): String {
    if (millis <= 0L) return ""
    val diff = (now - millis).coerceAtLeast(0L)
    return when {
        diff < MINUTE -> "now"
        diff < HOUR -> "${diff / MINUTE}m"
        diff < DAY && startOfDay(millis) == startOfDay(now) -> "${diff / HOUR}h"
        startOfDay(millis) == startOfDay(now) - DAY -> "Yesterday"
        diff < 7 * DAY -> formatDate(millis, "EEE")
        else -> formatDate(millis, "MMM d")
    }
}

/** Time of day for a message bubble, e.g. "3:42 PM". */
fun clockTime(millis: Long): String = if (millis <= 0L) "" else formatDate(millis, "h:mm a")

/** Heading for a run of messages sent on the same day. */
fun dayLabel(millis: Long, now: Long = nowMillis()): String {
    val day = startOfDay(millis)
    val today = startOfDay(now)
    return when {
        day == today -> "Today"
        day == startOfDay(today - 1) -> "Yesterday"
        now - millis < 7 * DAY -> formatDate(millis, "EEEE")
        formatDate(millis, "yyyy") == formatDate(now, "yyyy") -> formatDate(millis, "EEE, MMM d")
        else -> formatDate(millis, "MMM d, yyyy")
    }
}
