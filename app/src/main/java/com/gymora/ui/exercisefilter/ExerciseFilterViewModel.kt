package com.gymora.ui.exercisefilter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.model.DifficultyLevel
import com.gymora.domain.model.Exercise
import com.gymora.domain.model.ExerciseCategory
import com.gymora.domain.model.ExerciseType
import com.gymora.domain.model.ForceType
import com.gymora.domain.model.Mechanics
import com.gymora.domain.model.MuscleGroup
import com.gymora.domain.repository.ExerciseRepository
import com.gymora.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Which exercise attribute a filter screen is scoped to (matches the detail-screen chips). */
enum class ExerciseFilterKind(val label: String) {
    CATEGORY("Category"),
    LEVEL("Level"),
    FORCE("Force"),
    MECHANICS("Mechanics"),
    TYPE("Type"),
    MUSCLE("Muscle"),
    EQUIPMENT("Equipment"),
}

/** UI state for the filtered exercise list. */
data class ExerciseFilterUiState(
    val title: String = "",
    val exercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ExerciseFilterViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val kind: ExerciseFilterKind? = savedStateHandle.get<String>(Destinations.ExerciseFilter.KIND_ARG)
        ?.let { arg -> ExerciseFilterKind.entries.firstOrNull { it.name == arg } }
    private val value: String = savedStateHandle.get<String>(Destinations.ExerciseFilter.VALUE_ARG).orEmpty()

    private val _uiState = MutableStateFlow(ExerciseFilterUiState(title = titleFor()))
    val uiState: StateFlow<ExerciseFilterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            exerciseRepository.observeLibrary().collect { exercises ->
                _uiState.update {
                    it.copy(
                        exercises = exercises.filter(::matches).sortedBy { e -> e.name },
                        isLoading = false,
                    )
                }
            }
        }
    }

    private fun matches(exercise: Exercise): Boolean = when (kind) {
        ExerciseFilterKind.CATEGORY -> exercise.category?.name == value
        ExerciseFilterKind.LEVEL -> exercise.difficultyLevel?.name == value
        ExerciseFilterKind.FORCE -> exercise.forceType?.name == value
        ExerciseFilterKind.MECHANICS -> exercise.mechanics?.name == value
        ExerciseFilterKind.TYPE -> exercise.type?.name == value
        ExerciseFilterKind.MUSCLE ->
            exercise.muscleGroups.any { it.group.name == value } || exercise.muscleGroup?.name == value
        ExerciseFilterKind.EQUIPMENT -> exercise.equipment.any { it.name == value }
        null -> false
    }

    private fun titleFor(): String = when (kind) {
        ExerciseFilterKind.CATEGORY -> ExerciseCategory.entries.firstOrNull { it.name == value }?.displayName
        ExerciseFilterKind.LEVEL -> DifficultyLevel.entries.firstOrNull { it.name == value }?.displayName
        ExerciseFilterKind.FORCE -> ForceType.entries.firstOrNull { it.name == value }?.displayName
        ExerciseFilterKind.MECHANICS -> Mechanics.entries.firstOrNull { it.name == value }?.displayName
        ExerciseFilterKind.TYPE -> ExerciseType.entries.firstOrNull { it.name == value }?.name?.lowercase()
            ?.replaceFirstChar { it.uppercase() }
        ExerciseFilterKind.MUSCLE -> MuscleGroup.entries.firstOrNull { it.name == value }?.displayName
        ExerciseFilterKind.EQUIPMENT -> value
        null -> null
    } ?: "Exercises"
}
