package com.gymora.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.model.RoutineSummary
import com.gymora.domain.repository.RoutineRepository
import com.gymora.domain.usecase.DeleteRoutineUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for the My Routines list destination (FR-002). */
data class RoutineListUiState(
    val routines: List<RoutineSummary> = emptyList(),
    val isLoading: Boolean = true,
    val pendingDelete: RoutineSummary? = null,
)

@HiltViewModel
class RoutineListViewModel @Inject constructor(
    routineRepository: RoutineRepository,
    private val deleteRoutineUseCase: DeleteRoutineUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineListUiState())
    val uiState: StateFlow<RoutineListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            routineRepository.observeAll().collect { routines ->
                _uiState.update { it.copy(routines = routines, isLoading = false) }
            }
        }
    }

    fun onDeleteRequested(routine: RoutineSummary) {
        _uiState.update { it.copy(pendingDelete = routine) }
    }

    fun onDeleteDismissed() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    fun onDeleteConfirmed() {
        val routine = _uiState.value.pendingDelete ?: return
        _uiState.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            // History is protected structurally (BR-03); the list refreshes via the Flow.
            runCatching { deleteRoutineUseCase(routine.id) }
        }
    }
}
