package com.taskchain.core.schedule

import java.util.Calendar
import java.util.TimeZone

/**
 * Use this function to find the last local day start at or before [epochMillis].
 * A TaskChain day begins at [dayStartHour] local time (`home_day_start_hour`, default 2 AM), so a
 * timestamp before that hour belongs to the previous calendar date. Pure; depends only on [timeZone].
 */
fun dayStartAtOrBeforeEpochMillis(
    epochMillis: Long,
    dayStartHour: Int,
    timeZone: TimeZone = TimeZone.getDefault(),
): Long = Calendar.getInstance(timeZone).apply {
    timeInMillis = epochMillis
    add(Calendar.HOUR_OF_DAY, -dayStartHour)
    set(Calendar.HOUR_OF_DAY, dayStartHour)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
