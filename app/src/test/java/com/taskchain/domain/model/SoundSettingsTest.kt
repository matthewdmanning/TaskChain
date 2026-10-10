package com.taskchain.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies sound defaults, legacy decoding, and persisted per-token policy. */
class SoundSettingsTest {
    /** Use this function to verify stored runs without feedback fields still decode with safe defaults. */
    @Test
    fun legacyRunGetsFeedbackDefaults() {
        val run = Json.decodeFromString<RoutineRun>(
            """{"id":"run","routineId":"routine","routineTitle":"Legacy","tasks":[{"source":{"id":"task","title":"Task"},"startedAtEpochMillis":1000}],"currentTaskIndex":0,"startedAtEpochMillis":1000}""",
        )

        assertTrue(run.routineSoundEnabled)
        assertTrue(run.routineVibrateEnabled)
        assertEquals(0L, run.tasks.single().taskNudgeCount)
        assertTrue(run.soundSettings[SoundToken.TimerExpired].enabled)
    }

    /** Use this function to verify routines without a sound field receive the current native-beep defaults. */
    @Test
    fun legacyRoutineGetsDefaultSoundSettings() {
        val routine = Json { ignoreUnknownKeys = true }.decodeFromString<RoutineTemplate>(
            """{"id":"legacy","metadata":{"createdAtEpochMillis":0,"updatedAtEpochMillis":0},"title":"Legacy","tasks":[{"id":"task","title":"Task"}]}""",
        )

        assertTrue(routine.soundSettings[SoundToken.TimerExpired].enabled)
        assertEquals(null, routine.soundSettings[SoundToken.TimerExpired].assetPath)
        assertFalse(routine.soundSettings[SoundToken.TaskRunning].enabled)
        assertEquals("sounds/task_running.ogg", routine.soundSettings[SoundToken.TaskRunning].assetPath)
    }

    /** Use this function to verify configured sound entries survive JSON persistence unchanged. */
    @Test
    fun soundSettingsRoundTripPreservesEntries() {
        val json = Json { }
        val settings = SoundSettings(
            entries = mapOf(
                SoundToken.TimerExpired to SoundSetting(enabled = false, assetPath = "custom/expired.ogg"),
                SoundToken.TaskPaused to SoundSetting(enabled = true, assetPath = "custom/paused.ogg"),
            ),
        )

        val restored = json.decodeFromString<SoundSettings>(json.encodeToString(settings))

        assertEquals(settings.entries, restored.entries)
        assertFalse(restored[SoundToken.TimerExpired].enabled)
        assertEquals("custom/paused.ogg", restored[SoundToken.TaskPaused].assetPath)
        assertFalse(restored[SoundToken.TaskCompleted].enabled)
    }
}
