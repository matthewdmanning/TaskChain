package com.taskchain.features.execution.domain

import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RunStatus

/**
 * Use this function to decide whether a different active run blocks a selected routine.
 * Inputs: `activeRun` — current persisted run; `routineId` — selected routine identity.
 * Dependencies: `RoutineRun.status`, `RoutineRun.routineId`.
 */
internal fun startIsBlocked(activeRun: RoutineRun?, routineId: RoutineId): Boolean =
    activeRun?.status == RunStatus.ACTIVE && activeRun.routineId != routineId
