package com.gymora.data.repository

import androidx.room.withTransaction
import com.gymora.data.local.dao.WorkoutExerciseDao
import com.gymora.data.local.dao.WorkoutSessionDao
import com.gymora.data.local.dao.WorkoutSetDao
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.entity.WorkoutExerciseEntity
import com.gymora.data.local.entity.WorkoutSessionEntity
import com.gymora.data.local.entity.WorkoutSetEntity
import com.gymora.domain.calculator.WorkoutCalculators
import com.gymora.domain.model.ActiveExercise
import com.gymora.domain.model.ActiveSet
import com.gymora.domain.model.ActiveWorkout
import com.gymora.domain.model.ActiveWorkoutConflictException
import com.gymora.domain.model.CompletedSet
import com.gymora.domain.model.EntityNotFoundException
import com.gymora.domain.model.MeasurementType
import com.gymora.domain.model.PreviousPerformance
import com.gymora.domain.model.SessionStatus
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.model.WorkoutSession
import com.gymora.domain.model.WorkoutSummary
import com.gymora.domain.model.ExerciseSummary
import com.gymora.domain.repository.WorkoutSessionRepository
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class WorkoutSessionRepositoryImpl @Inject constructor(
    private val database: GymoraDatabase,
) : WorkoutSessionRepository {

    private val sessionDao: WorkoutSessionDao get() = database.workoutSessionDao()
    private val exerciseDao: WorkoutExerciseDao get() = database.workoutExerciseDao()
    private val setDao: WorkoutSetDao get() = database.workoutSetDao()

    override fun observeActiveSession(): Flow<ActiveWorkout?> =
        sessionDao.observeActive().map { entity ->
            if (entity == null) {
                null
            } else {
                ActiveWorkout(
                    session = entity.toDomain(),
                    exercises = buildExerciseGraph(entity.id),
                    startedAt = Instant.ofEpochMilli(entity.startedAt),
                )
            }
        }

    override suspend fun startFromRoutine(routineId: Long): Long {
        // FR-020 / BR-14: at most one active session.
        sessionDao.getActiveOnce()?.let { active ->
            throw ActiveWorkoutConflictException(active.id)
        }

        val routine = database.routineDao().getById(routineId)
            ?: throw EntityNotFoundException(routineId)
        val routineExercises = database.routineExerciseDao().getForRoutine(routineId)
        val now = System.currentTimeMillis()

        return database.withTransaction {
            val sessionId = sessionDao.insert(
                WorkoutSessionEntity(
                    routineId = routineId,
                    routineNameSnapshot = routine.name, // FR-056 snapshot
                    startedAt = now,
                    endedAt = null,
                    status = SessionStatus.ACTIVE.name,
                    notes = null,
                    createdAt = now,
                ),
            )
            routineExercises.forEach { routineExercise ->
                val exerciseEntity = database.exerciseDao().getById(routineExercise.exerciseId)
                val workoutExerciseId = exerciseDao.insert(
                    WorkoutExerciseEntity(
                        sessionId = sessionId,
                        exerciseId = routineExercise.exerciseId,
                        exerciseNameSnapshot = exerciseEntity?.name ?: "Unknown exercise",
                        position = routineExercise.position,
                        notes = routineExercise.notes,
                    ),
                )
                val templates = database.setTemplateDao()
                    .getForRoutineExercise(routineExercise.id)
                templates.forEach { template ->
                    setDao.insert(
                        WorkoutSetEntity(
                            workoutExerciseId = workoutExerciseId,
                            setNumber = template.setNumber,
                            reps = template.targetReps,
                            weight = template.targetWeight,
                            weightUnit = template.targetWeightUnit,
                            measurementType = template.measurementType,
                            isCompleted = false,
                            completedAt = null,
                            notes = null,
                        ),
                    )
                }
            }
            sessionId
        }
    }

    override suspend fun getActiveWorkout(sessionId: Long): ActiveWorkout {
        val session = sessionDao.getById(sessionId) ?: throw EntityNotFoundException(sessionId)
        return ActiveWorkout(
            session = session.toDomain(),
            exercises = buildExerciseGraph(sessionId),
            startedAt = Instant.ofEpochMilli(session.startedAt),
        )
    }

    override suspend fun getPreviousPerformance(exerciseId: Long): PreviousPerformance? {
        // Most recent completed session containing this exercise (FR-045, T052).
        val session = sessionDao.latestCompletedSessionForExercise(exerciseId) ?: return null
        val exercise = exerciseDao.getForSession(session.id)
            .firstOrNull { it.exerciseId == exerciseId } ?: return null
        val sets = setDao.getForExercise(exercise.id).filter { it.isCompleted }
        if (sets.isEmpty()) return null
        return PreviousPerformance(
            exerciseId = exerciseId,
            date = Instant.ofEpochMilli(session.startedAt),
            sets = sets.map { set ->
                com.gymora.domain.model.SetValue(
                    weight = set.weight,
                    weightUnit = set.weightUnit?.let { WeightUnit.valueOf(it) },
                    reps = set.reps,
                )
            },
        )
    }

    override suspend fun updateSetValues(
        setId: Long,
        weight: Double?,
        weightUnit: WeightUnit?,
        reps: Int?,
    ) {
        WorkoutCalculators.validateSetInput(weight, reps) // BR-16
        val entity = setDao.getById(setId) ?: throw EntityNotFoundException(setId)
        setDao.update(
            entity.copy(
                weight = weight,
                weightUnit = weightUnit?.name,
                reps = reps,
            ),
        )
    }

    override suspend fun completeSet(setId: Long) {
        val entity = setDao.getById(setId) ?: throw EntityNotFoundException(setId)
        setDao.update(entity.copy(isCompleted = true, completedAt = System.currentTimeMillis()))
    }

    override suspend fun uncompleteSet(setId: Long) {
        val entity = setDao.getById(setId) ?: throw EntityNotFoundException(setId)
        setDao.update(entity.copy(isCompleted = false, completedAt = null))
    }

    override suspend fun addSet(workoutExerciseId: Long): Long {
        exerciseDao.getById(workoutExerciseId) ?: throw EntityNotFoundException(workoutExerciseId)
        return setDao.insert(
            WorkoutSetEntity(
                workoutExerciseId = workoutExerciseId,
                setNumber = setDao.nextSetNumber(workoutExerciseId),
                reps = null,
                weight = null,
                weightUnit = null,
                measurementType = MeasurementType.WEIGHT_AND_REPS.name,
                isCompleted = false,
                completedAt = null,
                notes = null,
            ),
        )
    }

    override suspend fun deleteSet(setId: Long) {
        setDao.deleteById(setId)
    }

    override suspend fun addExerciseToSession(
        sessionId: Long,
        exerciseId: Long,
        addToRoutine: Boolean,
    ) {
        val session = sessionDao.getById(sessionId) ?: throw EntityNotFoundException(sessionId)
        val exerciseEntity = database.exerciseDao().getById(exerciseId)
            ?: throw EntityNotFoundException(exerciseId)

        database.withTransaction {
            exerciseDao.insert(
                WorkoutExerciseEntity(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    exerciseNameSnapshot = exerciseEntity.name,
                    position = exerciseDao.nextPosition(sessionId),
                    notes = null,
                ),
            )
            // FR-028: optionally also append to the source routine for the future.
            if (addToRoutine && session.routineId != null) {
                val position = database.routineExerciseDao().nextPosition(session.routineId)
                database.routineExerciseDao().insert(
                    com.gymora.data.local.entity.RoutineExerciseEntity(
                        routineId = session.routineId,
                        exerciseId = exerciseId,
                        position = position,
                        notes = null,
                    ),
                )
            }
        }
    }

    override suspend fun removeExerciseFromSession(workoutExerciseId: Long) {
        // FR-029: session-only removal; the routine template is untouched.
        exerciseDao.deleteById(workoutExerciseId)
    }

    override suspend fun updateSessionNotes(sessionId: Long, notes: String?) {
        val entity = sessionDao.getById(sessionId) ?: throw EntityNotFoundException(sessionId)
        sessionDao.update(entity.copy(notes = notes))
    }

    override suspend fun finish(sessionId: Long): WorkoutSummary {
        val session = sessionDao.getById(sessionId) ?: throw EntityNotFoundException(sessionId)
        val endedAt = System.currentTimeMillis()
        sessionDao.finish(sessionId, endedAt)
        return buildSummary(sessionId, endedAt)
    }

    override suspend fun getSummary(sessionId: Long): WorkoutSummary {
        val session = sessionDao.getById(sessionId) ?: throw EntityNotFoundException(sessionId)
        return buildSummary(sessionId, session.endedAt ?: session.startedAt)
    }

    private suspend fun buildSummary(sessionId: Long, endedAt: Long): WorkoutSummary {
        val session = sessionDao.getById(sessionId) ?: throw EntityNotFoundException(sessionId)
        val exercises = buildExerciseGraph(sessionId)
        val allSets = exercises.flatMap { exercise -> exercise.sets }
        val completedSets = allSets.filter { it.isCompleted }

        return WorkoutSummary(
            sessionId = sessionId,
            routineNameSnapshot = session.routineNameSnapshot,
            date = Instant.ofEpochMilli(session.startedAt),
            duration = Duration.ofMillis(endedAt - session.startedAt),
            exerciseCount = exercises.size,
            completedSetCount = completedSets.size,
            totalReps = completedSets.sumOf { it.reps ?: 0 },
            totalVolume = WorkoutCalculators.totalVolume(
                allSets.map { set ->
                    CompletedSet(
                        weight = set.weight,
                        reps = set.reps,
                        weightUnit = set.weightUnit,
                        isCompleted = set.isCompleted,
                    )
                },
                displayUnit = WeightUnit.KG,
            ),
            perExerciseBreakdown = exercises.map { exercise ->
                ExerciseSummary(
                    exerciseName = exercise.exerciseName,
                    completedSetCount = exercise.sets.count { it.isCompleted },
                )
            },
        )
    }

    override suspend fun discard(sessionId: Long) {
        // BR-15: permanent deletion; CASCADE removes exercises and sets.
        sessionDao.deleteById(sessionId)
    }

    private suspend fun buildExerciseGraph(sessionId: Long): List<ActiveExercise> =
        exerciseDao.getForSession(sessionId).map { workoutExercise ->
            ActiveExercise(
                workoutExerciseId = workoutExercise.id,
                exerciseId = workoutExercise.exerciseId,
                exerciseName = workoutExercise.exerciseNameSnapshot,
                position = workoutExercise.position,
                notes = workoutExercise.notes,
                sets = setDao.getForExercise(workoutExercise.id).map { it.toDomain() },
            )
        }

    private fun WorkoutSessionEntity.toDomain(): WorkoutSession = WorkoutSession(
        id = id,
        routineId = routineId,
        routineNameSnapshot = routineNameSnapshot,
        startedAt = Instant.ofEpochMilli(startedAt),
        endedAt = endedAt?.let { Instant.ofEpochMilli(it) },
        status = SessionStatus.valueOf(status),
        notes = notes,
    )

    private fun WorkoutSetEntity.toDomain(): ActiveSet = ActiveSet(
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
