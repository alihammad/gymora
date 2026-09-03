package com.gymora.domain.usecase

import com.gymora.domain.model.PreviousPerformance
import com.gymora.domain.repository.WorkoutSessionRepository
import javax.inject.Inject

/**
 * Fetch last performance + build pre-fill values (FR-045, FR-046).
 */
class PreviousPerformanceUseCase @Inject constructor(
    private val workoutSessionRepository: WorkoutSessionRepository,
) {

    suspend operator fun invoke(exerciseId: Long): PreviousPerformance? =
        workoutSessionRepository.getPreviousPerformance(exerciseId)
}
