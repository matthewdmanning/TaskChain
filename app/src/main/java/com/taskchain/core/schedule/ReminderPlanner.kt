package com.taskchain.core.schedule

import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineTemplate
import com.taskchain.core.model.ScheduleRule

/**
 * Platform-neutral reminder request used by the Android adapter.
 * Use it when a routine or legacy task needs a complete alarm description; it contains the
 * routine identity, trigger, recurrence, feedback settings, and optional legacy task identity.
 * It depends only on the domain schedule and model types, so no Android alarm state is required.
 */
data class ReminderRequest(
    val requestCode: Int,
    val routineId: String,
    val title: String,
    val triggerAtEpochMillis: Long,
    val schedule: ScheduleRule? = null,
    val taskId: String? = null,
    val remindEveryMinutes: Int? = null,
    val cycleStartEpochMillis: Long? = null,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
)

/**
 * Describes reminder cancellations and schedules produced by one domain planning operation.
 * Use it when a caller is saving, restoring, or completing a routine; inputs are
 * the relevant routine/request and clock value, and the Android adapter applies the returned
 * codes and requests without adding scheduling policy.
 */
data class ReminderPlan(
    val cancelRequestCodes: List<Int> = emptyList(),
    val requests: List<ReminderRequest> = emptyList(),
)

/**
 * Maps one routine occurrence to the complete adapter request.
 * Use this function when a future trigger has already been selected; the input is the trigger epoch and the
 * receiver depends only on the routine's persisted identity, recurrence, title, and feedback gates.
 */
internal fun RoutineTemplate.toReminderRequest(triggerAtEpochMillis: Long): ReminderRequest = ReminderRequest(
    requestCode = id.value.hashCode(),
    routineId = id.value,
    title = title,
    triggerAtEpochMillis = triggerAtEpochMillis,
    schedule = schedule,
    remindEveryMinutes = remindEveryMinutes,
    cycleStartEpochMillis = triggerAtEpochMillis.takeIf { schedule != null },
    soundEnabled = soundEnabled,
    vibrateEnabled = vibrateEnabled,
)

/**
 * Produces platform-neutral reminder changes for routine lifecycle events.
 * Use these functions from ViewModels and broadcast receivers for save, restore and completion; inputs are
 * domain snapshots and epoch times, and the only dependency is [NextTriggerCalculator], with no Android state.
 * Delivery rearming and the repeat window belong to the Reminders feature.
 */
object ReminderPlanner {
    /**
     * Plans alarm changes before a routine save is persisted.
     * Use this function with the new routine, optional previous snapshot, and current epoch; a configured
     * schedule must resolve to a future trigger or this throws before persistence, while a direct
     * reminder timestamp is preserved as supplied.
     */
    fun forSave(
        routine: RoutineTemplate,
        previous: RoutineTemplate?,
        nowEpochMillis: Long,
    ): ReminderPlan {
        val trigger = routine.schedule?.let {
            NextTriggerCalculator.nextTriggerEpochMillis(it, nowEpochMillis)
                ?: throw IllegalArgumentException("Routine schedule is not in the future")
        } ?: routine.reminderAtEpochMillis
        val cancelCodes = buildList {
            previous?.tasks?.forEach { task -> add(legacyTaskRequestCode(task.id.value)) }
            if (trigger == null) add(routineRequestCode(routine.id))
        }
        return ReminderPlan(
            cancelRequestCodes = cancelCodes,
            requests = trigger?.let { listOf(routine.toReminderRequest(it)) } ?: emptyList(),
        )
    }

    /**
     * Plans alarm restoration after reboot or local-time changes.
     * Use this function with one saved routine and current epoch; routine-level settings suppress legacy task
     * alarms, while a routine with no settings restores each future task alarm with its own gates.
     */
    fun forRestore(routine: RoutineTemplate, nowEpochMillis: Long): ReminderPlan {
        val hasRoutineSettings = routine.schedule != null || routine.deadlineEpochMillis != null ||
            routine.reminderAtEpochMillis != null || routine.remindEveryMinutes != null
        if (hasRoutineSettings) {
            val trigger = routine.schedule?.let { NextTriggerCalculator.nextTriggerEpochMillis(it, nowEpochMillis) }
                ?: routine.reminderAtEpochMillis?.takeIf { it > nowEpochMillis }
            val cancelCodes = buildList {
                if (trigger == null) add(routineRequestCode(routine.id))
                routine.tasks.forEach { task -> add(legacyTaskRequestCode(task.id.value)) }
            }
            return ReminderPlan(
                cancelRequestCodes = cancelCodes,
                requests = trigger?.let { listOf(routine.toReminderRequest(it)) } ?: emptyList(),
            )
        }

        val cancelCodes = mutableListOf(routineRequestCode(routine.id))
        val requests = buildList {
            routine.tasks.forEach { task ->
                val requestCode = legacyTaskRequestCode(task.id.value)
                val trigger = task.schedule?.let { NextTriggerCalculator.nextTriggerEpochMillis(it, nowEpochMillis) }
                    ?: task.reminderAtEpochMillis?.takeIf { it > nowEpochMillis }
                if (trigger == null) cancelCodes += requestCode
                else add(
                    ReminderRequest(
                        requestCode = requestCode,
                        routineId = routine.id.value,
                        title = task.title,
                        triggerAtEpochMillis = trigger,
                        schedule = task.schedule,
                        taskId = task.id.value,
                        remindEveryMinutes = task.remindEveryMinutes,
                        cycleStartEpochMillis = trigger.takeIf { task.schedule != null },
                        soundEnabled = task.soundEnabled,
                        vibrateEnabled = task.vibrateEnabled,
                    ),
                )
            }
        }
        return ReminderPlan(cancelRequestCodes = cancelCodes, requests = requests)
    }

    /**
     * Plans the next alarm after a routine completion.
     * Use this function with the completed routine when available, its stable id, and completion epoch; a
     * next recurring trigger schedules the routine and every other case cancels its routine alarm.
     */
    fun afterCompletion(
        routine: RoutineTemplate?,
        routineId: RoutineId,
        completedAtEpochMillis: Long,
    ): ReminderPlan {
        val next = routine?.schedule?.let {
            NextTriggerCalculator.nextTriggerAfterCompletionEpochMillis(it, completedAtEpochMillis)
        }
        return if (routine != null && next != null) {
            ReminderPlan(requests = listOf(routine.toReminderRequest(next)))
        } else {
            ReminderPlan(cancelRequestCodes = listOf(routineRequestCode(routineId)))
        }
    }

    /** Use this function to map a stable RoutineId to the alarm code used by request mapping and cancellation. */
    fun routineRequestCode(routineId: RoutineId): Int = routineId.value.hashCode()

    /** Use this function to map a task id to the legacy alarm code used before settings moved to the routine. */
    private fun legacyTaskRequestCode(taskId: String): Int = "step:$taskId".hashCode()
}
