package com.gymora.domain.model

/** Weight unit for entry and display (FR-048). Stored values keep their own unit (R-04). */
enum class WeightUnit {
    KG,
    LB,
}

/** Theme preference (FR-051). */
enum class Theme {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Set measurement model (FR-030, BR-17). v1 supports WEIGHT_AND_REPS and REPS_ONLY;
 * DURATION and DISTANCE can be added later as new values without a data redesign (R-11).
 */
enum class MeasurementType {
    WEIGHT_AND_REPS,
    REPS_ONLY,
}

/**
 * Muscle group grouping for the exercise library (FR-005).
 * Expanded to the 17 groups used by the built-in library seed (docs/exercises.json).
 */
enum class MuscleGroup(val displayName: String) {
    ABDOMINALS("Abdominals"),
    ABDUCTORS("Abductors"),
    ADDUCTORS("Adductors"),
    BICEPS("Biceps"),
    CALVES("Calves"),
    CHEST("Chest"),
    FOREARMS("Forearms"),
    GLUTES("Glutes"),
    HAMSTRINGS("Hamstrings"),
    LATS("Lats"),
    LOWER_BACK("Lower Back"),
    MIDDLE_BACK("Middle Back"),
    NECK("Neck"),
    QUADRICEPS("Quadriceps"),
    SHOULDERS("Shoulders"),
    TRAPS("Traps"),
    TRICEPS("Triceps"),
    ;

    companion object {
        /** Maps docs/exercises.json slugs (e.g. "lower-back") to enum values. */
        fun fromSlug(slug: String): MuscleGroup = valueOf(slug.replace("-", "_").uppercase())
    }
}

/** How an exercise is counted (docs/exercises.json `type`). */
enum class ExerciseType {
    REPS,
}

/** Exercise difficulty (docs/exercises.json `difficultyLevel`). */
enum class DifficultyLevel(val displayName: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    EXPERT("Expert"),
}

/** Direction of force (docs/exercises.json `forceType`). */
enum class ForceType(val displayName: String) {
    PUSH("Push"),
    PULL("Pull"),
    STATIC("Static"),
}

/** Movement pattern (docs/exercises.json `mechanics`). */
enum class Mechanics(val displayName: String) {
    COMPOUND("Compound"),
    ISOLATION("Isolation"),
}

/** Exercise classification (docs/exercises.json `category`). */
enum class ExerciseCategory(val displayName: String) {
    STRENGTH("Strength"),
    STRETCHING("Stretching"),
    PLYOMETRICS("Plyometrics"),
    POWERLIFTING("Powerlifting"),
    OLYMPIC_WEIGHT_LIFTING("Olympic Weightlifting"),
    STRONGMAN("Strongman"),
    CARDIO("Cardio"),
    ;

    companion object {
        /** Maps docs/exercises.json category slugs (e.g. "olympicWeightlifting"). */
        fun fromSlug(slug: String): ExerciseCategory = when (slug) {
            "strength" -> STRENGTH
            "stretching" -> STRETCHING
            "plyometrics" -> PLYOMETRICS
            "powerlifting" -> POWERLIFTING
            "olympicWeightlifting" -> OLYMPIC_WEIGHT_LIFTING
            "strongman" -> STRONGMAN
            "cardio" -> CARDIO
            else -> error("Unknown exercise category: $slug")
        }
    }
}

/** Role of a muscle group in an exercise (primary vs. secondary mover). */
enum class MuscleGroupType {
    PRIMARY,
    SECONDARY,
}

/** Equipment resistance kind (docs/exercises.json equipment `type`). */
enum class EquipmentType {
    WEIGHT,
    RESISTANCE,
}

/** Equipment hand/load usage (docs/exercises.json equipment `usageType`). */
enum class EquipmentUsageType {
    SINGLE,
    DOUBLE,
    MULTIPLE,
}

/**
 * Workout session lifecycle status (R-12). Discarded sessions are deleted permanently
 * (BR-15) and never appear as a stored status.
 */
enum class SessionStatus {
    ACTIVE,
    COMPLETED,
}
