package com.taskchain.core.startup

import com.taskchain.core.data.CompletionRepository
import com.taskchain.core.data.RoutineRepository
import com.taskchain.core.data.UserPreferenceRepository
import kotlinx.coroutines.flow.first

/** Use this function before editable screens to migrate and validate local data, then run terminal recovery.
 * Inputs: migration action, routine/history/preferences repositories, and recovery action.
 * Dependencies: repository Flow validation and feature-owned recovery.
 */
suspend fun initializeUserData(
    migrate: suspend () -> Unit,
    routines: RoutineRepository,
    completions: CompletionRepository,
    preferences: UserPreferenceRepository,
    recover: suspend () -> Unit,
) {
    migrate()
    routines.observeAll().first()
    completions.observeAll().first()
    preferences.observe().first()
    recover()
}
