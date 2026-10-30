package com.taskchain.features.running.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskchain.R
import com.taskchain.core.data.RoutineRunRepository
import com.taskchain.core.designsystem.Button
import com.taskchain.core.designsystem.TextButton
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineTemplate
import com.taskchain.core.model.RunStatus
import com.taskchain.features.running.domain.startIsBlocked
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** State for starting a routine or explaining why another active routine must be resumed. */
data class RunningEntryState(
    val activeRun: RoutineRun? = null,
    val pendingStart: RoutineTemplate? = null,
    val blockedRoutine: RoutineTemplate? = null,
)

/** Coordinates active-run observation and the start conflict decision at the Running boundary. */
class RunningEntryViewModel(activeRuns: RoutineRunRepository) : ViewModel() {
    private val pendingStart = MutableStateFlow<RoutineTemplate?>(null)
    private val blockedRoutine = MutableStateFlow<RoutineTemplate?>(null)
    private val activeRun = activeRuns.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val state: StateFlow<RunningEntryState> = combine(activeRun, pendingStart, blockedRoutine) { run, pending, blocked ->
        RunningEntryState(run?.takeIf { it.status == RunStatus.ACTIVE }, pending, blocked)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), RunningEntryState())

    /** Use this function when a browsing surface selects a routine to start. */
    fun requestStart(routine: RoutineTemplate) {
        val run = activeRun.value?.takeIf { it.status == RunStatus.ACTIVE }
        if (startIsBlocked(run, routine.id)) blockedRoutine.value = routine else pendingStart.value = routine
    }

    /** Use this function after root navigation has opened the requested runner. */
    fun startHandled() {
        pendingStart.value = null
    }

    /** Use this function when the active-run conflict dialog is dismissed. */
    fun dismissBlockedStart() {
        blockedRoutine.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** Use this function to render the active-run conflict and offer resume or continued browsing. */
@Composable
internal fun RunningEntryDialog(
    viewModel: RunningEntryViewModel,
    onResume: (RoutineId) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val blocked = state.blockedRoutine ?: return
    val run = state.activeRun ?: return
    AlertDialog(
        onDismissRequest = viewModel::dismissBlockedStart,
        title = { androidx.compose.material3.Text(stringResource(R.string.active_run_in_progress_title)) },
        text = { androidx.compose.material3.Text(stringResource(R.string.active_run_in_progress_message, run.routineTitle)) },
        confirmButton = {
            Button(onClick = {
                viewModel.dismissBlockedStart()
                onResume(run.routineId)
            }) {
                androidx.compose.material3.Text(stringResource(R.string.resume_routine, run.routineTitle))
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissBlockedStart) {
                androidx.compose.material3.Text(stringResource(R.string.keep_browsing))
            }
        },
    )
}
