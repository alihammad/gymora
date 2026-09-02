package com.gymora.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.model.WorkoutSummary
import com.gymora.domain.repository.WorkoutSessionRepository
import com.gymora.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for the workout summary screen (FR-035). */
data class WorkoutSummaryUiState(
    val summary: WorkoutSummary? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class WorkoutSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutSessionRepository: WorkoutSessionRepository,
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<String>(Destinations.WorkoutSummary.ARG)
        ?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(WorkoutSummaryUiState())
    val uiState: StateFlow<WorkoutSummaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { workoutSessionRepository.getSummary(sessionId) }
                .onSuccess { summary ->
                    _uiState.update { it.copy(summary = summary, isLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }
}
