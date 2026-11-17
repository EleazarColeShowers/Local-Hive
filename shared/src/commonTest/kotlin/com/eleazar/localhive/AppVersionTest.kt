package com.eleazar.localhive

import com.eleazar.localhive.domain.isOutdated
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppVersionTest {
    @Test
    fun olderVersionsMustUpdate() {
        assertTrue(isOutdated("1.06", "1.07"))
        assertTrue(isOutdated("1.9", "1.10"))
        assertTrue(isOutdated("1.0", "1.0.1"))
    }

    @Test
    fun currentAndNewerVersionsCarryOn() {
        assertFalse(isOutdated("1.07", "1.07"))
        assertFalse(isOutdated("1.10", "1.9"))
        assertFalse(isOutdated("2.0", "1.07"))
    }

    @Test
    fun aMissingOrMistypedMinimumNeverLocksAnyoneOut() {
        assertFalse(isOutdated("1.06", ""))
        assertFalse(isOutdated("1.06", "1.o7"))
        assertFalse(isOutdated("", "1.07"))
    }
}
