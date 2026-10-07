package com.taskchain.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies defaults and validation for persisted user preferences. */
class UserPreferencesTest {
    /**
     * Use this function to verify new preferences retain their safe defaults.
     * Inputs: none.
     * Dependencies: `UserPreferences` and JUnit assertions.
     */
    @Test
    fun defaultsKeepOptionalFeaturesSafe() {
        val preferences = UserPreferences()

        assertEquals(1f, preferences.vibrationIntensity)
        assertTrue(preferences.screenTransitionsEnabled)
        assertTrue(!preferences.bubbleOnMinimize)
    }

    /**
     * Use this function to verify vibration intensity rejects values outside its persisted range.
     * Inputs: none; the test constructs invalid preference values.
     * Dependencies: `UserPreferences` and JUnit assertions.
     */
    @Test
    fun vibrationIntensityMustBeBetweenZeroAndOne() {
        val below = runCatching { UserPreferences(vibrationIntensity = -0.01f) }
        val above = runCatching { UserPreferences(vibrationIntensity = 1.01f) }

        assertTrue(below.isFailure)
        assertTrue(above.isFailure)
    }
}
