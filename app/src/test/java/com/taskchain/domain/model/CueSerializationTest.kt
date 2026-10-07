package com.taskchain.domain.model

import com.taskchain.domain.run.RoutineRunEngine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies cue persistence, legacy defaults, and derived duration behavior. */
class CueSerializationTest {
    /** Use this function to verify template, active-run, and history JSON retain cue state. */
    @Test
    fun cueStateRoundTripsAcrossTemplateRunAndHistory() {
        val json = Json { }
        val template = RoutineTemplate(
            id = RoutineId("routine"),
            metadata = EntityMetadata(0, 0),
            title = "Routine",
            steps = listOf(
                RoutineStep(
                    RoutineStepId("step"),
                    "Step",
                    cues = listOf(
                        RoutineCue(RoutineCueId("one"), "One", 60),
                        RoutineCue(RoutineCueId("two"), "Two", 120),
                    ),
                ),
            ),
        )
        val run = RoutineRunEngine().completeCurrent(
            RoutineRunEngine().start(template, RoutineRunId("run"), 0),
            30_000L,
        )
        val event = RoutineRunEngine().toCompletionEvent(
            run.copy(
                steps = run.steps.map {
                    it.copy(status = RunStepStatus.COMPLETED, actualDurationMillis = 30_000L)
                },
                status = RunStatus.COMPLETED,
                endedAtEpochMillis = 30_000L,
            ),
        )

        assertEquals(template, json.decodeFromString<RoutineTemplate>(json.encodeToString(template)))
        assertEquals(run, json.decodeFromString<RoutineRun>(json.encodeToString(run)))
        assertEquals(event, json.decodeFromString<CompletionEvent>(json.encodeToString(event)))
        assertEquals(180L, template.steps.single().durationSeconds)
        assertTrue(defaultSoundSettings()[SoundToken.CueAdvanced].enabled)
    }

    /** Use this function to verify old no-cue records decode with empty cue state and current preferences. */
    @Test
    fun legacyRecordsUseNoCueDefaults() {
        val json = Json { ignoreUnknownKeys = true }
        val run = json.decodeFromString<RoutineRun>(
            """{"id":"run","routineId":"routine","routineTitle":"Legacy","steps":[{"source":{"id":"step","title":"Task"},"startedAtEpochMillis":1000}],"currentStepIndex":0,"startedAtEpochMillis":1000}""",
        )
        val preferences = json.decodeFromString<UserPreferences>("""{"selectedTheme":"system"}""")

        assertTrue(run.steps.single().source.cues.isEmpty())
        assertEquals(null, run.steps.single().activeCueId)
        assertTrue(run.steps.single().cueAdvancements.isEmpty())
        assertFalse(preferences.showCueTimeRemaining)
    }

    /** Use this function to verify cue validation rejects malformed identities, titles, durations, and totals. */
    @Test
    fun cueValidationRequiresPositiveUniqueSafeDefinitions() {
        val base = RoutineTemplate(
            id = RoutineId("routine"),
            metadata = EntityMetadata(0, 0),
            title = "Routine",
            steps = listOf(RoutineStep(RoutineStepId("step"), "Step")),
        )

        listOf(
            listOf(RoutineCue(RoutineCueId(""), "Cue", 1)),
            listOf(RoutineCue(RoutineCueId("cue"), "", 1)),
            listOf(RoutineCue(RoutineCueId("cue"), "Cue", 0)),
            listOf(
                RoutineCue(RoutineCueId("cue"), "Cue", 1),
                RoutineCue(RoutineCueId("cue"), "Again", 1),
            ),
            listOf(
                RoutineCue(RoutineCueId("cue"), "Cue", Long.MAX_VALUE / 1_000L),
                RoutineCue(RoutineCueId("other"), "Other", 1),
            ),
        ).forEach { cues ->
            val invalid = base.copy(steps = listOf(base.steps.single().copy(cues = cues)))
            runCatching { invalid.requireRunnable() }.onSuccess { error("Expected invalid cue definition") }
        }
    }
}
