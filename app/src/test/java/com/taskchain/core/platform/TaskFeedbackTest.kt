package com.taskchain.core.platform

import com.taskchain.core.model.SoundSetting
import com.taskchain.core.model.SoundSettings
import com.taskchain.core.model.SoundToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        val feedback: TaskFeedback = AndroidTaskFeedback(
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
        val feedback: TaskFeedback = AndroidTaskFeedback(
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

    /** Use this function to verify zero suppresses haptics and fractional intensity maps to Android amplitude.
     * Inputs: none.
     * Dependencies: `AndroidTaskFeedback`, `vibrationAmplitude`, and fake haptic callbacks.
     */
    @Test
    fun vibrationIntensityControlsAmplitudeAndZeroSuppresses() {
        val amplitudes = mutableListOf<Int>()
        val feedback: TaskFeedback = AndroidTaskFeedback(
            soundPlayer = object : SoundPlayer {
                override fun play(token: SoundToken, settings: SoundSetting) = Unit
            },
            vibrate = { error("legacy haptic callback should not be used when amplitude callback is present") },
            vibrateWithAmplitude = { _, amplitude -> amplitudes += amplitude },
        )
        val settings = SoundSettings(entries = mapOf(SoundToken.TaskNudge to SoundSetting(enabled = false, assetPath = "")))

        feedback.fire(SoundToken.TaskNudge, true, true, settings, intensity = 0f)
        feedback.fire(SoundToken.TaskNudge, true, true, settings, intensity = 0.5f)

        assertEquals(listOf(128), amplitudes)
        assertNull(vibrationAmplitude(0f))
        assertEquals(255, vibrationAmplitude(2f))
    }

    /** Use this function to verify the countdown pulse is short and obeys the haptic gate and intensity.
     * Inputs: none.
     * Dependencies: `AndroidTaskFeedback` and fake haptic callbacks.
     */
    @Test
    fun countdownPulseIsShortAndRespectsGatesAndIntensity() {
        val pulses = mutableListOf<Pair<Long, Int>>()
        val feedback: TaskFeedback = AndroidTaskFeedback(
            soundPlayer = object : SoundPlayer {
                override fun play(token: SoundToken, settings: SoundSetting) = Unit
            },
            vibrate = { error("legacy haptic callback should not be used when amplitude callback is present") },
            vibrateWithAmplitude = { duration, amplitude -> pulses += duration to amplitude },
        )

        feedback.pulse(vibrateEnabled = false, intensity = 1f)
        feedback.pulse(vibrateEnabled = true, intensity = 0f)
        feedback.pulse(vibrateEnabled = true, intensity = 0.5f)

        assertEquals(listOf(40L to 128), pulses)
    }
}
