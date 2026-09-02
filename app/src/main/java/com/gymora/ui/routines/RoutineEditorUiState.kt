package com.gymora.ui.routines

import com.gymora.domain.model.Exercise
import com.gymora.domain.model.RoutineDetail
import com.gymora.domain.model.SetTemplateInput

/** UI state for the routine editor (FR-011..FR-018). */
data class RoutineEditorUiState(
    val routine: RoutineDetail? = null,
    val isLoading: Boolean = true,
    val showExercisePicker: Boolean = false,
    val libraryExercises: List<Exercise> = emptyList(),
    val showDeleteConfirm: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null,
)
