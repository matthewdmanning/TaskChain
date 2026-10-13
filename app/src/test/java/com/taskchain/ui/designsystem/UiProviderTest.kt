package com.taskchain.ui.designsystem

import com.taskchain.domain.model.UserPreferences
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
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
    /** Both persisted IDs resolve to distinct component implementations. */
    @Test
    fun eachProviderResolvesToItsOwnComponents() {
        assertSame(CyberpunkUiComponents, componentsFor(UiProvider.CYBERPUNK))
        assertSame(MaterialUiComponents, componentsFor(UiProvider.MATERIAL))
        assertNotSame(componentsFor(UiProvider.CYBERPUNK), componentsFor(UiProvider.MATERIAL))
    }

    /** Selecting a provider does not modify the independent theme preference. */
    @Test
    fun providerSelectionDoesNotChangeThemePreference() {
        val preferences = UserPreferences(selectedTheme = "system", selectedUiProvider = UiProvider.MATERIAL.id)
        val restored = Json.decodeFromString<UserPreferences>(Json.encodeToString(preferences))

        assertEquals("system", restored.selectedTheme)
        assertEquals(UiProvider.MATERIAL.id, restored.selectedUiProvider)
    }

    /** The provider identifier mapping is stable for stored user preferences. */
    @Test
    fun knownProviderIdsResolveWithoutFallback() {
        UiProvider.entries.forEach { provider ->
            assertEquals(provider, UiProvider.fromId(provider.id))
        }
    }
}
