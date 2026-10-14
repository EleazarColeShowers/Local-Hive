package com.eleazar.localhive.ui.utilities

import com.eleazar.localhive.domain.model.UtilityReport

/** How much of a span a supply was on for, and how much of it anyone knew about. */
data class Uptime(val onMillis: Long, val knownMillis: Long)

/**
 * How long a supply was on during the [windowMillis] before [now], going by [reports] of that
 * supply in any order. Each report holds until the next one. Before the first report nobody
 * knows, so that stretch counts towards neither total. Null when nothing was reported in or
 * before the window.
 */
fun uptime(reports: List<UtilityReport>, windowMillis: Long, now: Long): Uptime? {
    val start = now - windowMillis
    val sorted = reports.filter { it.at <= now }.sortedBy { it.at }
    // The report in force as the window opens, if any, then everything during it.
    val before = sorted.lastOrNull { it.at <= start }
    val during = sorted.filter { it.at > start }
    if (before == null && during.isEmpty()) return null

    var on = 0L
    var known = 0L
    var from = if (before != null) start else during.first().at
    var isOn = before?.isOn ?: during.first().isOn
    for (report in during) {
        val span = report.at - from
        known += span
        if (isOn) on += span
        from = report.at
        isOn = report.isOn
    }
    known += now - from
    if (isOn) on += now - from
    return Uptime(onMillis = on, knownMillis = known)
}

/** "6h", "45m" or "2h 30m". */
fun shortDuration(millis: Long): String {
    val minutes = (millis / 60_000L).coerceAtLeast(0L)
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0L -> "${rest}m"
        rest == 0L -> "${hours}h"
        else -> "${hours}h ${rest}m"
    }
}
