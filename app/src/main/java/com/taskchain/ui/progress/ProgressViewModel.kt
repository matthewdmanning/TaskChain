package com.taskchain.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskchain.AppContainer
import com.taskchain.domain.progress.ProgressSummary
import com.taskchain.domain.progress.projectProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Projects local completion events into the small progress summary. */
private val EMPTY_PROGRESS = ProgressSummary(0, 0, 0, 0, 0, emptyList())
class ProgressViewModel(container: AppContainer) : ViewModel() {
    val state: StateFlow<ProgressSummary> = container.completions.observeAll()
        .map { events -> projectProgress(events, container.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), EMPTY_PROGRESS)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
