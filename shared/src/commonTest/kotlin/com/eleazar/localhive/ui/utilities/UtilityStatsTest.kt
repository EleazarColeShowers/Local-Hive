package com.eleazar.localhive.ui.utilities

import com.eleazar.localhive.domain.model.UtilityReport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UtilityStatsTest {
    private val hour = 60 * 60 * 1000L
    private val day = 24 * hour
    private val now = 100 * day

    private fun report(hoursAgo: Long, isOn: Boolean) = UtilityReport(isOn = isOn, at = now - hoursAgo * hour)

    @Test
    fun nothingReportedIsUnknown() {
        assertNull(uptime(emptyList(), day, now))
    }

    @Test
    fun aReportBeforeTheWindowCoversItAll() {
        assertEquals(Uptime(onMillis = day, knownMillis = day), uptime(listOf(report(30, true)), day, now))
        assertEquals(Uptime(onMillis = 0, knownMillis = day), uptime(listOf(report(30, false)), day, now))
    }

    @Test
    fun eachReportHoldsUntilTheNext() {
        // Off from before the window, on 10 hours ago, off again 4 hours ago: on for 6 hours.
        val reports = listOf(report(30, false), report(10, true), report(4, false))
        assertEquals(Uptime(onMillis = 6 * hour, knownMillis = day), uptime(reports, day, now))
    }

    @Test
    fun timeBeforeTheFirstReportIsntCounted() {
        val reports = listOf(report(5, true), report(2, false))
        assertEquals(Uptime(onMillis = 3 * hour, knownMillis = 5 * hour), uptime(reports, day, now))
    }

    @Test
    fun repeatsAndOrderDontMatter() {
        val reports = listOf(report(1, true), report(3, true), report(30, false))
        assertEquals(Uptime(onMillis = 3 * hour, knownMillis = day), uptime(reports, day, now))
    }

    @Test
    fun durationsReadNaturally() {
        assertEquals("45m", shortDuration(45 * 60_000L))
        assertEquals("6h", shortDuration(6 * hour))
        assertEquals("2h 30m", shortDuration(150 * 60_000L))
    }
}
