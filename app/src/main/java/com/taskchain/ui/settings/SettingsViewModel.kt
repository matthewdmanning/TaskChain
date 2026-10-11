package com.taskchain.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskchain.AppContainer
import com.taskchain.domain.model.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Coordinates Settings UI with persisted user preferences. */
class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<UserPreferences> = container.preferences.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UserPreferences())

    /** Use this function when a supported theme is selected in Settings. */
    fun setTheme(theme: String) {
        viewModelScope.launch { container.preferences.setTheme(theme) }
    }

    /** Use this function when overtime behavior is toggled in Settings. */
    fun setContinuePastZero(enabled: Boolean) {
        viewModelScope.launch { container.preferences.setContinueTimerPastZero(enabled) }
    }

    /** Use this function to set press vibration strength; input is 0..1 intensity, dependency is the preference repository. */
    fun setVibrationIntensity(intensity: Float) {
        viewModelScope.launch { container.preferences.setVibrationIntensity(intensity) }
    }

    /** Use this function to toggle task entry effects; input is the enabled gate, dependency is the preference repository. */
    fun setScreenTransitionsEnabled(enabled: Boolean) {
        viewModelScope.launch { container.preferences.setScreenTransitionsEnabled(enabled) }
    }

    /** Use this function to opt into native run bubbles; input is the enabled gate, dependency is the preference repository. */
    fun setBubbleOnMinimize(enabled: Boolean) {
        viewModelScope.launch { container.preferences.setBubbleOnMinimize(enabled) }
    }

    /** Use this function when subtask countdown visibility is toggled in Settings. */
    fun setShowSubtaskTimeRemaining(enabled: Boolean) {
        viewModelScope.launch { container.preferences.setShowSubtaskTimeRemaining(enabled) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
