package com.gymora.domain.usecase

import com.gymora.domain.repository.ExerciseRepository
import javax.inject.Inject

/**
 * Delete an exercise (FR-009, BR-04, R-03).
 *
 * T021 (US1) wires the soft-delete behavior. Template-reference removal
 * (removing the exercise from routine templates with confirmation) lands in
 * T029 (US2) once the routine tables exist. History is never touched.
 */
class DeleteExerciseUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {

    suspend operator fun invoke(exerciseId: Long) {
        exerciseRepository.delete(exerciseId)
        // T029 (US2) extends this with routine-template reference removal.
    }
}
