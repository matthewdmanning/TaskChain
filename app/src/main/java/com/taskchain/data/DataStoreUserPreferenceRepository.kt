package com.taskchain.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

    private companion object {
        const val DEFAULT_THEME = "system"
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        val CONTINUE_PAST_ZERO = booleanPreferencesKey("continue_timer_past_zero")
    }
}
