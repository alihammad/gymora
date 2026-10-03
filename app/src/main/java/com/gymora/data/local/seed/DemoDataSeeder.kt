package com.gymora.data.local.seed

import androidx.room.withTransaction
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.entity.WorkoutExerciseEntity
import com.gymora.data.local.entity.WorkoutSessionEntity
import com.gymora.data.local.entity.WorkoutSetEntity
import com.gymora.domain.model.MeasurementType
import com.gymora.domain.model.SessionStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * TEMPORARY test data: 50 completed workouts with varied durations spread over the
 * last ~150 days, so history, charts, calendar and progress screens can be exercised.
 *
 * Every demo session is tagged with [DEMO_TAG] in `notes`, so [purge] removes exactly
 * what [seed] added (exercise/set rows go via ON DELETE CASCADE).
 */
@Singleton
class DemoDataSeeder @Inject constructor(private val database: GymoraDatabase) {

    suspend fun seed() {
        val sessionDao = database.workoutSessionDao()
        if (sessionDao.listCompleted(Int.MAX_VALUE, 0).any { it.notes == DEMO_TAG }) return

        val routines = database.routineDao().getAllOnce()
        val rng = Random(42)
        val dayMs = 24L * 60 * 60 * 1000
        val now = System.currentTimeMillis()

        database.withTransaction {
            repeat(SESSION_COUNT) { i ->
                val template = TEMPLATES[i % TEMPLATES.size]
                val routine = routines.firstOrNull { it.name == template.name }
                // Oldest first: i = 0 is ~150 days ago, i = 49 is yesterday.
                val daysAgo = ((SESSION_COUNT - i) * 3).coerceAtLeast(1) + rng.nextInt(0, 2)
                val startedAt = now - daysAgo * dayMs +
                    (17 * 60 + rng.nextInt(0, 120)) * 60_000L - (now % dayMs)
                val durationMin = DURATIONS[i % DURATIONS.size]
                val endedAt = startedAt + durationMin * 60_000L
                val progress = i / (SESSION_COUNT - 1.0) // 0..1 strength progression

                val sessionId = sessionDao.insert(
                    WorkoutSessionEntity(
                        routineId = routine?.id,
                        routineNameSnapshot = template.name,
                        startedAt = startedAt,
                        endedAt = endedAt,
                        status = SessionStatus.COMPLETED.name,
                        notes = DEMO_TAG,
                        createdAt = startedAt,
                    ),
                )

                var position = 0
                template.exercises.forEach { (name, baseWeight, reps) ->
                    val exercise = database.exerciseDao().getByName(name) ?: return@forEach
                    val weId = database.workoutExerciseDao().insert(
                        WorkoutExerciseEntity(
                            sessionId = sessionId,
                            exerciseId = exercise.id,
                            exerciseNameSnapshot = exercise.name,
                            position = position++,
                            notes = null,
                        ),
                    )
                    val weighted = baseWeight != null
                    val minutesPerSet = durationMin.toDouble() / (template.exercises.size * SETS)
                    repeat(SETS) { s ->
                        val weight = baseWeight?.let {
                            val w = it * (1 + 0.25 * progress) + rng.nextInt(-1, 2) * 2.5
                            Math.round(w / 2.5) * 2.5
                        }
                        database.workoutSetDao().insert(
                            WorkoutSetEntity(
                                workoutExerciseId = weId,
                                setNumber = s + 1,
                                reps = reps + rng.nextInt(-2, 3),
                                weight = weight,
                                weightUnit = if (weighted) "KG" else null,
                                measurementType = (if (weighted) MeasurementType.WEIGHT_AND_REPS
                                else MeasurementType.REPS_ONLY).name,
                                isCompleted = true,
                                completedAt = startedAt +
                                    (minutesPerSet * ((position - 1) * SETS + s + 1) * 60_000L).toLong(),
                                notes = null,
                            ),
                        )
                    }
                }
            }
        }
    }

    /** Deletes every session [seed] created (children cascade). */
    suspend fun purge() {
        database.openHelper.writableDatabase
            .execSQL("DELETE FROM workout_sessions WHERE notes = '$DEMO_TAG'")
    }

    private data class DemoExercise(val name: String, val baseWeightKg: Double?, val reps: Int)

    private class DemoTemplate(val name: String, val exercises: List<DemoExercise>)

    private companion object {
        const val DEMO_TAG = "[DEMO_SEED]"
        const val SESSION_COUNT = 50
        const val SETS = 3

        /** Minutes; cycled so completion times range from a quick 22 min to a long 112 min. */
        val DURATIONS = listOf(
            45, 62, 28, 75, 38, 90, 52, 22, 68, 105, 33, 58, 80, 41, 112,
            47, 70, 25, 63, 95, 36, 55, 85, 30, 72,
        )

        private fun w(name: String, kg: Number, reps: Int = 8) =
            DemoExercise(name, kg.toDouble(), reps)
        private fun b(name: String, reps: Int = 12) = DemoExercise(name, null, reps)

        val TEMPLATES = listOf(
            DemoTemplate("Legs", listOf(
                w("Barbell Squat", 80), w("Leg Press", 140, 10), b("Leg Extensions", 15),
                w("Romanian Deadlift", 70, 10), b("Standing Calf Raises", 15),
            )),
            DemoTemplate("Chest", listOf(
                w("Barbell Bench Press", 60), w("Incline Dumbbell Press", 24, 10),
                w("Dumbbell Flyes", 14, 12), b("Cable Crossover", 15),
            )),
            DemoTemplate("Back", listOf(
                w("Barbell Deadlift", 100, 5), b("Pull Up", 8), w("Bent Over Barbell Row", 60),
                b("Seated Cable Rows", 10),
            )),
            DemoTemplate("Shoulders", listOf(
                w("Seated Barbell Military Press", 40), w("Dumbbell Shoulder Press", 20, 10),
                b("Side Lateral Raise", 15), b("Face Pull", 15),
            )),
            DemoTemplate("Arms", listOf(
                w("Barbell Curl", 30, 10), w("Hammer Curls", 14, 10),
                w("Close-Grip Barbell Bench Press", 50, 10), b("Triceps Pushdown"),
            )),
            DemoTemplate("Full Body", listOf(
                w("Barbell Squat", 75), w("Barbell Bench Press", 57.5), w("Barbell Deadlift", 95, 5),
                b("Pull Up", 8),
            )),
            DemoTemplate("Core & Abs", listOf(
                b("Hanging Leg Raise", 15), b("Cable Crunch", 15), b("Ab Roller", 15),
            )),
        )
    }
}
