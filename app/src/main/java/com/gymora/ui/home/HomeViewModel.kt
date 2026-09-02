package com.gymora.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.repository.HistoryRepository
import com.gymora.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            routineRepository.observeAll().collect { routines ->
                _uiState.update { it.copy(routines = routines, isLoading = false) }
            }
        }
        loadRecentWorkouts()
    }

    /** The 3 most recent completed workouts (spec Assumption, FR-002, T050a). */
    fun loadRecentWorkouts() {
        viewModelScope.launch {
            val recent = historyRepository.listCompleted(limit = RECENT_COUNT, offset = 0)
            _uiState.update { it.copy(recentWorkouts = recent) }
        }
    }

    /** Persist the new home-screen order (FR-015). */
    fun onReorder(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value.routines.toMutableList()
        val moved = current.removeAt(fromIndex)
        current.add(toIndex, moved)
        _uiState.update { it.copy(routines = current) }
        viewModelScope.launch {
            routineRepository.reorder(current.map { it.id })
        }
    }

    companion object {
        const val RECENT_COUNT = 3
    }
}
