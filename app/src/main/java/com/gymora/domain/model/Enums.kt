package com.gymora.domain.model

/** Weight unit for entry and display (FR-048). Stored values keep their own unit (R-04). */
enum class WeightUnit {
    KG,
    LB,
}

/**
 * Theme preference (FR-051). Persisted by [name], so existing entries must keep their names.
 * [LIGHT] is the Light Minimal theme and [DARK] the original Night Volt theme.
 */
enum class Theme(val displayName: String) {
    SYSTEM("System default"),
    LIGHT("Light Minimal"),
    DARK("Night Volt"),
    OBSIDIAN("Obsidian"),
    GRAPHITE_LIME("Graphite + Lime"),
    MIDNIGHT_BLUE("Midnight Blue"),
    BLACK_PURPLE("Black + Purple"),
    CHARCOAL_ORANGE("Charcoal + Orange"),
    DEEP_FOREST("Deep Forest"),
}

/** A value a set can record; which ones apply depends on the [MeasurementType]. */
enum class SetField {
    WEIGHT,
    REPS,
    DURATION,
    DISTANCE,
}

/**
 * Set measurement model (FR-030, BR-17, R-11). Persisted by [name], so existing
 * entries must keep their names. An exercise has a default type; each performed
 * set stores the type it was logged with, so history renders as recorded.
 */
enum class MeasurementType(
    val displayName: String,
    val example: String,
    val fields: List<SetField>,
    /** Label for the weight field when it is not the lifted load. */
    val weightLabel: String = "Weight",
) {
    WEIGHT_AND_REPS("Weight & reps", "Bench press, squat", listOf(SetField.WEIGHT, SetField.REPS)),
    REPS_ONLY("Reps only", "Push-ups, crunches", listOf(SetField.REPS)),
    WEIGHTED_BODYWEIGHT(
        "Bodyweight + added weight", "Weighted dips, pull-ups",
        listOf(SetField.WEIGHT, SetField.REPS), weightLabel = "Added",
    ),
    ASSISTED_BODYWEIGHT(
        "Assisted bodyweight", "Assisted pull-ups, machine dips",
        listOf(SetField.WEIGHT, SetField.REPS), weightLabel = "Assist",
    ),
    DURATION("Time", "Planks, holds, stretches", listOf(SetField.DURATION)),
    DISTANCE_AND_DURATION(
        "Distance & time", "Running, rowing, cycling",
        listOf(SetField.DISTANCE, SetField.DURATION),
    ),
    WEIGHT_AND_DISTANCE("Weight & distance", "Farmer's carry, sled push", listOf(SetField.WEIGHT, SetField.DISTANCE)),
    ;

    /**
     * Whether the weight field is load the lifter moved. Assistance is not: it is
     * excluded from volume, heaviest-weight and estimated 1RM.
     */
    val countsWeightAsLoad: Boolean get() = this != ASSISTED_BODYWEIGHT

    fun has(field: SetField): Boolean = field in fields
}

/** Side of the body a unilateral set was performed with. Persisted by [name]. */
enum class Side(val shortLabel: String) {
    LEFT("L"),
    RIGHT("R"),
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
