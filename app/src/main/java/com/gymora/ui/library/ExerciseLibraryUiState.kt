package com.gymora.ui.library

import com.gymora.domain.model.Exercise
import com.gymora.domain.model.MuscleGroup

/** UI state for the exercise library screen (FR-008). */
data class ExerciseLibraryUiState(
    val groupedExercises: Map<MuscleGroup?, List<Exercise>> = emptyMap(),
    val searchQuery: String = "",
    val searchResults: List<Exercise>? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val pendingDelete: Exercise? = null,
) {
    /** True when a search is active and returned nothing (FR-008 empty-result state). */
    val isSearchEmpty: Boolean
        get() = searchResults?.isEmpty() == true
}
