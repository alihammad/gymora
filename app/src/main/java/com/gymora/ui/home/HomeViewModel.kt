package com.gymora.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            routineRepository.observeAll().collect { routines ->
                _uiState.update { it.copy(routines = routines, isLoading = false) }
            }
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
}
