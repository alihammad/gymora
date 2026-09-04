package com.gymora.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.model.Exercise
import com.gymora.domain.repository.ExerciseRepository
import com.gymora.domain.usecase.DeleteExerciseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val deleteExerciseUseCase: DeleteExerciseUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseLibraryUiState())
    val uiState: StateFlow<ExerciseLibraryUiState> = _uiState.asStateFlow()

    init {
        observeLibrary()
    }

    private fun observeLibrary() {
        viewModelScope.launch {
            exerciseRepository.observeLibrary().collect { exercises ->
                _uiState.update { state ->
                    state.copy(
                        groupedExercises = exercises.groupBy { it.muscleGroup },
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            val results = if (query.isBlank()) {
                null
            } else {
                exerciseRepository.search(query)
            }
            _uiState.update { it.copy(searchResults = results) }
        }
    }

    fun onDeleteRequested(exercise: Exercise) {
        _uiState.update { it.copy(pendingDelete = exercise) }
    }

    fun onDeleteConfirmed() {
        val exercise = _uiState.value.pendingDelete ?: return
        viewModelScope.launch {
            runCatching { deleteExerciseUseCase(exercise.id) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = friendlyMessage(error),
                            pendingDelete = null,
                        )
                    }
                }
            _uiState.update { it.copy(pendingDelete = null) }
        }
    }

    /** FR-060: no stack traces surface; map domain failures to friendly copy. */
    private fun friendlyMessage(error: Throwable): String = when (error) {
        is com.gymora.domain.model.EntityNotFoundException ->
            "This exercise no longer exists."
        else -> "Something went wrong. Please try again."
    }

    fun onDeleteDismissed() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
