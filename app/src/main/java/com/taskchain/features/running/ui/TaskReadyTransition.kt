package com.taskchain.features.running.ui

import com.taskchain.core.model.RoutineRun
import com.taskchain.features.running.domain.RunReadiness

/** Represents the visible label during the five-second transition into the next routine task. */
internal enum class TaskReadyPhase(val label: String) {
    GET_READY("Get Ready"),
    THREE("3"),
    TWO("2"),
    ONE("1"),
}

/** Use this function to tell the numbered countdown steps (3, 2, 1), which pulse, from the Get Ready lead-in. */
internal fun TaskReadyPhase.isCountdownNumber(): Boolean = this != TaskReadyPhase.GET_READY

/** Use this function to map elapsed transition time to the label shown before a task starts.
 * Inputs: `elapsedMillis` — elapsed time since the next task became current.
 * Dependencies: `RunReadiness.taskReadyGetReadyDurationMillis`, `RunReadiness.taskReadyPhaseDurationMillis`, and
 * `RunReadiness.taskReadyTransitionDurationMillis`.
 */
internal fun taskReadyPhase(elapsedMillis: Long): TaskReadyPhase? = when {
    elapsedMillis < 0L -> TaskReadyPhase.GET_READY
    elapsedMillis < RunReadiness.taskReadyGetReadyDurationMillis -> TaskReadyPhase.GET_READY
    elapsedMillis < RunReadiness.taskReadyGetReadyDurationMillis + RunReadiness.taskReadyPhaseDurationMillis -> TaskReadyPhase.THREE
    elapsedMillis < RunReadiness.taskReadyGetReadyDurationMillis + RunReadiness.taskReadyPhaseDurationMillis * 2 -> TaskReadyPhase.TWO
    elapsedMillis < RunReadiness.taskReadyTransitionDurationMillis -> TaskReadyPhase.ONE
    else -> null
}

/** Use this function to identify a completed-task transition that has a pending next task to prepare.
 * Inputs: `previous` — the displayed run before completion; `next` — the persisted run after completion.
 * Dependencies: `RunReadiness.startsTaskReadyTransition`, the same rule that delays the persisted start.
 */
internal fun shouldHoldTaskReadyTransition(previous: RoutineRun?, next: RoutineRun?): Boolean =
    previous != null && next != null && RunReadiness.startsTaskReadyTransition(previous, next)
