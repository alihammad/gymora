package com.gymora.domain.usecase

import com.gymora.domain.model.WorkoutSummary
import com.gymora.domain.repository.WorkoutSessionRepository
import javax.inject.Inject

/**
 * Record end timestamp, mark completed, build summary (FR-033..FR-036, BR-13).
 */
class FinishWorkoutUseCase @Inject constructor(
    private val workoutSessionRepository: WorkoutSessionRepository,
) {

    suspend operator fun invoke(sessionId: Long): WorkoutSummary =
        workoutSessionRepository.finish(sessionId)
}
