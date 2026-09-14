package com.gymora.ui.exercisedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.model.Exercise
import com.gymora.domain.repository.ExerciseRepository
import com.gymora.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for the exercise detail screen. */
data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val exerciseId: Long? = savedStateHandle.get<String>(Destinations.ExerciseDetail.ARG)
        ?.toLongOrNull()
        ?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    init {
        exerciseId?.let(::loadExercise)
    }

    private fun loadExercise(id: Long) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { exerciseRepository.getById(id) }
                .onSuccess { exercise ->
                    _uiState.update { it.copy(exercise = exercise, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = when (error) {
                                is com.gymora.domain.model.EntityNotFoundException ->
                                    "This exercise no longer exists."
                                else -> "Something went wrong. Please try again."
                            },
                        )
                    }
                }
        }
    }
}
