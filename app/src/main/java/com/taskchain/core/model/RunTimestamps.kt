package com.taskchain.core.model

/**
 * Use this function to read when a run task was completed; null unless its status is completed.
 * Prefers the completion timestamp and falls back to the finish timestamp recorded by older runs.
 */
fun RoutineRunTask.completedAtOrNull(): Long? =
    if (status == RunTaskStatus.COMPLETED) completedAtEpochMillis ?: finishedAtEpochMillis else null

/**
 * Use this function to read when a Routine run completed; null unless its status is completed.
 * Prefers the run end timestamp and falls back to the latest completed task when no end was recorded.
 */
fun RoutineRun.completedAtOrNull(): Long? {
    if (status != RunStatus.COMPLETED) return null
    return endedAtEpochMillis ?: tasks.mapNotNull { it.completedAtOrNull() }.maxOrNull()
}
