package com.eleazar.localhive.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoBitrateTest {
    @Test
    fun shortClipsGetTheSharpestBitrate() {
        assertEquals(2_000_000, videoBitrateFor(5_000))
        assertEquals(2_000_000, videoBitrateFor(60_000))
    }

    @Test
    fun theLongestVideoAllowedStillFitsUnder40Mb() {
        val seconds = 180
        val bytes = (videoBitrateFor(seconds * 1000L) + 96_000L) * seconds / 8
        assertTrue(bytes < 40L * 1024 * 1024, "$bytes bytes")
    }

    @Test
    fun anUnknownLengthGetsAMiddlingBitrate() {
        assertEquals(1_200_000, videoBitrateFor(-1))
    }
}
