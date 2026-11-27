package io.github.seyoon.futureetp.feature.alertsettings

import io.github.seyoon.futureetp.core.api.model.QuietTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietTimesTest {
    @Test
    fun slotsAreEveryHalfHourOfTheDay() {
        assertEquals(48, QUIET_SLOTS.size)
        assertEquals(0, QUIET_SLOTS.first())
        assertEquals(1410, QUIET_SLOTS.last())
    }

    @Test
    fun formatsAsTwentyFourHourClock() {
        assertEquals("00:00", formatQuietMinute(0))
        assertEquals("07:30", formatQuietMinute(450))
        assertEquals("23:30", formatQuietMinute(1410))
    }

    @Test
    fun crossingMidnightEndsNextDay() {
        assertTrue(QuietTime(startMin = 1380, endMin = 420).endsNextDay)
        assertFalse(QuietTime(startMin = 60, endMin = 420).endsNextDay)
    }

    @Test
    fun theOtherEdgeIsBlockedAndEditingKeepsIt() {
        val time = QuietTime(startMin = 1380, endMin = 420)
        assertEquals(420, time.blockedFor(QuietEdge.START))
        assertEquals(1380, time.blockedFor(QuietEdge.END))
        assertEquals(QuietTime(startMin = 1380, endMin = 480), time.with(QuietEdge.END, 480))
    }
}
