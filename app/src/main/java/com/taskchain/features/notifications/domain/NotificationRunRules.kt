package com.taskchain.features.notifications.domain

import com.taskchain.core.model.RoutineRun

/** Run capabilities supplied by the composition root; Notifications owns no run engine. */
class NotificationRunRules(
    val remainingMillis: (RoutineRun, Long) -> Long?,
    val subtaskRemainingMillis: (RoutineRun, Long) -> Long?,
    val elapsedMillis: (RoutineRun, Long) -> Long,
    val plannedRunMillis: (RoutineRun) -> Long,
    val skipCurrent: (RoutineRun, Long) -> RoutineRun,
    val completeCurrent: (RoutineRun, Long) -> RoutineRun,
    val needsTimerFeedback: (RoutineRun, Long) -> Boolean,
    val acknowledgeTimerFeedback: (RoutineRun, Long) -> RoutineRun,
    val evaluateNudge: (RoutineRun, Long, Long, Boolean) -> NotificationNudge,
)

/** Persisted nudge acknowledgement plus whether this delivery should emit feedback. */
data class NotificationNudge(val run: RoutineRun, val shouldFire: Boolean)
