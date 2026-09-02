package com.gymora.domain.repository

import com.gymora.domain.model.ExercisePerformance
import com.gymora.domain.model.HistoryEntry
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.model.WorkoutDetail
import kotlinx.coroutines.flow.Flow

/**
 * History contract (FR-040..FR-044, contracts/repositories.md).
 * Read-side: never joins to templates for display names (snapshots, R-07).
 * Correction methods (FR-042) are scoped strictly to one session (US11).
 */
interface HistoryRepository {

    /** Completed sessions, newest first, paged (FR-040, FR-058, R-07). */
    suspend fun listCompleted(limit: Int, offset: Int): List<HistoryEntry>

    /** Full read-only workout graph from snapshot rows (FR-041). */
    suspend fun getWorkoutDetail(sessionId: Long): WorkoutDetail

    /** All performances of one exercise, newest first, paged (FR-044). */
    suspend fun getExerciseHistory(exerciseId: Long, limit: Int, offset: Int): List<ExercisePerformance>

    // --- FR-042 historical correction — scoped strictly to the given session (US11) ---

    suspend fun correctSet(
        setId: Long,
        weight: Double?,
        weightUnit: WeightUnit?,
        reps: Int?,
        isCompleted: Boolean,
        notes: String?,
    )

    suspend fun addExerciseToHistoricalWorkout(sessionId: Long, exerciseId: Long)

    suspend fun removeExerciseFromHistoricalWorkout(workoutExerciseId: Long)

    suspend fun updateHistoricalWorkoutNotes(sessionId: Long, notes: String?)
}
