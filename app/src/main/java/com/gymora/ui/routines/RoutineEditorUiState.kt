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
    /** True until a freshly created workout is first saved. */
    val isNewWorkout: Boolean = false,
    val progress: List<com.gymora.domain.model.RoutineSessionPoint> = emptyList(),
    val showDeleteConfirm: Boolean = false,
    /** One-shot snackbar text: errors and save confirmations. */
    val message: String? = null,
)
