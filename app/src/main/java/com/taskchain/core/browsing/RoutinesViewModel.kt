package com.taskchain.core.browsing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskchain.core.data.RoutineRepository
import com.taskchain.core.model.RoutineTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** UI state for the editable and predefined routine collections. */
data class RoutinesState(
    val routines: List<RoutineTemplate> = emptyList(),
    val builtIns: List<RoutineTemplate> = emptyList(),
)

/** Loads routine definitions and built-ins for reusable collection browsing. */
class RoutinesViewModel(
    routines: RoutineRepository,
    private val loadBuiltIns: suspend (Long) -> List<RoutineTemplate>,
    private val now: () -> Long,
) : ViewModel() {
    private val builtIns = MutableStateFlow<List<RoutineTemplate>>(emptyList())

    val state: StateFlow<RoutinesState> = combine(
        routines.observeAll(),
        builtIns,
    ) { definitions, library ->
        RoutinesState(
            routines = definitions,
            builtIns = library.filterNot { builtIn -> definitions.any { it.id == builtIn.id } },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), RoutinesState())

    init {
        viewModelScope.launch { builtIns.value = loadBuiltIns(now()) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
