package com.taskchain.core.schedule

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies the shared local day start used by Home and Progress. */
class DayStartTest {
    /** Use this function to verify a time before the day-start hour belongs to the previous day. */
    @Test
    fun timeBeforeDayStartBelongsToPreviousDay() {
        val utc = TimeZone.getTimeZone("UTC")
        assertEquals(7L * DAY + 2L * HOUR, dayStartAtOrBeforeEpochMillis(8L * DAY + HOUR, 2, utc))
        assertEquals(8L * DAY + 2L * HOUR, dayStartAtOrBeforeEpochMillis(8L * DAY + 2L * HOUR, 2, utc))
        assertEquals(8L * DAY + 2L * HOUR, dayStartAtOrBeforeEpochMillis(8L * DAY + 23L * HOUR, 2, utc))
    }

    /** Use this function to verify a zero hour keeps local midnight and the time zone shifts the boundary. */
    @Test
    fun hourZeroIsMidnightInTheGivenZone() {
        assertEquals(8L * DAY, dayStartAtOrBeforeEpochMillis(8L * DAY + HOUR, 0, TimeZone.getTimeZone("UTC")))
        val plusOne = TimeZone.getTimeZone("GMT+01:00")
        assertEquals(8L * DAY + HOUR, dayStartAtOrBeforeEpochMillis(8L * DAY + 2L * HOUR + 30L * 60_000L, 2, plusOne))
    }

    private companion object {
        const val HOUR = 3_600_000L
        const val DAY = 24L * HOUR
    }
}
