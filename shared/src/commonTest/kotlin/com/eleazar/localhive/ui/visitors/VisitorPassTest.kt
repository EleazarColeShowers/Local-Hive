package com.eleazar.localhive.ui.visitors

import com.eleazar.localhive.domain.model.GUARD_CODE_LENGTH
import com.eleazar.localhive.domain.model.PASS_CODE_LENGTH
import com.eleazar.localhive.domain.model.PassStage
import com.eleazar.localhive.domain.model.VisitorPass
import com.eleazar.localhive.domain.model.isValidCode
import com.eleazar.localhive.domain.model.normalizeCode
import com.eleazar.localhive.domain.model.randomCode
import com.eleazar.localhive.ui.util.startOfDay
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VisitorPassTest {
    private val hour = 60 * 60 * 1000L

    // Must match the Firestore rules' and the Worker's ^[A-HJ-NP-Z2-9]{n}$.
    private val ruleCode = Regex("^[A-HJ-NP-Z2-9]+$")

    @Test
    fun codesUseOnlyCharactersTheRulesAccept() {
        val random = Random(42)
        repeat(500) {
            val pass = randomCode(PASS_CODE_LENGTH, random)
            val guard = randomCode(GUARD_CODE_LENGTH, random)
            assertTrue(ruleCode.matches(pass) && pass.length == PASS_CODE_LENGTH, pass)
            assertTrue(ruleCode.matches(guard) && guard.length == GUARD_CODE_LENGTH, guard)
        }
    }

    @Test
    fun typedCodesAreTidied() {
        assertEquals("K7MQ4X", normalizeCode(" k7m-q4x "))
        assertTrue(isValidCode(normalizeCode("k7mq4x"), PASS_CODE_LENGTH))
        // 0, O, 1 and I are never in a code.
        assertFalse(isValidCode("K7MQ4O", PASS_CODE_LENGTH))
        assertFalse(isValidCode("K7MQ4", PASS_CODE_LENGTH))
    }

    @Test
    fun aDayWindowIsOneWholeDay() {
        val now = startOfDay(1_700_000_000_000L) + 15 * hour
        val (from, until) = dayWindow(0, now)
        assertEquals(startOfDay(now), from)
        assertTrue(now in from until until)
        assertTrue(until - from in 23 * hour..25 * hour)
        val (tomorrow, _) = dayWindow(1, now)
        assertEquals(until, tomorrow)
    }

    @Test
    fun aPassIsUsableOnlyOnItsDayAndOnlyOnce() {
        val now = 1_700_000_000_000L
        val pass = VisitorPass(code = "K7MQ4X", validFrom = now - hour, validUntil = now + hour)
        assertTrue(pass.canCheckIn(now))
        assertFalse(pass.canCheckIn(now + 2 * hour))
        assertTrue(pass.isExpired(now + 2 * hour))
        assertFalse(pass.copy(validFrom = now + hour, validUntil = now + 3 * hour).canCheckIn(now))
        val inside = pass.copy(stage = PassStage.INSIDE)
        assertFalse(inside.canCheckIn(now))
        assertTrue(inside.isCurrent(now + 5 * hour))
        assertFalse(pass.copy(stage = PassStage.LEFT).isCurrent(now))
        assertFalse(pass.copy(stage = PassStage.CANCELLED).canCheckIn(now))
    }
}
