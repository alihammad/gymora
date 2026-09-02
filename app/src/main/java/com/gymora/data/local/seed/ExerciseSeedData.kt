package com.gymora.data.local.seed

import com.gymora.domain.model.MuscleGroup

/**
 * Built-in exercise library seed data (FR-005, BR-20, SC-008, R-08).
 * Sourced from PRD-§6 lists, extended with common exercises to satisfy the
 * SC-008 contract (30+ exercises across 5 muscle groups).
 */
data class SeedExercise(
    val name: String,
    val muscleGroup: MuscleGroup,
)

object ExerciseSeedData {

    val EXERCISES: List<SeedExercise> = listOf(
        // Chest (7)
        SeedExercise("Barbell Bench Press", MuscleGroup.CHEST),
        SeedExercise("Dumbbell Bench Press", MuscleGroup.CHEST),
        SeedExercise("Incline Dumbbell Press", MuscleGroup.CHEST),
        SeedExercise("Chest Fly", MuscleGroup.CHEST),
        SeedExercise("Cable Fly", MuscleGroup.CHEST),
        SeedExercise("Dips", MuscleGroup.CHEST),
        SeedExercise("Push-Up", MuscleGroup.CHEST),
        // Back (6)
        SeedExercise("Deadlift", MuscleGroup.BACK),
        SeedExercise("Barbell Row", MuscleGroup.BACK),
        SeedExercise("Dumbbell Row", MuscleGroup.BACK),
        SeedExercise("Lat Pulldown", MuscleGroup.BACK),
        SeedExercise("Pull Up", MuscleGroup.BACK),
        SeedExercise("Seated Cable Row", MuscleGroup.BACK),
        // Shoulders (6)
        SeedExercise("Overhead Press", MuscleGroup.SHOULDERS),
        SeedExercise("Dumbbell Shoulder Press", MuscleGroup.SHOULDERS),
        SeedExercise("Lateral Raise", MuscleGroup.SHOULDERS),
        SeedExercise("Front Raise", MuscleGroup.SHOULDERS),
        SeedExercise("Rear Delt Fly", MuscleGroup.SHOULDERS),
        SeedExercise("Face Pull", MuscleGroup.SHOULDERS),
        // Arms (6)
        SeedExercise("Barbell Curl", MuscleGroup.ARMS),
        SeedExercise("Dumbbell Curl", MuscleGroup.ARMS),
        SeedExercise("Hammer Curl", MuscleGroup.ARMS),
        SeedExercise("Tricep Pushdown", MuscleGroup.ARMS),
        SeedExercise("Skull Crusher", MuscleGroup.ARMS),
        SeedExercise("Preacher Curl", MuscleGroup.ARMS),
        // Legs (7)
        SeedExercise("Squat", MuscleGroup.LEGS),
        SeedExercise("Leg Press", MuscleGroup.LEGS),
        SeedExercise("Leg Extension", MuscleGroup.LEGS),
        SeedExercise("Leg Curl", MuscleGroup.LEGS),
        SeedExercise("Romanian Deadlift", MuscleGroup.LEGS),
        SeedExercise("Calf Raise", MuscleGroup.LEGS),
        SeedExercise("Lunge", MuscleGroup.LEGS),
    )
}
