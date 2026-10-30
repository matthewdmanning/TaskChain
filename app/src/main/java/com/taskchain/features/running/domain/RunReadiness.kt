package com.taskchain.features.running.domain

import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RunStatus
import com.taskchain.core.model.RunTaskStatus

/**
 * Owns the completion-effect and readiness-countdown timing that delays a newly started task's persisted start,
 * and the rule for when that delay applies. Presentation reads the same values so the countdown and the persisted
 * start stay aligned. Constructor inputs: none. Dependencies: `RoutineRun` task statuses and indexes.
 */
object RunReadiness {
    /** Completion effect length before the readiness countdown begins. */
    const val completionDurationMillis = 350L
    /** "Get Ready" lead-in before the numbered countdown. */
    const val taskReadyGetReadyDurationMillis = 2_000L
    /** Length of each numbered countdown step (3, 2, 1). */
    const val taskReadyPhaseDurationMillis = 1_000L
    /** Whole readiness countdown: the lead-in plus three numbered steps. */
    const val taskReadyTransitionDurationMillis =
        taskReadyGetReadyDurationMillis + taskReadyPhaseDurationMillis * 3
    /** Persisted start delay of the next task when the completion effect and countdown are presented. */
    const val taskStartDelayMillis = completionDurationMillis + taskReadyTransitionDurationMillis

    /**
     * Use this function when Complete is pressed to choose the next task's start delay.
     * Inputs: `presented` — whether task transitions are enabled and the Run screen is in the foreground.
     */
    fun startDelayMillis(presented: Boolean): Long = if (presented) taskStartDelayMillis else 0L

    /**
     * Use this function to tell whether a transition completed the main task that was current before it.
     * Inputs: `previous` — run before the transition; `next` — run after it. Different runs never match.
     */
    fun completedCurrentTask(previous: RoutineRun, next: RoutineRun): Boolean {
        if (previous.id != next.id || previous.routineId != next.routineId) return false
        val previousTask = previous.tasks.getOrNull(previous.currentTaskIndex) ?: return false
        val nextTask = next.tasks.getOrNull(previous.currentTaskIndex) ?: return false
        return previousTask.status != RunTaskStatus.COMPLETED && nextTask.status == RunTaskStatus.COMPLETED
    }

    /**
     * Use this function to tell whether a completion advanced the run to a later pending task that should wait for
     * the completion effect and readiness countdown.
     * Inputs: `previous` — run before the transition; `next` — run after it.
     */
    fun startsTaskReadyTransition(previous: RoutineRun, next: RoutineRun): Boolean {
        if (!completedCurrentTask(previous, next)) return false
        if (next.status != RunStatus.ACTIVE || next.currentTaskIndex <= previous.currentTaskIndex) return false
        return next.tasks.getOrNull(next.currentTaskIndex)?.status == RunTaskStatus.PENDING
    }
}
