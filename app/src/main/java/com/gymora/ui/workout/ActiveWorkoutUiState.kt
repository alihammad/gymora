package com.gymora.ui.workout

import com.gymora.domain.model.ActiveWorkout
import com.gymora.domain.model.Exercise
import com.gymora.domain.model.PreviousPerformance

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
    /** Previous performance per exercise for pre-fill display (FR-045, FR-046). */
    val previousPerformanceMap: Map<Long, PreviousPerformance> = emptyMap(),
)
