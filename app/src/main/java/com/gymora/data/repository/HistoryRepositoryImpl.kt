package com.gymora.data.repository

import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.toCompletedSet
import com.gymora.data.local.toDomain
import com.gymora.data.local.weightIsLoad
import com.gymora.data.local.entity.WorkoutExerciseEntity
import com.gymora.domain.calculator.WorkoutCalculators
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

    override suspend fun completedDays(
        from: java.time.LocalDate,
        toExclusive: java.time.LocalDate,
    ): Set<java.time.LocalDate> {
        val zone = java.time.ZoneId.systemDefault()
        return sessionDao.completedStartTimesBetween(
            from.atStartOfDay(zone).toInstant().toEpochMilli(),
            toExclusive.atStartOfDay(zone).toInstant().toEpochMilli(),
        ).map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.toSet()
    }

    override suspend fun completedWorkoutDates(): List<java.time.LocalDate> {
        val zone = java.time.ZoneId.systemDefault()
        return sessionDao.completedStartTimesBetween(0L, Long.MAX_VALUE)
            .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
    }

    override suspend fun getWorkoutsOn(day: java.time.LocalDate): List<WorkoutDetail> {
        val zone = java.time.ZoneId.systemDefault()
        return sessionDao.listCompletedBetween(
            day.atStartOfDay(zone).toInstant().toEpochMilli(),
            day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
        ).map { getWorkoutDetail(it.id) }
    }

    override suspend fun getRoutineProgress(
        routineId: Long,
        limit: Int,
    ): List<com.gymora.domain.model.RoutineSessionPoint> =
        sessionDao.listCompletedForRoutine(routineId, limit).reversed().map { session ->
            val sets = exerciseDao.getForSession(session.id)
                .flatMap { setDao.getForExercise(it.id) }
            com.gymora.domain.model.RoutineSessionPoint(
                sessionId = session.id,
                date = Instant.ofEpochMilli(session.startedAt),
                volumeKg = WorkoutCalculators.totalVolume(
                    sets.map { it.toCompletedSet() },
                    WeightUnit.KG,
                ),
                completedSets = sets.count { it.isCompleted },
            )
        }

    override suspend fun getExerciseProgress(
        sinceMillis: Long,
    ): List<com.gymora.domain.model.ProgressSeries> {
        // exerciseId -> (name, per-session best estimated 1RM), sessions oldest first.
        val byExercise = linkedMapOf<Long, Pair<String, MutableList<Double>>>()
        sessionDao.listCompletedSince(sinceMillis).forEach { session ->
            exerciseDao.getForSession(session.id).forEach { we ->
                val exerciseId = we.exerciseId ?: return@forEach
                val best = setDao.getForExercise(we.id)
                    .filter { it.isCompleted && it.weightIsLoad && (it.weight ?: 0.0) > 0 && (it.reps ?: 0) > 0 }
                    .maxOfOrNull {
                        WorkoutCalculators.estimatedOneRepMax(
                            WorkoutCalculators.convertWeight(
                                it.weight!!,
                                it.weightUnit?.let { u -> WeightUnit.valueOf(u) } ?: WeightUnit.KG,
                                WeightUnit.KG,
                            ),
                            it.reps!!,
                        )
                    } ?: return@forEach
                byExercise.getOrPut(exerciseId) { we.exerciseNameSnapshot to mutableListOf() }
                    .second.add(best)
            }
        }
        return byExercise.map { (id, v) -> com.gymora.domain.model.ProgressSeries(id, v.first, v.second) }
    }

    override suspend fun getRoutineProgressSince(
        sinceMillis: Long,
    ): List<com.gymora.domain.model.ProgressSeries> {
        val byRoutine = linkedMapOf<Long, Pair<String, MutableList<Double>>>()
        sessionDao.listCompletedSince(sinceMillis).forEach { session ->
            val routineId = session.routineId ?: return@forEach
            val sets = exerciseDao.getForSession(session.id).flatMap { setDao.getForExercise(it.id) }
            val volume = WorkoutCalculators.totalVolume(
                sets.map { it.toCompletedSet() },
                WeightUnit.KG,
            )
            // Latest snapshot name wins if the routine was renamed.
            val entry = byRoutine.getOrPut(routineId) { session.routineNameSnapshot to mutableListOf() }
            byRoutine[routineId] = session.routineNameSnapshot to entry.second.also { it.add(volume) }
        }
        return byRoutine.map { (id, v) -> com.gymora.domain.model.ProgressSeries(id, v.first, v.second) }
    }

    override suspend fun getWorkoutStats(): List<com.gymora.domain.model.WorkoutStat> =
        sessionDao.listCompletedSince(0L).map { session ->
            val sets = exerciseDao.getForSession(session.id).flatMap { setDao.getForExercise(it.id) }
            com.gymora.domain.model.WorkoutStat(
                sessionId = session.id,
                routineName = session.routineNameSnapshot,
                startedAt = Instant.ofEpochMilli(session.startedAt),
                duration = Duration.ofMillis(
                    ((session.endedAt ?: session.startedAt) - session.startedAt).coerceAtLeast(0),
                ),
                volumeKg = WorkoutCalculators.totalVolume(
                    sets.map { it.toCompletedSet() },
                    WeightUnit.KG,
                ),
                totalReps = sets.filter { it.isCompleted }.sumOf { it.reps ?: 0 },
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
                    supersetGroup = workoutExercise.supersetGroup,
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
        durationSeconds: Int?,
        distanceMeters: Double?,
    ) {
        // BR-16 validation at the correction boundary, mirroring live logging.
        WorkoutCalculators.validateSetInput(weight, reps, durationSeconds, distanceMeters)
        val entity = setDao.getById(setId) ?: throw EntityNotFoundException(setId)
        setDao.update(
            entity.copy(
                weight = weight,
                weightUnit = weightUnit?.name,
                reps = reps,
                durationSeconds = durationSeconds,
                distanceMeters = distanceMeters,
                isCompleted = isCompleted,
                completedAt = when {
                    isCompleted && entity.completedAt == null -> System.currentTimeMillis()
                    !isCompleted -> null
                    else -> entity.completedAt
                },
                notes = notes,
            ),
        )
    }

    override suspend fun addExerciseToHistoricalWorkout(sessionId: Long, exerciseId: Long) {
        val session = sessionDao.getById(sessionId) ?: throw EntityNotFoundException(sessionId)
        val exerciseEntity = database.exerciseDao().getById(exerciseId)
            ?: throw EntityNotFoundException(exerciseId)

        exerciseDao.insert(
            WorkoutExerciseEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                // Name snapshot captured at edit time (FR-056).
                exerciseNameSnapshot = exerciseEntity.name,
                position = exerciseDao.nextPosition(sessionId),
                notes = null,
            ),
        )
    }

    override suspend fun removeExerciseFromHistoricalWorkout(workoutExerciseId: Long) {
        // CASCADE removes the exercise's sets; scoped strictly to this session (BR-11).
        exerciseDao.deleteById(workoutExerciseId)
    }

    override suspend fun deleteWorkout(sessionId: Long) {
        sessionDao.deleteById(sessionId)
    }

    override suspend fun updateHistoricalWorkoutNotes(sessionId: Long, notes: String?) {
        val entity = sessionDao.getById(sessionId) ?: throw EntityNotFoundException(sessionId)
        sessionDao.update(entity.copy(notes = notes))
    }

    override suspend fun updateHistoricalWorkoutTimes(
        sessionId: Long,
        startedAt: Instant,
        endedAt: Instant,
    ) {
        val entity = sessionDao.getById(sessionId) ?: throw EntityNotFoundException(sessionId)
        sessionDao.update(
            entity.copy(startedAt = startedAt.toEpochMilli(), endedAt = endedAt.toEpochMilli()),
        )
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

}
