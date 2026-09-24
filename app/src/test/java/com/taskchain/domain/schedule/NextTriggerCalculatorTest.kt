package com.taskchain.domain.schedule

import com.taskchain.domain.model.ScheduleFrequency
import com.taskchain.domain.model.ScheduleRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Verifies recurrence boundaries without Android framework dependencies. */
class NextTriggerCalculatorTest {
    private val zone = TimeZone.getTimeZone("America/New_York")
    private val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { timeZone = zone }

    /** Use this function to verify a passed daily time moves into the next calendar day. */
    @Test
    fun dailyRollsToTomorrowAfterTodaysTime() {
        val now = format.parse("2026-01-31 10:00")!!.time
        val result = NextTriggerCalculator.nextTriggerEpochMillis(ScheduleRule(ScheduleFrequency.DAILY, 9, 0), now, zone)
        assertEquals("2026-02-01 09:00", format.format(Date(result!!)))
    }

    /** Use this function to verify stored ISO weekdays map to Android calendar days. */
    @Test
    fun selectedDaysUseIsoMondayThroughSunday() {
        val now = format.parse("2026-02-01 10:00")!!.time
        val result = NextTriggerCalculator.nextTriggerEpochMillis(
            ScheduleRule(ScheduleFrequency.SELECTED_DAYS, 9, 0, daysOfWeek = setOf(1)), now, zone,
        )
        assertEquals("2026-02-02 09:00", format.format(Date(result!!)))
    }

    /** Use this function to verify expired one-time schedules do not rearm. */
    @Test
    fun onceInThePastHasNoTrigger() {
        val now = format.parse("2026-01-01 10:00")!!.time
        assertNull(NextTriggerCalculator.nextTriggerEpochMillis(ScheduleRule(ScheduleFrequency.ONCE, 0, 0, oneTimeEpochMillis = now), now, zone))
    }

    /** Use this function to verify a future one-time schedule preserves its stored instant. */
    @Test
    fun futureOnceUsesStoredEpoch() {
        val now = format.parse("2026-01-01 10:00")!!.time
        val trigger = format.parse("2026-01-02 08:30")!!.time
        assertEquals(trigger, NextTriggerCalculator.nextTriggerEpochMillis(
            ScheduleRule(ScheduleFrequency.ONCE, 8, 30, oneTimeEpochMillis = trigger), now, zone,
        ))
    }

    /** Use this function to verify weekday schedules skip a weekend after Friday. */
    @Test
    fun weekdaysSkipWeekend() {
        val now = format.parse("2026-01-02 10:00")!!.time
        val result = NextTriggerCalculator.nextTriggerEpochMillis(ScheduleRule(ScheduleFrequency.WEEKDAYS, 9, 0), now, zone)
        assertEquals("2026-01-05 09:00", format.format(Date(result!!)))
    }

    /** Use this function to verify ISO Sunday selection wraps to the following Sunday. */
    @Test
    fun selectedSundayWrapsToFollowingWeek() {
        val now = format.parse("2026-01-05 10:00")!!.time
        val result = NextTriggerCalculator.nextTriggerEpochMillis(
            ScheduleRule(ScheduleFrequency.SELECTED_DAYS, 9, 0, daysOfWeek = setOf(7)), now, zone,
        )
        assertEquals("2026-01-11 09:00", format.format(Date(result!!)))
    }

    /** Use this function to verify a missing DST wall-clock time resolves to a future valid time. */
    @Test
    fun springForwardUsesTheNextValidLocalTime() {
        val now = format.parse("2026-03-08 00:00")!!.time
        val result = NextTriggerCalculator.nextTriggerEpochMillis(ScheduleRule(ScheduleFrequency.DAILY, 2, 30), now, zone)
        assertEquals("2026-03-08 03:30", format.format(Date(result!!)))
    }

    /** Use this function to verify invalid or empty recurrence rules produce no alarm. */
    @Test
    fun invalidTimeOrEmptySelectionHasNoTrigger() {
        val now = format.parse("2026-01-01 10:00")!!.time
        assertNull(NextTriggerCalculator.nextTriggerEpochMillis(ScheduleRule(ScheduleFrequency.DAILY, 24, 0), now, zone))
        assertNull(NextTriggerCalculator.nextTriggerEpochMillis(ScheduleRule(ScheduleFrequency.SELECTED_DAYS, 9, 0), now, zone))
    }

    /** Use this function to verify a repeated prompt yields to the next regular task occurrence. */
    @Test
    fun repeatedPromptYieldsToNextOccurrence() {
        val daily = ScheduleRule(ScheduleFrequency.DAILY, 9, 0)
        val morning = format.parse("2026-01-01 09:00")!!.time
        val beforeNext = format.parse("2026-01-02 08:55")!!.time
        assertEquals("2026-01-01 09:15", format.format(Date(NextTriggerCalculator.nextRepeatedTriggerEpochMillis(daily, morning, 15, zone)!!)))
        assertEquals("2026-01-02 09:00", format.format(Date(NextTriggerCalculator.nextRepeatedTriggerEpochMillis(daily, beforeNext, 15, zone)!!)))
        val once = ScheduleRule(ScheduleFrequency.ONCE, 9, 0, oneTimeEpochMillis = morning)
        assertEquals("2026-01-01 09:15", format.format(Date(NextTriggerCalculator.nextRepeatedTriggerEpochMillis(once, morning, 15, zone)!!)))
    }

    /** Use this function to verify a completion before today's schedule skips today's occurrence. */
    @Test
    fun completionBeforeDailyScheduleMovesToTomorrow() {
        val completedAt = format.parse("2026-01-31 08:00")!!.time
        val result = NextTriggerCalculator.nextTriggerAfterCompletionEpochMillis(
            ScheduleRule(ScheduleFrequency.DAILY, 9, 0), completedAt, zone,
        )
        assertEquals("2026-02-01 09:00", format.format(Date(result!!)))
    }

    /** Use this function to verify a Friday completion skips the same-day weekday occurrence. */
    @Test
    fun completionBeforeWeekdayScheduleMovesToMonday() {
        val completedAt = format.parse("2026-01-02 08:00")!!.time
        val result = NextTriggerCalculator.nextTriggerAfterCompletionEpochMillis(
            ScheduleRule(ScheduleFrequency.WEEKDAYS, 9, 0), completedAt, zone,
        )
        assertEquals("2026-01-05 09:00", format.format(Date(result!!)))
    }

    /** Use this function to verify a selected day completion moves to the next selected day. */
    @Test
    fun completionBeforeSelectedDayMovesToNextSelectedDay() {
        val completedAt = format.parse("2026-01-05 08:00")!!.time
        val result = NextTriggerCalculator.nextTriggerAfterCompletionEpochMillis(
            ScheduleRule(ScheduleFrequency.SELECTED_DAYS, 9, 0, daysOfWeek = setOf(1)), completedAt, zone,
        )
        assertEquals("2026-01-12 09:00", format.format(Date(result!!)))
    }

    /** Use this function to verify completion never rearms a one-time schedule. */
    @Test
    fun completionOfOnceHasNoTrigger() {
        val completedAt = format.parse("2026-01-01 08:00")!!.time
        assertNull(
            NextTriggerCalculator.nextTriggerAfterCompletionEpochMillis(
                ScheduleRule(ScheduleFrequency.ONCE, 9, 0, oneTimeEpochMillis = completedAt), completedAt, zone,
            ),
        )
    }
}
