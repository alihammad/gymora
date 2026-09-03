package com.gymora.data.repository

import com.gymora.data.local.db.GymoraDatabase
import com.gymora.domain.model.ActiveExercise
import com.gymora.domain.model.ActiveSet
import com.gymora.domain.model.EntityNotFoundException
import com.gymora.domain.model.ExercisePerformance
import com.gymora.domain.model.HistoryEntry
import com.gymora.domain.model.MeasurementType
import com.gymora.domain.model.SessionStatus
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.model.WorkoutDetail
import com.gymora.domain.model.WorkoutSession
import com.gymora.domain.repository.HistoryRepository
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val database: GymoraDatabase,
) : HistoryRepository {

    private val sessionDao get() = database.workoutSessionDao()
    private val exerciseDao get() = database.workoutExerciseDao()
    private val setDao get() = database.workoutSetDao()

    override suspend fun listCompleted(limit: Int, offset: Int): List<HistoryEntry> =
        sessionDao.listCompleted(limit, offset).map { entity ->
            HistoryEntry(
                id = entity.id,
                routineNameSnapshot = entity.routineNameSnapshot,
                startedAt = Instant.ofEpochMilli(entity.startedAt),
                duration = Duration.ofMillis(
                    (entity.endedAt ?: entity.startedAt) - entity.startedAt,
                ),
            )
        }

    override suspend fun getWorkoutDetail(sessionId: Long): WorkoutDetail {
        val session = sessionDao.getById(sessionId) ?: throw EntityNotFoundException(sessionId)
        return WorkoutDetail(
            session = session.toDomain(),
            // Full graph read straight from snapshot rows — no joins to
            // templates for display names (R-07, FR-043).
            exercises = exerciseDao.getForSession(sessionId).map { workoutExercise ->
                ActiveExercise(
                    workoutExerciseId = workoutExercise.id,
                    exerciseId = workoutExercise.exerciseId,
                    exerciseName = workoutExercise.exerciseNameSnapshot,
                    position = workoutExercise.position,
                    notes = workoutExercise.notes,
                    sets = setDao.getForExercise(workoutExercise.id).map { it.toDomain() },
                )
            },
        )
    }

    override suspend fun getExerciseHistory(
        exerciseId: Long,
        limit: Int,
        offset: Int,
    ): List<ExercisePerformance> {
        val workoutExercises = exerciseDao.getForExerciseAcrossSessions(
            exerciseId, limit, offset,
        )
        return workoutExercises.map { we ->
            val session = sessionDao.getById(we.sessionId)
            ExercisePerformance(
                sessionId = we.sessionId,
                date = session?.let { Instant.ofEpochMilli(it.startedAt) } ?: Instant.EPOCH,
                sets = setDao.getForExercise(we.id).map { it.toDomain() },
            )
        }
    }

    // --- FR-042 historical correction (US11, T069) ---

    override suspend fun correctSet(
        setId: Long,
        weight: Double?,
        weightUnit: WeightUnit?,
        reps: Int?,
        isCompleted: Boolean,
        notes: String?,
    ) {
        // Implemented in US11 (T069).
    }

    override suspend fun addExerciseToHistoricalWorkout(sessionId: Long, exerciseId: Long) {
        // Implemented in US11 (T069).
    }

    override suspend fun removeExerciseFromHistoricalWorkout(workoutExerciseId: Long) {
        // Implemented in US11 (T069).
    }

    override suspend fun updateHistoricalWorkoutNotes(sessionId: Long, notes: String?) {
        // Implemented in US11 (T069).
    }

    private fun com.gymora.data.local.entity.WorkoutSessionEntity.toDomain(): WorkoutSession =
        WorkoutSession(
            id = id,
            routineId = routineId,
            routineNameSnapshot = routineNameSnapshot,
            startedAt = Instant.ofEpochMilli(startedAt),
            endedAt = endedAt?.let { Instant.ofEpochMilli(it) },
            status = SessionStatus.valueOf(status),
            notes = notes,
        )

    private fun com.gymora.data.local.entity.WorkoutSetEntity.toDomain(): ActiveSet = ActiveSet(
        id = id,
        setNumber = setNumber,
        reps = reps,
        weight = weight,
        weightUnit = weightUnit?.let { WeightUnit.valueOf(it) },
        measurementType = MeasurementType.valueOf(measurementType),
        isCompleted = isCompleted,
        completedAt = completedAt?.let { Instant.ofEpochMilli(it) },
        notes = notes,
    )
}
