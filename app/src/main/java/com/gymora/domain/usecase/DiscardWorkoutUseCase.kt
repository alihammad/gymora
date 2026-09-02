package com.gymora.domain.usecase

import com.gymora.domain.repository.WorkoutSessionRepository
import javax.inject.Inject

/**
 * Permanently delete an active session (FR-037, BR-15).
 * Confirmation is enforced by the UI before calling this.
 */
class DiscardWorkoutUseCase @Inject constructor(
    private val workoutSessionRepository: WorkoutSessionRepository,
) {

    suspend operator fun invoke(sessionId: Long) {
        workoutSessionRepository.discard(sessionId)
    }
}
