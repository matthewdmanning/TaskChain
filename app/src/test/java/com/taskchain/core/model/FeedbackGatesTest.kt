package com.taskchain.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the routine-over-task Sound and Vibrate gates shared by foreground and background run feedback. */
class FeedbackGatesTest {
    /** Use this function to build a one-task run with the given routine gates and nudge sound enablement. */
    private fun gatedRun(
        routineSound: Boolean = true,
        routineVibrate: Boolean = true,
        nudgeEnabled: Boolean = true,
        task: RoutineTask = gatedTask(),
    ) = RoutineRun(
        id = RoutineRunId("run"),
        routineId = RoutineId("routine"),
        routineTitle = "Routine",
        tasks = listOf(RoutineRunTask(source = task, startedAtEpochMillis = 0)),
        currentTaskIndex = 0,
        startedAtEpochMillis = 0,
        routineSoundEnabled = routineSound,
        soundSettings = SoundSettings(mapOf(SoundToken.TaskNudge to SoundSetting(enabled = nudgeEnabled))),
        routineVibrateEnabled = routineVibrate,
    )

    /** Use this function to build a task with the given gates and optional own sound. */
    private fun gatedTask(sound: Boolean = true, vibrate: Boolean = true, soundUri: String? = null) =
        RoutineTask(RoutineTaskId("task"), "Task", soundEnabled = sound, vibrateEnabled = vibrate, soundUri = soundUri)

    /** Use this function to verify routine mute takes precedence over an enabled task and token. */
    @Test
    fun routineMuteSilencesEnabledTask() {
        val task = gatedTask()
        assertFalse(FeedbackGates.soundEnabled(gatedRun(routineSound = false, task = task), task, SoundToken.TaskNudge))
        assertTrue(FeedbackGates.soundEnabled(gatedRun(task = task), task, SoundToken.TaskNudge))
    }

    /** Use this function to verify a task mute and a disabled token each silence the sound. */
    @Test
    fun taskMuteAndDisabledTokenSilence() {
        val muted = gatedTask(sound = false)
        assertFalse(FeedbackGates.soundEnabled(gatedRun(task = muted), muted, SoundToken.TaskNudge))
        val task = gatedTask()
        assertFalse(FeedbackGates.soundEnabled(gatedRun(nudgeEnabled = false, task = task), task, SoundToken.TaskNudge))
        // Tokens the run did not customize resolve to their defaults: timer expiry is enabled by default.
        assertTrue(FeedbackGates.soundEnabled(gatedRun(nudgeEnabled = false, task = task), task, SoundToken.TimerExpired))
    }

    /** Use this function to verify a task's own sound keeps the token's enabled flag, so both paths agree. */
    @Test
    fun taskSoundDoesNotChangeGate() {
        val task = gatedTask(soundUri = "content://sound")
        listOf(true, false).forEach { enabled ->
            val plain = gatedRun(nudgeEnabled = enabled, task = task)
            val withTaskSound = plain.copy(soundSettings = plain.soundSettings.withTaskSound(task.soundUri))
            assertEquals(
                FeedbackGates.soundEnabled(plain, task, SoundToken.TaskNudge),
                FeedbackGates.soundEnabled(withTaskSound, task, SoundToken.TaskNudge),
            )
        }
    }

    /** Use this function to verify Vibrate needs both the routine and the task gate. */
    @Test
    fun vibrateNeedsRoutineAndTask() {
        val task = gatedTask()
        val noVibrate = gatedTask(vibrate = false)
        assertTrue(FeedbackGates.vibrateEnabled(gatedRun(task = task), task))
        assertFalse(FeedbackGates.vibrateEnabled(gatedRun(routineVibrate = false, task = task), task))
        assertFalse(FeedbackGates.vibrateEnabled(gatedRun(task = noVibrate), noVibrate))
        // Sound mute never affects haptics.
        assertTrue(FeedbackGates.vibrateEnabled(gatedRun(routineSound = false, task = task), task))
    }
}
