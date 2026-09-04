package com.gymora.data.repository

import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.entity.WorkoutSessionEntity
import com.gymora.data.local.entity.WorkoutSetEntity
import com.gymora.domain.calculator.WorkoutCalculators
import com.gymora.domain.model.PersonalRecord
import com.gymora.domain.model.PersonalRecords
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.repository.RecordsRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Computes personal records on demand from completed workout sessions (FR-047, R-09).
 * Each record carries exercise name and date context. Weight/volume values are
 * normalized to a canonical unit (KG) at the data boundary so cross-unit sets
 * compare correctly; display conversion happens in the presentation layer (FR-049, R-04).
 */
@Singleton
class RecordsRepositoryImpl @Inject constructor(
    private val database: GymoraDatabase,
) : RecordsRepository {

    override suspend fun getPersonalRecords(): PersonalRecords {
        val sessions = database.workoutSessionDao()
            .listCompleted(limit = Int.MAX_VALUE, offset = 0)
        if (sessions.isEmpty()) return PersonalRecords(null, null, null, null)

        val performedSets = collectPerformedSets(sessions)

        return PersonalRecords(
            heaviestWeight = heaviestWeight(performedSets),
            highestReps = highestReps(performedSets),
            bestEstimatedOneRepMax = bestEstimatedOneRepMax(performedSets),
            largestWorkoutVolume = largestWorkoutVolume(sessions),
        )
    }

    /** A performed set with exercise/date context; weight normalized to KG (R-04). */
    private data class PerformedSet(
        val weightKg: Double?,
        val reps: Int?,
        val isCompleted: Boolean,
        val exerciseName: String,
        val sessionDate: Instant,
    )

    private suspend fun collectPerformedSets(
        sessions: List<WorkoutSessionEntity>,
    ): List<PerformedSet> {
        val setDao = database.workoutSetDao()
        val exerciseDao = database.workoutExerciseDao()
        return sessions.flatMap { session ->
            exerciseDao.getForSession(session.id).flatMap { exercise ->
                setDao.getForExercise(exercise.id).map { set ->
                    PerformedSet(
                        weightKg = set.weight?.let { weight ->
                            WorkoutCalculators.convertWeight(
                                weight,
                                parseWeightUnit(set.weightUnit),
                                WeightUnit.KG,
                            )
                        },
                        reps = set.reps,
                        isCompleted = set.isCompleted,
                        exerciseName = exercise.exerciseNameSnapshot,
                        sessionDate = Instant.ofEpochMilli(session.startedAt),
                    )
                }
            }
        }
    }

    private fun heaviestWeight(sets: List<PerformedSet>): PersonalRecord? =
        sets.asSequence()
            .filter { it.isCompleted && it.weightKg != null && it.weightKg > 0 }
            .maxByOrNull { it.weightKg!! }
            ?.let { PersonalRecord(it.weightKg!!, it.exerciseName, it.sessionDate) }

    private fun highestReps(sets: List<PerformedSet>): PersonalRecord? =
        sets.asSequence()
            .filter { it.isCompleted && it.reps != null && it.reps > 0 }
            .maxByOrNull { it.reps!! }
            ?.let { PersonalRecord(it.reps!!.toDouble(), it.exerciseName, it.sessionDate) }

    private fun bestEstimatedOneRepMax(sets: List<PerformedSet>): PersonalRecord? =
        sets.asSequence()
            .filter { set ->
                set.isCompleted &&
                    set.weightKg != null && set.weightKg > 0 &&
                    set.reps != null && set.reps >= 1
            }
            .maxByOrNull { WorkoutCalculators.estimatedOneRepMax(it.weightKg!!, it.reps!!) }
            ?.let {
                PersonalRecord(
                    WorkoutCalculators.estimatedOneRepMax(it.weightKg!!, it.reps!!),
                    it.exerciseName,
                    it.sessionDate,
                )
            }

    private suspend fun largestWorkoutVolume(
        sessions: List<WorkoutSessionEntity>,
    ): PersonalRecord? {
        var best: Triple<String, Double, Instant>? = null
        for (session in sessions) {
            val volume = sessionVolume(session) ?: continue
            if (best == null || volume.second > best.second) best = volume
        }
        return best?.let { (name, volume, date) -> PersonalRecord(volume, name, date) }
    }

    private suspend fun sessionVolume(
        session: WorkoutSessionEntity,
    ): Triple<String, Double, Instant>? {
        val volume = database.workoutExerciseDao()
            .getForSession(session.id)
            .flatMap { exercise -> database.workoutSetDao().getForExercise(exercise.id) }
            .sumOf(::completedSetVolume)
        if (volume <= 0) return null
        return Triple(
            session.routineNameSnapshot,
            volume,
            Instant.ofEpochMilli(session.startedAt),
        )
    }

    private fun completedSetVolume(set: WorkoutSetEntity): Double {
        val weight = set.weight ?: return 0.0
        val reps = set.reps ?: return 0.0
        if (!set.isCompleted || weight <= 0) return 0.0
        return WorkoutCalculators.convertWeight(
            weight,
            parseWeightUnit(set.weightUnit),
            WeightUnit.KG,
        ) * reps
    }

    private fun parseWeightUnit(raw: String?): WeightUnit =
        try {
            WeightUnit.valueOf(raw ?: WeightUnit.KG.name)
        } catch (_: Exception) {
            WeightUnit.KG
        }
}
