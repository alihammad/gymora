package com.gymora.domain.usecase

import com.gymora.domain.model.WeightUnit
import com.gymora.domain.repository.HistoryRepository
import javax.inject.Inject

/**
 * Scoped edits to one completed workout (FR-042, BR-11). History is read-only
 * by default; these methods are invoked only after the user explicitly opts
 * into editing a workout.
 */
class CorrectHistoricalWorkoutUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {

    suspend fun correctSet(
        setId: Long,
        weight: Double?,
        weightUnit: WeightUnit?,
        reps: Int?,
        isCompleted: Boolean,
        notes: String?,
        durationSeconds: Int? = null,
        distanceMeters: Double? = null,
    ) {
        historyRepository.correctSet(
            setId, weight, weightUnit, reps, isCompleted, notes, durationSeconds, distanceMeters,
        )
    }

    suspend fun addExercise(sessionId: Long, exerciseId: Long) {
        historyRepository.addExerciseToHistoricalWorkout(sessionId, exerciseId)
    }

    suspend fun removeExercise(workoutExerciseId: Long) {
        historyRepository.removeExerciseFromHistoricalWorkout(workoutExerciseId)
    }

    suspend fun deleteWorkout(sessionId: Long) {
        historyRepository.deleteWorkout(sessionId)
    }

    suspend fun updateNotes(sessionId: Long, notes: String?) {
        historyRepository.updateHistoricalWorkoutNotes(sessionId, notes)
    }

    suspend fun updateTimes(
        sessionId: Long,
        startedAt: java.time.Instant,
        endedAt: java.time.Instant,
    ) {
        historyRepository.updateHistoricalWorkoutTimes(sessionId, startedAt, endedAt)
    }
}
