package com.taskchain.features.progress.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskchain.core.data.CompletionRepository
import com.taskchain.features.progress.domain.ProgressSummary
import com.taskchain.features.progress.domain.projectProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Projects local completion events into the small progress summary. */
private val EMPTY_PROGRESS = ProgressSummary(0, 0, 0, 0, 0, emptyList())
class ProgressViewModel(completions: CompletionRepository, private val now: () -> Long) : ViewModel() {
    val state: StateFlow<ProgressSummary> = completions.observeAll()
        .map { events -> projectProgress(events, now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), EMPTY_PROGRESS)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
