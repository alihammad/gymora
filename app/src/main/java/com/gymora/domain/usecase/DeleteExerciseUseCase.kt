package com.gymora.domain.usecase

import com.gymora.domain.repository.ExerciseRepository
import com.gymora.domain.repository.RoutineRepository
import javax.inject.Inject

/**
 * Delete an exercise (FR-009, BR-04, R-03, OQ-2).
 *
 * Soft-deletes the exercise and removes its references from routine templates.
 * History is never touched (BR-04). Confirmation is handled by the UI (T021).
 */
class DeleteExerciseUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val routineRepository: RoutineRepository,
) {

    suspend operator fun invoke(exerciseId: Long) {
        // Remove template references first so routines stay consistent (R-03).
        routineRepository.removeExerciseReferences(exerciseId)
        exerciseRepository.delete(exerciseId)
    }
}
