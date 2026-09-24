package com.taskchain.data

import com.taskchain.domain.model.CompletionEvent
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/** Storage boundary for reusable routine definitions. */
interface RoutineRepository {
    /** Use this function when UI needs a live list of locally stored routines. */
    fun observeAll(): Flow<List<RoutineTemplate>>

    /** Use this function when opening one routine by stable identity. */
    suspend fun get(id: RoutineId): RoutineTemplate?

    /** Use this function when creating or updating a routine definition. */
    suspend fun save(routine: RoutineTemplate)

    /** Use this function when explicitly deleting a routine definition. */
    suspend fun delete(id: RoutineId)
}

/** Storage boundary for the one active run that must survive process death. */
interface RoutineRunRepository {
    /** Use this function when runner UI needs the persisted active session. */
    fun observeActive(): Flow<RoutineRun?>

    /** Use this function after every domain transition in an active run. */
    suspend fun saveActive(run: RoutineRun)

    /** Use this function after a terminal run has been safely added to history. */
    suspend fun clearActive()
}

/** Append-only storage boundary for progress history. */
interface CompletionRepository {
    /** Use this function when progress UI needs all locally recorded run events. */
    fun observeAll(): Flow<List<CompletionEvent>>

    /** Use this function exactly once when a run becomes completed or aborted. */
    suspend fun append(event: CompletionEvent)
}

/** Persistence boundary for user-level appearance and timer behavior. */
interface UserPreferenceRepository {
    /** Use this function when UI or runner behavior needs current preferences. */
    fun observe(): Flow<UserPreferences>

    /** Use this function when the selected app theme changes. */
    suspend fun setTheme(theme: String)

    /** Use this function when overtime behavior is toggled. */
    suspend fun setContinueTimerPastZero(enabled: Boolean)
}
