package com.eleazar.localhive.ui.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CharactersLeftTest {

    @Test
    fun longLimitsShowTheLast200() {
        assertFalse(nearLimit(length = 1799, max = 2000))
        assertTrue(nearLimit(length = 1800, max = 2000))
        assertTrue(nearLimit(length = 5000, max = 5000))
    }

    @Test
    fun shortLimitsShowTheLastFifth() {
        assertFalse(nearLimit(length = 159, max = 200))
        assertTrue(nearLimit(length = 160, max = 200))
        assertFalse(nearLimit(length = 0, max = 500))
        assertTrue(nearLimit(length = 400, max = 500))
    }
}
