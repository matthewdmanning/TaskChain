package com.taskchain.domain.run

import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunTaskStatus

/** Holds the run after due alerts were acknowledged and which alerts the caller must now play. */
data class DueRunAlerts(
    val run: RoutineRun,
    val nudge: Boolean,
    val timerExpired: Boolean,
)

/**
 * Decides when the run needs its next audio and haptic alert while the Run screen is not visible,
 * and which alerts are due. Platform-neutral: the Android alarm adapter only maps these times to alarms.
 * Dependencies: [RoutineRunEngine], [RunFeedbackPolicy].
 */
object RunAlertPolicy {
    /**
     * Use this function to decide whether periodic reminders must stop for this run.
     * They stop when active time reaches the sum of all task times. A run with no timer stops them
     * `untimedIdleLimitMillis` after the latest navigation, pause, resume, skip, or complete.
     */
    fun remindersExhausted(
        run: RoutineRun,
        engine: RoutineRunEngine,
        nowEpochMillis: Long,
        untimedIdleLimitMillis: Long,
    ): Boolean {
        if (engine.plannedRunMillis(run) > 0L) return engine.isPastPlannedRunTime(run, nowEpochMillis)
        val idleSince = run.lastInteractionAtEpochMillis ?: run.startedAtEpochMillis
        return nowEpochMillis - idleSince >= untimedIdleLimitMillis
    }

    /**
     * Use this function to find the wall-clock time of the next alert, or null when none can occur
     * (not active, paused, in confirmation, or reminders exhausted and no timer expiry pending).
     * The result is never earlier than `nowEpochMillis`.
     */
    fun nextAlertAt(
        run: RoutineRun,
        engine: RoutineRunEngine,
        nowEpochMillis: Long,
        nudgeIntervalMillis: Long,
        untimedIdleLimitMillis: Long,
    ): Long? {
        require(nudgeIntervalMillis > 0L)
        if (run.status != RunStatus.ACTIVE || run.finishConfirmationRequested || run.abortConfirmationRequested) return null
        val task = run.tasks.getOrNull(run.currentTaskIndex) ?: return null
        if (task.status != RunTaskStatus.PENDING || task.pausedAtEpochMillis != null) return null
        val startedAt = task.startedAtEpochMillis ?: return null
        val candidates = mutableListOf<Long>()
        if (!remindersExhausted(run, engine, nowEpochMillis, untimedIdleLimitMillis)) {
            candidates += startedAt + (task.taskNudgeCount + 1) * nudgeIntervalMillis
        }
        if (task.timerFeedbackAtEpochMillis == null) {
            engine.remainingMillis(run, nowEpochMillis)?.let { candidates += nowEpochMillis + it }
        }
        return candidates.minOrNull()?.coerceAtLeast(nowEpochMillis)
    }

    /**
     * Use this function when the alert alarm fires, or a screen-off tick runs, to acknowledge what is due.
     * A due nudge bucket is always acknowledged, but only reported when reminders are not exhausted.
     */
    fun takeDue(
        run: RoutineRun,
        engine: RoutineRunEngine,
        nowEpochMillis: Long,
        nudgeIntervalMillis: Long,
        untimedIdleLimitMillis: Long,
    ): DueRunAlerts {
        if (run.status != RunStatus.ACTIVE || run.finishConfirmationRequested || run.abortConfirmationRequested) {
            return DueRunAlerts(run, nudge = false, timerExpired = false)
        }
        val task = run.tasks.getOrNull(run.currentTaskIndex)
        if (task == null || task.status != RunTaskStatus.PENDING || task.pausedAtEpochMillis != null) {
            return DueRunAlerts(run, nudge = false, timerExpired = false)
        }
        val exhausted = remindersExhausted(run, engine, nowEpochMillis, untimedIdleLimitMillis)
        val nudge = RunFeedbackPolicy.evaluateNudge(run, nowEpochMillis, nudgeIntervalMillis, emit = !exhausted)
        var updated = nudge.run
        val timerExpired = engine.needsTimerFeedback(updated, nowEpochMillis)
        if (timerExpired) updated = engine.acknowledgeTimerFeedback(updated, nowEpochMillis)
        return DueRunAlerts(updated, nudge = nudge.shouldFire, timerExpired = timerExpired)
    }
}
