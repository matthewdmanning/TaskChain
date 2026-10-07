package com.taskchain.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.taskchain.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

/** DataStore-backed user preferences kept separate from device permission state. */
class DataStoreUserPreferenceRepository(private val context: Context) : UserPreferenceRepository {
    /** Use this function when app theme or timer behavior must update reactively. */
    override fun observe(): Flow<UserPreferences> = context.userPreferencesDataStore.data.map { values ->
        UserPreferences(
            selectedTheme = values[SELECTED_THEME] ?: DEFAULT_THEME,
            continueTimerPastZero = values[CONTINUE_PAST_ZERO] ?: true,
            vibrationIntensity = values[VIBRATION_INTENSITY] ?: 1f,
            screenTransitionsEnabled = values[SCREEN_TRANSITIONS_ENABLED] ?: true,
            bubbleOnMinimize = values[BUBBLE_ON_MINIMIZE] ?: false,
        )
    }

    /** Use this function when settings chooses a theme supported by the design system. */
    override suspend fun setTheme(theme: String) {
        context.userPreferencesDataStore.edit { it[SELECTED_THEME] = theme }
    }

    /** Use this function when settings changes whether countdowns show overtime. */
    override suspend fun setContinueTimerPastZero(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[CONTINUE_PAST_ZERO] = enabled }
    }

    /** Use this function when settings changes the strength of haptic feedback. */
    override suspend fun setVibrationIntensity(intensity: Float) {
        require(intensity in 0f..1f)
        context.userPreferencesDataStore.edit { it[VIBRATION_INTENSITY] = intensity }
    }

    /** Use this function when settings changes completion transition presentation. */
    override suspend fun setScreenTransitionsEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[SCREEN_TRANSITIONS_ENABLED] = enabled }
    }

    /** Use this function when settings changes whether the app requests a minimize bubble. */
    override suspend fun setBubbleOnMinimize(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[BUBBLE_ON_MINIMIZE] = enabled }
    }

    private companion object {
        const val DEFAULT_THEME = "system"
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        val CONTINUE_PAST_ZERO = booleanPreferencesKey("continue_timer_past_zero")
        val VIBRATION_INTENSITY = floatPreferencesKey("vibration_intensity")
        val SCREEN_TRANSITIONS_ENABLED = booleanPreferencesKey("screen_transitions_enabled")
        val BUBBLE_ON_MINIMIZE = booleanPreferencesKey("bubble_on_minimize")
    }
}
