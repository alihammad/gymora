package com.gymora.data.local.seed

import androidx.room.withTransaction
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.entity.RoutineEntity
import com.gymora.data.local.entity.RoutineExerciseEntity
import com.gymora.data.local.entity.SetTemplateEntity
import com.gymora.domain.model.MeasurementType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * First-launch default routine seeding. On a fresh install the home screen should
 * already present a set of ready-to-use workout routines ("Legs", "Chest", "Back",
 * etc.), each composed of exercises picked from the bundled exercise library.
 *
 * This runs after [LibrarySeeder] so exercise names resolve to real ids. It is
 * idempotent: if any routine already exists (e.g. the user re-seeded or created
 * one), seeding is skipped.
 */
interface RoutineSeeder {
    suspend fun seed(database: GymoraDatabase)
}

@Singleton
class RoutineSeederImpl @Inject constructor() : RoutineSeeder {

    override suspend fun seed(database: GymoraDatabase) {
        val routineDao = database.routineDao()
        // Idempotency: never overwrite the user's routines or double-seed.
        if (routineDao.getAllOnce().isNotEmpty()) return

        val now = System.currentTimeMillis()
        database.withTransaction {
            DEFAULT_ROUTINES.forEachIndexed { index, routine ->
                val routineId = routineDao.insert(
                    RoutineEntity(
                        name = routine.name,
                        description = routine.description,
                        position = index,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                routine.exercises.forEachIndexed { exerciseIndex, exercise ->
                    val exerciseId = database.exerciseDao().getByName(exercise.name)?.id
                    // Skip silently if a library name ever drifts; never seed a broken FK.
                    if (exerciseId == null) return@forEachIndexed
                    val routineExerciseId = database.routineExerciseDao().insert(
                        RoutineExerciseEntity(
                            routineId = routineId,
                            exerciseId = exerciseId,
                            position = exerciseIndex,
                            notes = null,
                        ),
                    )
                    repeat(DEFAULT_SET_COUNT) { setIndex ->
                        database.setTemplateDao().insert(
                            SetTemplateEntity(
                                routineExerciseId = routineExerciseId,
                                setNumber = setIndex + 1,
                                targetReps = exercise.targetReps,
                                targetWeight = null,
                                targetWeightUnit = null,
                                measurementType = exercise.measurementType.name,
                            ),
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_SET_COUNT = 3

        data class SeedExercise(
            val name: String,
            val targetReps: Int,
            val measurementType: MeasurementType,
        )

        data class SeedRoutine(
            val name: String,
            val description: String?,
            val exercises: List<SeedExercise>,
        )

        /** Weighted compound lifts — 3 sets of 8, weight-based intent. */
        fun weighted(name: String, reps: Int = 8) =
            SeedExercise(name, reps, MeasurementType.WEIGHT_AND_REPS)

        /** Bodyweight / machine-isolation movements — 3 sets of 12, reps-only intent. */
        fun reps(name: String, reps: Int = 12) =
            SeedExercise(name, reps, MeasurementType.REPS_ONLY)

        val DEFAULT_ROUTINES = listOf(
            SeedRoutine(
                name = "Legs",
                description = "Complete lower-body day targeting quads, hamstrings and calves.",
                exercises = listOf(
                    weighted("Barbell Squat"),
                    weighted("Leg Press", 10),
                    reps("Leg Extensions", 15),
                    weighted("Romanian Deadlift", 10),
                    reps("Lying Leg Curls"),
                    reps("Standing Calf Raises", 15),
                    reps("Dumbbell Lunges", 12),
                ),
            ),
            SeedRoutine(
                name = "Chest",
                description = "Upper, middle and lower chest with a strong emphasis on pressing.",
                exercises = listOf(
                    weighted("Barbell Bench Press"),
                    weighted("Incline Dumbbell Press", 10),
                    weighted("Dumbbell Flyes", 12),
                    weighted("Decline Barbell Bench Press", 10),
                    reps("Machine Pec Fly", 15),
                    reps("Cable Crossover", 15),
                    reps("Dips - Chest Version"),
                ),
            ),
            SeedRoutine(
                name = "Back",
                description = "Thick, wide back built around deadlifts, pulls and rows.",
                exercises = listOf(
                    weighted("Barbell Deadlift", 5),
                    reps("Pull Up", 8),
                    reps("Wide-Grip Lat Pulldown", 10),
                    weighted("Bent Over Barbell Row"),
                    reps("Seated Cable Rows", 10),
                    weighted("One-Arm Dumbbell Row", 10),
                    reps("Straight-Arm Pulldown", 15),
                ),
            ),
            SeedRoutine(
                name = "Shoulders",
                description = "Well-rounded delts with presses, raises and rear-delt work.",
                exercises = listOf(
                    weighted("Seated Barbell Military Press"),
                    weighted("Dumbbell Shoulder Press", 10),
                    reps("Side Lateral Raise", 15),
                    reps("Front Dumbbell Raise", 12),
                    reps("Dumbbell Reverse Fly", 15),
                    reps("Face Pull", 15),
                    weighted("Barbell Shrug", 10),
                ),
            ),
            SeedRoutine(
                name = "Arms",
                description = "Biceps and triceps superset-style pump for bigger arms.",
                exercises = listOf(
                    weighted("Barbell Curl", 10),
                    weighted("Hammer Curls", 10),
                    reps("Preacher Curl", 12),
                    weighted("Close-Grip Barbell Bench Press", 10),
                    reps("Triceps Pushdown"),
                    weighted("EZ-Bar Skullcrusher", 10),
                    reps("Triceps Overhead Extension with Rope", 12),
                ),
            ),
            SeedRoutine(
                name = "Push Day",
                description = "Chest, shoulders and triceps in one pressing session.",
                exercises = listOf(
                    weighted("Barbell Bench Press"),
                    weighted("Incline Dumbbell Press", 10),
                    weighted("Dumbbell Shoulder Press", 10),
                    reps("Side Lateral Raise", 15),
                    reps("Triceps Pushdown"),
                    weighted("Close-Grip Barbell Bench Press", 10),
                ),
            ),
            SeedRoutine(
                name = "Pull Day",
                description = "Back, rear delts and biceps in one pulling session.",
                exercises = listOf(
                    weighted("Barbell Deadlift", 5),
                    reps("Pull Up", 8),
                    weighted("Bent Over Barbell Row"),
                    reps("Seated Cable Rows", 10),
                    reps("Face Pull", 15),
                    weighted("Barbell Curl", 10),
                ),
            ),
            SeedRoutine(
                name = "Full Body",
                description = "Efficient compound-only session hitting every major muscle group.",
                exercises = listOf(
                    weighted("Barbell Squat"),
                    weighted("Barbell Bench Press"),
                    weighted("Barbell Deadlift", 5),
                    reps("Pull Up", 8),
                    weighted("Dumbbell Shoulder Press", 10),
                    weighted("Barbell Curl", 10),
                ),
            ),
            SeedRoutine(
                name = "Core & Abs",
                description = "Strengthen the midline with planks, crunches and rotations.",
                exercises = listOf(
                    reps("Plank", 60),
                    reps("Hanging Leg Raise", 15),
                    reps("Cable Crunch", 15),
                    reps("Air Bike", 20),
                    reps("Ab Roller", 15),
                    reps("Russian Twist", 20),
                ),
            ),
            SeedRoutine(
                name = "Glutes & Hamstrings",
                description = "Posterior-chain focus for stronger glutes and hamstrings.",
                exercises = listOf(
                    weighted("Barbell Hip Thrust", 10),
                    weighted("Romanian Deadlift", 10),
                    reps("Lying Leg Curls"),
                    reps("Glute Kickback", 15),
                    weighted("Barbell Glute Bridge", 12),
                    reps("Cable Hip Extension", 15),
                ),
            ),
        )
    }
}
