package com.gymora.ui.workout

import com.gymora.domain.model.ActiveWorkout
import com.gymora.domain.model.Exercise

/** UI state for the active workout screen (FR-022..FR-037). */
data class ActiveWorkoutUiState(
    val activeWorkout: ActiveWorkout? = null,
    val elapsedSeconds: Long = 0,
    val isLoading: Boolean = true,
    val showExercisePicker: Boolean = false,
    val libraryExercises: List<Exercise> = emptyList(),
    val pendingAddExerciseId: Long? = null,
    val showFinishConfirm: Boolean = false,
    val showCancelConfirm: Boolean = false,
    val finishedSessionId: Long? = null,
    val errorMessage: String? = null,
)
