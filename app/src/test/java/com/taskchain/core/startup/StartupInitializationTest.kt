package com.taskchain.core.startup

import com.taskchain.core.data.CompletionRepository
import com.taskchain.core.data.RoutineRepository
import com.taskchain.core.data.UserPreferenceRepository
import com.taskchain.core.model.CompletionEvent
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineTemplate
import com.taskchain.core.model.SoundToken
import com.taskchain.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies startup validation completes before feature-owned terminal recovery. */
class StartupInitializationTest {
    /** Use this function to verify migration, repository validation, and recovery retain their required order. */
    @Test
    fun validatesAllSharedRepositoriesBeforeRecovery() = runBlocking {
        val events = mutableListOf<String>()

        initializeUserData(
            migrate = { events += "migration" },
            routines = RecordingRoutines(events),
            completions = RecordingCompletions(events),
            preferences = RecordingPreferences(events),
            recover = { events += "recovery" },
        )

        assertEquals(
            listOf("migration", "routines", "completions", "preferences", "recovery"),
            events,
        )
    }

    /** Fake routine storage that records the validation read. */
    private class RecordingRoutines(private val events: MutableList<String>) : RoutineRepository {
        /** Use this function to record routine validation and emit an empty routine list. */
        override fun observeAll(): Flow<List<RoutineTemplate>> = flow {
            events += "routines"
            emit(emptyList())
        }

        /** Use this function to satisfy routine lookup without storage; input is a routine identity. */
        override suspend fun get(id: RoutineId): RoutineTemplate? = null

        /** Use this function to satisfy routine save without storage; input is a routine snapshot. */
        override suspend fun save(routine: RoutineTemplate) = Unit

        /** Use this function to satisfy routine deletion without storage; input is a routine identity. */
        override suspend fun delete(id: RoutineId) = Unit
    }

    /** Fake completion storage that records the validation read. */
    private class RecordingCompletions(private val events: MutableList<String>) : CompletionRepository {
        /** Use this function to record completion validation and emit empty history. */
        override fun observeAll(): Flow<List<CompletionEvent>> = flow {
            events += "completions"
            emit(emptyList())
        }

        /** Use this function to satisfy history append without storage; input is a completion event. */
        override suspend fun append(event: CompletionEvent) = Unit
    }

    /** Fake preference storage that records the validation read. */
    private class RecordingPreferences(private val events: MutableList<String>) : UserPreferenceRepository {
        /** Use this function to record preference validation and emit default preferences. */
        override fun observe(): Flow<UserPreferences> = flow {
            events += "preferences"
            emit(UserPreferences())
        }

        /** Use this function to satisfy theme writes without storage; input is a theme identifier. */
        override suspend fun setTheme(theme: String) = Unit

        /** Use this function to satisfy UI-provider writes without storage; input is a provider identifier. */
        override suspend fun setUiProvider(providerId: String) = Unit

        /** Use this function to satisfy overtime writes without storage; input is the enabled state. */
        override suspend fun setContinueTimerPastZero(enabled: Boolean) = Unit

        /** Use this function to satisfy vibration writes without storage; input is normalized intensity. */
        override suspend fun setVibrationIntensity(intensity: Float) = Unit

        /** Use this function to satisfy transition writes without storage; input is the enabled state. */
        override suspend fun setScreenTransitionsEnabled(enabled: Boolean) = Unit

        /** Use this function to satisfy subtask-time writes without storage; input is the enabled state. */
        override suspend fun setShowSubtaskTimeRemaining(enabled: Boolean) = Unit
        override suspend fun setKeepScreenOnDuringRun(enabled: Boolean) = Unit

        /** Use this function to satisfy default-sound writes without storage; inputs are a token and nullable URI. */
        override suspend fun setDefaultSoundUri(token: SoundToken, uri: String?) = Unit
    }
}
