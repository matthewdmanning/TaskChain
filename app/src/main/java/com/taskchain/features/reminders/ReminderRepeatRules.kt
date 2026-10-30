package com.taskchain.features.reminders

import com.taskchain.core.model.CompletionEvent
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RunStatus
import com.taskchain.core.model.ScheduleFrequency
import com.taskchain.core.model.completedAtOrNull
import com.taskchain.core.schedule.NextTriggerCalculator
import com.taskchain.core.schedule.ReminderRequest

/**
 * Checks whether a task completed during the current reminder cycle.
 * Use this function before rearming repeated task prompts; inputs are routine/task ids, cycle start, active
 * run, and completion history, and it depends only on persisted run timestamps and statuses.
 */
internal fun completedSinceCycle(
    routineId: String,
    taskId: String,
    cycleStartEpochMillis: Long,
    activeRun: RoutineRun?,
    history: List<CompletionEvent>,
): Boolean {
    if (cycleStartEpochMillis <= 0) return false
    val activeCompleted = activeRun?.takeIf { it.routineId.value == routineId }?.tasks?.any { task ->
        task.source.id.value == taskId && (task.completedAtOrNull() ?: Long.MIN_VALUE) >= cycleStartEpochMillis
    } == true
    return activeCompleted || history.any { event ->
        event.routineId.value == routineId && event.tasks.any { task ->
            task.source.id.value == taskId && (task.completedAtOrNull() ?: Long.MIN_VALUE) >= cycleStartEpochMillis
        }
    }
}

/**
 * Checks whether a routine completed during the current reminder cycle.
 * Use this function before rearming repeated routine prompts; inputs are the routine id, cycle start, active
 * run, and completion history, and it depends on completed status plus the run's completion time.
 */
internal fun completedRoutineSinceCycle(
    routineId: String,
    cycleStartEpochMillis: Long,
    activeRun: RoutineRun?,
    history: List<CompletionEvent>,
): Boolean {
    if (cycleStartEpochMillis <= 0) return false
    val activeCompleted = activeRun?.takeIf { it.routineId.value == routineId }
        ?.completedAtOrNull()?.let { it >= cycleStartEpochMillis } == true
    return activeCompleted || history.any { event ->
        event.routineId.value == routineId && event.status == RunStatus.COMPLETED &&
            event.endedAtEpochMillis >= cycleStartEpochMillis
    }
}

/**
 * Rearm policy for a delivered routine reminder: repeats inside the repeat window, then the next regular occurrence.
 * Use it from reminder delivery and Snooze/Dismiss handling; it depends only on [NextTriggerCalculator].
 */
internal object ReminderDelivery {
    /** Repeats stop this long after the scheduled time; the next regular occurrence resumes after that. */
    const val REPEAT_WINDOW_MILLIS = 2 * 60 * 60 * 1000L

    /**
     * Computes a delivery rearm request from a delivered request.
     * Use this function in the receiver with the request, current epoch, and whether an immediate repeat is
     * wanted; it depends on [NextTriggerCalculator] and returns null for missing/expired policy.
     */
    fun afterDelivery(
        request: ReminderRequest,
        nowEpochMillis: Long,
        repeatNow: Boolean = true,
    ): ReminderRequest? {
        val rule = request.schedule ?: return null
        val minutes = request.remindEveryMinutes?.takeIf { it > 0 }
        if (rule.frequency == ScheduleFrequency.ONCE && minutes == null) return null
        val regular = NextTriggerCalculator.nextTriggerEpochMillis(rule, nowEpochMillis)
        val windowEnd = request.cycleStartEpochMillis?.takeIf { it > 0 }?.plus(REPEAT_WINDOW_MILLIS)
        val repeated = NextTriggerCalculator.nextRepeatedTriggerEpochMillis(rule, nowEpochMillis, minutes ?: 0)
        val next = if (repeatNow && (windowEnd == null || repeated.let { it != null && it <= windowEnd })) {
            repeated
        } else {
            regular
        } ?: return null
        val cycleStart = if (!repeatNow || next == regular) {
            next
        } else {
            request.cycleStartEpochMillis?.takeIf { it > 0 } ?: nowEpochMillis
        }
        return request.copy(
            triggerAtEpochMillis = next,
            remindEveryMinutes = minutes,
            cycleStartEpochMillis = cycleStart,
        )
    }
}
