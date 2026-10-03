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

    /** Local dates in [from, toExclusive) on which at least one workout was completed. */
    suspend fun completedDays(from: java.time.LocalDate, toExclusive: java.time.LocalDate): Set<java.time.LocalDate>

    /** Full details of every workout completed on the given local [day], oldest first. */
    suspend fun getWorkoutsOn(day: java.time.LocalDate): List<WorkoutDetail>

    /** Completed sessions of a routine, oldest first (max [limit] most recent), with volume in kg. */
    suspend fun getRoutineProgress(routineId: Long, limit: Int): List<com.gymora.domain.model.RoutineSessionPoint>

    /** Estimated 1RM (kg) per session for every exercise performed since [sinceMillis]. */
    suspend fun getExerciseProgress(sinceMillis: Long): List<com.gymora.domain.model.ProgressSeries>

    /** Training volume (kg) per session for every routine performed since [sinceMillis]. */
    suspend fun getRoutineProgressSince(sinceMillis: Long): List<com.gymora.domain.model.ProgressSeries>

    /** Headline stats of every completed workout, oldest first (profile charts and calendar). */
    suspend fun getWorkoutStats(): List<com.gymora.domain.model.WorkoutStat>

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
