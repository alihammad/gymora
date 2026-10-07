package com.gymora.domain.usecase

import com.gymora.domain.model.WeightUnit
import com.gymora.domain.repository.WorkoutSessionRepository
import javax.inject.Inject

/**
 * Validate + persist set value edits and completion (FR-023..FR-025, BR-16,
 * BR-05 write-through).
 */
class LogSetUseCase @Inject constructor(
    private val workoutSessionRepository: WorkoutSessionRepository,
) {

    suspend fun updateValues(
        setId: Long,
        weight: Double?,
        weightUnit: WeightUnit?,
        reps: Int?,
        durationSeconds: Int? = null,
        distanceMeters: Double? = null,
    ) {
        workoutSessionRepository.updateSetValues(setId, weight, weightUnit, reps, durationSeconds, distanceMeters)
    }

    suspend fun complete(setId: Long) {
        workoutSessionRepository.completeSet(setId)
    }

    suspend fun uncomplete(setId: Long) {
        workoutSessionRepository.uncompleteSet(setId)
    }
}
