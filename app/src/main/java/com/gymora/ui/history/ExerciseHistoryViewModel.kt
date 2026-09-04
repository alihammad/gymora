package com.gymora.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.model.ExercisePerformance
import com.gymora.domain.repository.HistoryRepository
import com.gymora.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for the exercise history screen (FR-044). */
data class ExerciseHistoryUiState(
    val exerciseName: String = "",
    val performances: List<ExercisePerformance> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
)

@HiltViewModel
class ExerciseHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val exerciseId: Long = savedStateHandle
        .get<String>(Destinations.ExerciseHistory.ARG)?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(ExerciseHistoryUiState())
    val uiState: StateFlow<ExerciseHistoryUiState> = _uiState.asStateFlow()

    init {
        loadInitial()
    }

    private fun loadInitial() {
        viewModelScope.launch {
            val performances = historyRepository.getExerciseHistory(
                exerciseId, limit = PAGE_SIZE, offset = 0,
            )
            _uiState.update {
                it.copy(
                    performances = performances,
                    isLoading = false,
                    hasMore = performances.size == PAGE_SIZE,
                )
            }
        }
    }

    /** Incremental loading for large exercise histories (FR-044, R-07). */
    fun onLoadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            val more = historyRepository.getExerciseHistory(
                exerciseId, limit = PAGE_SIZE, offset = state.performances.size,
            )
            _uiState.update {
                it.copy(
                    performances = it.performances + more,
                    isLoadingMore = false,
                    hasMore = more.size == PAGE_SIZE,
                )
            }
        }
    }

    companion object {
        const val PAGE_SIZE = 30
    }
}
