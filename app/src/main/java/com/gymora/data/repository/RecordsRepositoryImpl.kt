package com.gymora.data.repository

import com.gymora.data.local.db.GymoraDatabase
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
        val setDao = database.workoutSetDao()
        val exerciseDao = database.workoutExerciseDao()
        val sessionDao = database.workoutSessionDao()

        val sessions = sessionDao.listCompleted(limit = Int.MAX_VALUE, offset = 0)
        if (sessions.isEmpty()) return PersonalRecords(null, null, null, null)

        // A performed set with exercise/date context; weight normalized to KG (R-04).
        data class PerformedSet(
            val weightKg: Double?,
            val reps: Int?,
            val isCompleted: Boolean,
            val exerciseName: String,
            val sessionDate: Instant,
        )

        val allSets = mutableListOf<PerformedSet>()
        for (session in sessions) {
            val exercises = exerciseDao.getForSession(session.id)
            for (exercise in exercises) {
                val sets = setDao.getForExercise(exercise.id)
                for (set in sets) {
                    val unit = parseWeightUnit(set.weightUnit)
                    val weightKg = set.weight?.let { w ->
                        WorkoutCalculators.convertWeight(w, unit, WeightUnit.KG)
                    }
                    allSets.add(
                        PerformedSet(
                            weightKg = weightKg,
                            reps = set.reps,
                            isCompleted = set.isCompleted,
                            exerciseName = exercise.exerciseNameSnapshot,
                            sessionDate = Instant.ofEpochMilli(session.startedAt),
                        ),
                    )
                }
            }
        }

        // Heaviest weight: max completed weighted set with weight > 0 (FR-047).
        val heaviestWeight = allSets
            .filter { it.isCompleted && it.weightKg != null && it.weightKg > 0 }
            .maxByOrNull { it.weightKg!! }
            ?.let { PersonalRecord(it.weightKg!!, it.exerciseName, it.sessionDate) }

        // Highest reps: max completed set with reps > 0 (FR-047).
        val highestReps = allSets
            .filter { it.isCompleted && it.reps != null && it.reps > 0 }
            .maxByOrNull { it.reps!! }
            ?.let { PersonalRecord(it.reps!!.toDouble(), it.exerciseName, it.sessionDate) }

        // Best estimated 1RM (Epley): max among completed weighted sets, reps ≥ 1 (R-09).
        val bestEpley = allSets
            .filter {
                it.isCompleted && it.weightKg != null && it.weightKg > 0 &&
                    it.reps != null && it.reps >= 1
            }
            .maxByOrNull { WorkoutCalculators.estimatedOneRepMax(it.weightKg!!, it.reps!!) }
            ?.let {
                PersonalRecord(
                    WorkoutCalculators.estimatedOneRepMax(it.weightKg!!, it.reps!!),
                    it.exerciseName,
                    it.sessionDate,
                )
            }

        // Largest workout volume: per-session Σ(weight × reps) over completed weighted sets.
        val largestVolume = sessions
            .mapNotNull { session ->
                val volume = exerciseDao.getForSession(session.id)
                    .flatMap { ex -> setDao.getForExercise(ex.id) }
                    .sumOf { set ->
                        if (!set.isCompleted || set.weight == null || set.weight <= 0 || set.reps == null) {
                            0.0
                        } else {
                            val unit = parseWeightUnit(set.weightUnit)
                            WorkoutCalculators.convertWeight(set.weight, unit, WeightUnit.KG) * set.reps
                        }
                    }
                if (volume > 0) {
                    Triple(session.routineNameSnapshot, volume, Instant.ofEpochMilli(session.startedAt))
                } else {
                    null
                }
            }
            .maxByOrNull { it.second }
            ?.let { (name, volume, date) -> PersonalRecord(volume, name, date) }

        return PersonalRecords(
            heaviestWeight = heaviestWeight,
            highestReps = highestReps,
            bestEstimatedOneRepMax = bestEpley,
            largestWorkoutVolume = largestVolume,
        )
    }

    private fun parseWeightUnit(raw: String?): WeightUnit =
        try {
            WeightUnit.valueOf(raw ?: WeightUnit.KG.name)
        } catch (_: Exception) {
            WeightUnit.KG
        }
}