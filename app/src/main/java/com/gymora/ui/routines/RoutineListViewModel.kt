package com.gymora.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.model.RoutineSummary
import com.gymora.domain.repository.RoutineRepository
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
)

@HiltViewModel
class RoutineListViewModel @Inject constructor(
    routineRepository: RoutineRepository,
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
}
