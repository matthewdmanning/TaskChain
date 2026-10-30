package com.taskchain.features.notifications.domain

import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RoutineRunTask
import com.taskchain.core.model.RoutineTask
import com.taskchain.core.model.RoutineTaskId
import com.taskchain.features.notifications.NotificationNudge
import com.taskchain.features.notifications.NotificationRunRules

/**
 * Use this function to fake the injected Running contract in Notifications unit tests.
 * Every capability defaults to an inert value: no timer, no elapsed time, unchanged runs, no nudge.
 * The real Running binding is covered by the root `NotificationIntegrationTest`.
 */
internal fun fakeRunRules(
    remainingMillis: (RoutineRun, Long) -> Long? = { _, _ -> null },
    subtaskRemainingMillis: (RoutineRun, Long) -> Long? = { _, _ -> null },
    elapsedMillis: (RoutineRun, Long) -> Long = { _, _ -> 0L },
    plannedRunMillis: (RoutineRun) -> Long = { 0L },
    skipCurrent: (RoutineRun, Long) -> RoutineRun = { run, _ -> run },
    completeCurrent: (RoutineRun, Long) -> RoutineRun = { run, _ -> run },
    needsTimerFeedback: (RoutineRun, Long) -> Boolean = { _, _ -> false },
    acknowledgeTimerFeedback: (RoutineRun, Long) -> RoutineRun = { run, _ -> run },
    evaluateNudge: (RoutineRun, Long, Long, Boolean) -> NotificationNudge = { run, _, _, _ ->
        NotificationNudge(run, shouldFire = false)
    },
    nextNudgeAt: (RoutineRun, Long) -> Long? = { _, _ -> null },
): NotificationRunRules = NotificationRunRules(
    remainingMillis = remainingMillis,
    subtaskRemainingMillis = subtaskRemainingMillis,
    elapsedMillis = elapsedMillis,
    plannedRunMillis = plannedRunMillis,
    skipCurrent = skipCurrent,
    completeCurrent = completeCurrent,
    needsTimerFeedback = needsTimerFeedback,
    acknowledgeTimerFeedback = acknowledgeTimerFeedback,
    evaluateNudge = evaluateNudge,
    nextNudgeAt = nextNudgeAt,
)

/** Use this function to hand-build an active run on [currentTaskIndex] whose first task started at zero by default. */
internal fun fakeRun(
    tasks: List<RoutineRunTask> = listOf(RoutineRunTask(source = RoutineTask(RoutineTaskId("task"), "Task"), startedAtEpochMillis = 0)),
    currentTaskIndex: Int = 0,
): RoutineRun = RoutineRun(
    id = RoutineRunId("run"),
    routineId = RoutineId("routine"),
    routineTitle = "Morning",
    tasks = tasks,
    currentTaskIndex = currentTaskIndex,
    startedAtEpochMillis = 0,
)
