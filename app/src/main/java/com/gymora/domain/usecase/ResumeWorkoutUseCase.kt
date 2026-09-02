package com.gymora.domain.usecase

import com.gymora.domain.model.ActiveWorkout
import com.gymora.domain.repository.WorkoutSessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Detect + restore an active session on launch (FR-038, FR-039, BR-06).
 * Duration needs no timer state: it is derived from started_at (BR-07, R-05).
 */
class ResumeWorkoutUseCase @Inject constructor(
    private val workoutSessionRepository: WorkoutSessionRepository,
) {

    /** The unfinished workout, if any — drives the recovery prompt (FR-038). */
    suspend fun detect(): ActiveWorkout? =
        workoutSessionRepository.observeActiveSession().first()

    /** Full session graph for the resumed workout (FR-039). */
    suspend fun resume(sessionId: Long): ActiveWorkout =
        workoutSessionRepository.getActiveWorkout(sessionId)
}
