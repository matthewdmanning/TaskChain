package com.taskchain.reminder

import com.taskchain.domain.model.SoundSetting
import com.taskchain.domain.model.SoundSettings
import com.taskchain.domain.model.SoundToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies independent sound and haptic gates for semantic run feedback. */
class TaskFeedbackTest {
    /**
     * Use this function to verify an audio failure does not suppress the enabled default nudge haptic.
     * Inputs: none. Dependencies: `AndroidTaskFeedback`, fake `SoundPlayer`.
     */
    @Test
    fun missingSoundDoesNotSuppressNudgeHaptic() {
        val hapticDurations = mutableListOf<Long>()
        val feedback = AndroidTaskFeedback(
            soundPlayer = object : SoundPlayer {
                /**
                 * Use this function to model a missing or unloadable configured sound asset.
                 * Inputs: `token` — semantic event; `settings` — resolved token setting.
                 * Dependencies: None.
                 */
                override fun play(token: SoundToken, settings: SoundSetting) {
                    throw IllegalStateException("missing asset")
                }
            },
            vibrate = hapticDurations::add,
        )

        feedback.fire(
            token = SoundToken.TaskNudge,
            soundEnabled = true,
            vibrateEnabled = true,
            soundSettings = SoundSettings(
                entries = mapOf(SoundToken.TaskNudge to SoundSetting(enabled = true, assetPath = "missing.ogg")),
            ),
        )

        assertEquals(listOf(300L), hapticDurations)
    }

    /**
     * Use this function to verify a routine mute suppresses audio while an enabled nudge haptic still fires.
     * Inputs: none. Dependencies: `AndroidTaskFeedback`, fake `SoundPlayer`.
     */
    @Test
    fun routineSoundMutePreservesNudgeHaptic() {
        var played = false
        val hapticDurations = mutableListOf<Long>()
        val feedback = AndroidTaskFeedback(
            soundPlayer = object : SoundPlayer {
                /**
                 * Use this function to record whether a muted sound crossed the playback boundary.
                 * Inputs: `token` — semantic event; `settings` — resolved token setting.
                 * Dependencies: `played`.
                 */
                override fun play(token: SoundToken, settings: SoundSetting) {
                    played = true
                }
            },
            vibrate = hapticDurations::add,
        )

        feedback.fire(
            SoundToken.TaskNudge, false, true,
            SoundSettings(entries = mapOf(SoundToken.TaskNudge to SoundSetting(enabled = true, assetPath = "nudge.ogg"))),
        )

        assertTrue(!played)
        assertEquals(listOf(300L), hapticDurations)
    }
}
