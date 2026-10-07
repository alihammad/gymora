package com.gymora.domain.repository

import com.gymora.domain.model.ActiveWorkout
import com.gymora.domain.model.PreviousPerformance
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.model.WorkoutSession
import com.gymora.domain.model.WorkoutSummary
import kotlinx.coroutines.flow.Flow

/**
 * Workout session contract (FR-019..FR-039, contracts/repositories.md).
 * All writes are write-through (R-05): durable before returning (FR-027).
 */
interface WorkoutSessionRepository {

    /** The single ACTIVE session with its full graph, if any (BR-14). */
    fun observeActiveSession(): Flow<ActiveWorkout?>

    /**
     * Creates session (status ACTIVE, started_at = now) by copying routine
     * exercises + planned sets into historical rows (FR-019).
     * Throws ActiveWorkoutConflictException if one is already active (FR-020).
     */
    suspend fun startFromRoutine(routineId: Long): Long

    /** Full active-workout graph for rendering/recovery (FR-039). */
    suspend fun getActiveWorkout(sessionId: Long): ActiveWorkout

    /** Most recent completed performance per exercise for pre-fill (FR-045/046). */
    suspend fun getPreviousPerformance(exerciseId: Long): PreviousPerformance?

    /** Set logging — every call persists immediately (FR-025, FR-027). */
    suspend fun updateSetValues(
        setId: Long,
        weight: Double?,
        weightUnit: WeightUnit?,
        reps: Int?,
        durationSeconds: Int? = null,
        distanceMeters: Double? = null,
    )
    suspend fun completeSet(setId: Long)
    suspend fun uncompleteSet(setId: Long)
    /** FR-026. Adds a left and a right set for unilateral exercises; returns the first id. */
    suspend fun addSet(workoutExerciseId: Long): Long
    suspend fun deleteSet(setId: Long)

    /** FR-028: add exercise to session; addToRoutine=true also appends it to the source routine. */
    suspend fun addExerciseToSession(sessionId: Long, exerciseId: Long, addToRoutine: Boolean)

    /** FR-029: remove from this session only; routine untouched. */
    suspend fun removeExerciseFromSession(workoutExerciseId: Long)

    suspend fun updateSessionNotes(sessionId: Long, notes: String?)

    /** Records ended_at, sets status COMPLETED, returns summary (FR-033/034/035). */
    suspend fun finish(sessionId: Long): WorkoutSummary

    /** Rebuilds the summary from a persisted (completed) session (FR-035). */
    suspend fun getSummary(sessionId: Long): WorkoutSummary

    /** Permanent deletion of the session graph (BR-15). UI requires confirmation (FR-037). */
    suspend fun discard(sessionId: Long)
}
