package com.taskchain.domain.schedule

import com.taskchain.domain.model.ScheduleFrequency
import com.taskchain.domain.model.ScheduleRule
import java.util.Calendar
import java.util.TimeZone

/** Calculates the next local wall-clock occurrence for a platform-neutral schedule. */
object NextTriggerCalculator {
    /** Use this function to turn a saved schedule into its next future epoch-millisecond trigger. */
    fun nextTriggerEpochMillis(
        rule: ScheduleRule,
        nowEpochMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): Long? {
        if (rule.localHour !in 0..23 || rule.localMinute !in 0..59) return null
        if (rule.frequency == ScheduleFrequency.ONCE) {
            return rule.oneTimeEpochMillis?.takeIf { it > nowEpochMillis }
        }
        val allowedDays = when (rule.frequency) {
            ScheduleFrequency.DAILY -> (Calendar.SUNDAY..Calendar.SATURDAY).toSet()
            ScheduleFrequency.WEEKDAYS -> (Calendar.MONDAY..Calendar.FRIDAY).toSet()
            ScheduleFrequency.SELECTED_DAYS -> rule.daysOfWeek
                .filter { it in 1..7 }
                .map { (it % 7) + Calendar.SUNDAY }
                .toSet()
            ScheduleFrequency.ONCE -> emptySet()
        }
        if (allowedDays.isEmpty()) return null
        for (offset in 0..7) {
            val candidate = Calendar.getInstance(timeZone).apply {
                timeInMillis = nowEpochMillis
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, rule.localHour)
                set(Calendar.MINUTE, rule.localMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (candidate.get(Calendar.DAY_OF_WEEK) in allowedDays && candidate.timeInMillis > nowEpochMillis) {
                return candidate.timeInMillis
            }
        }
        return null
    }

    /** Use this function after completing a scheduled routine to start recurrence on a later local calendar day. */
    fun nextTriggerAfterCompletionEpochMillis(
        rule: ScheduleRule,
        completedAtEpochMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): Long? {
        if (rule.frequency == ScheduleFrequency.ONCE) return null
        val nextDay = Calendar.getInstance(timeZone).apply {
            timeInMillis = completedAtEpochMillis
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return nextTriggerEpochMillis(rule, nextDay - 1, timeZone)
    }

    /** Use this function when a scheduled routine repeats prompts but must yield to its next regular occurrence. */
    fun nextRepeatedTriggerEpochMillis(
        rule: ScheduleRule,
        deliveredAtEpochMillis: Long,
        minutes: Int,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): Long? {
        if (minutes <= 0) return nextTriggerEpochMillis(rule, deliveredAtEpochMillis, timeZone)
        if (rule.localHour !in 0..23 || rule.localMinute !in 0..59) return null
        val regular = nextTriggerEpochMillis(rule, deliveredAtEpochMillis, timeZone)
        if (rule.frequency != ScheduleFrequency.ONCE && regular == null) return null
        if (rule.frequency == ScheduleFrequency.ONCE && rule.oneTimeEpochMillis == null) return null
        val repeated = deliveredAtEpochMillis + minutes.toLong() * MILLIS_PER_MINUTE
        if (repeated <= deliveredAtEpochMillis) return null
        return regular?.takeIf { it <= repeated } ?: repeated
    }

    private const val MILLIS_PER_MINUTE = 60_000L
}
