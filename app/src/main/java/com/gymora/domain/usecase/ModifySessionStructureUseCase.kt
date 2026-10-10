package com.gymora.domain.usecase

import com.gymora.domain.repository.WorkoutSessionRepository
import javax.inject.Inject

/**
 * Add/delete sets; add/remove exercises in a session; "add to routine" choice
 * (FR-026, FR-028, FR-029, BR-09/10).
 */
class ModifySessionStructureUseCase @Inject constructor(
    private val workoutSessionRepository: WorkoutSessionRepository,
) {

    suspend fun addSet(workoutExerciseId: Long): Long =
        workoutSessionRepository.addSet(workoutExerciseId)

    suspend fun deleteSet(setId: Long) {
        workoutSessionRepository.deleteSet(setId)
    }

    suspend fun addExercise(sessionId: Long, exerciseId: Long, addToRoutine: Boolean) {
        workoutSessionRepository.addExerciseToSession(sessionId, exerciseId, addToRoutine)
    }

    /** Session-only removal; the routine is untouched (FR-029). */
    suspend fun removeExercise(workoutExerciseId: Long) {
        workoutSessionRepository.removeExerciseFromSession(workoutExerciseId)
    }

    /** Session-only reorder; the routine is untouched. */
    suspend fun reorderExercises(sessionId: Long, orderedWorkoutExerciseIds: List<Long>) {
        workoutSessionRepository.reorderSessionExercises(sessionId, orderedWorkoutExerciseIds)
    }
}
