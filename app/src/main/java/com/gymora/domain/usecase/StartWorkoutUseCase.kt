package com.gymora.domain.usecase

import com.gymora.domain.repository.WorkoutSessionRepository
import javax.inject.Inject

/**
 * Start a session from a routine (FR-019, FR-020, BR-14).
 * Enforces the single-active invariant; throws ActiveWorkoutConflictException
 * when a workout is already in progress.
 */
class StartWorkoutUseCase @Inject constructor(
    private val workoutSessionRepository: WorkoutSessionRepository,
) {

    suspend operator fun invoke(routineId: Long): Long =
        workoutSessionRepository.startFromRoutine(routineId)

    /** Start a standalone session for a single exercise, without a routine. */
    suspend fun startAdHoc(exerciseId: Long): Long =
        workoutSessionRepository.startAdHoc(exerciseId)
}
