package com.taskchain.ui.designsystem

import com.taskchain.domain.model.UserPreferences
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies persisted component-provider compatibility independently of Android UI rendering. */
class UiProviderTest {
    /** Legacy preferences must preserve the existing Cyberpunk component implementation. */
    @Test
    fun legacyPreferencesDefaultToCyberpunk() {
        val preferences = Json.decodeFromString<UserPreferences>("""{"selectedTheme":"system"}""")

        assertEquals(UiProvider.CYBERPUNK, UiProvider.fromId(preferences.selectedUiProvider))
    }

    /** A saved Material selection survives JSON serialization and decoding. */
    @Test
    fun materialProviderRoundTrips() {
        val original = UserPreferences(selectedUiProvider = UiProvider.MATERIAL.id)
        val restored = Json.decodeFromString<UserPreferences>(Json.encodeToString(original))

        assertEquals(UiProvider.MATERIAL, UiProvider.fromId(restored.selectedUiProvider))
    }

    /** Unknown or stale provider IDs fall back without throwing during app startup. */
    @Test
    fun unknownProviderFallsBackToCyberpunk() {
        assertEquals(UiProvider.CYBERPUNK, UiProvider.fromId("future-provider"))
    }
}
