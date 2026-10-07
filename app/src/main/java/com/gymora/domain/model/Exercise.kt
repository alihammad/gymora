package com.gymora.domain.model

/**
 * Exercise library entry (FR-005..FR-010). Pure-Kotlin mirror of the Room
 * entity without persistence annotations (plan.md Structure Decision).
 *
 * The library carries the full descriptive metadata from docs/exercises.json
 * so the detail screen can render instructions, muscle groups, equipment,
 * and classification attributes aesthetically.
 */
data class Exercise(
    val id: Long,
    val name: String,
    val muscleGroup: MuscleGroup?,
    val description: String?,
    val notes: String?,
    val isCustom: Boolean,
    val isDeleted: Boolean,
    val type: ExerciseType? = null,
    val difficultyLevel: DifficultyLevel? = null,
    val forceType: ForceType? = null,
    val mechanics: Mechanics? = null,
    val category: ExerciseCategory? = null,
    val instructions: List<String> = emptyList(),
    val muscleGroups: List<MuscleGroupRef> = emptyList(),
    val equipment: List<EquipmentItem> = emptyList(),
    /** What new sets of this exercise record. */
    val measurementType: MeasurementType = MeasurementType.WEIGHT_AND_REPS,
    /** Left and right sides are logged as separate sets. */
    val isUnilateral: Boolean = false,
    /** Short coaching reminders shown while training. */
    val formCues: List<String> = emptyList(),
    /** File name of a demo image or GIF inside the app's exercise media folder. */
    val mediaFile: String? = null,
)

/** A muscle group's role within an exercise (primary/secondary mover). */
data class MuscleGroupRef(
    val group: MuscleGroup,
    val type: MuscleGroupType,
)

/** A piece of equipment used by an exercise (weight/resistance, usage mode). */
data class EquipmentItem(
    val name: String,
    val type: EquipmentType,
    val usageType: EquipmentUsageType,
)

/** Input for creating a custom exercise (FR-006). */
data class CreateExerciseInput(
    val name: String,
    val muscleGroup: MuscleGroup? = null,
    val description: String? = null,
    val notes: String? = null,
    val measurementType: MeasurementType = MeasurementType.WEIGHT_AND_REPS,
    val isUnilateral: Boolean = false,
    val formCues: List<String> = emptyList(),
    val mediaFile: String? = null,
)

/** Input for editing an exercise (FR-007). */
data class UpdateExerciseInput(
    val name: String,
    val muscleGroup: MuscleGroup? = null,
    val description: String? = null,
    val notes: String? = null,
    val measurementType: MeasurementType = MeasurementType.WEIGHT_AND_REPS,
    val isUnilateral: Boolean = false,
    val formCues: List<String> = emptyList(),
    val mediaFile: String? = null,
)

/**
 * Best-guess tracking defaults for library exercises, from name and category.
 * Users can change both per exercise.
 */
object ExerciseTrackingDefaults {

    private val durationWords = listOf("plank", "wall sit", "isometric", " hold", "dead hang")
    private val carryWords = listOf("carry", "farmer's walk", "farmers walk", "yoke", "sled")
    private val bodyweightLoadable = listOf("pull-up", "pullup", "chin-up", "chinup", "dip")
    private val unilateralWords = listOf(
        "one-arm", "one arm", "single-arm", "single arm", "one-leg", "one leg",
        "single-leg", "single leg", "unilateral",
    )

    fun measurementType(name: String, category: ExerciseCategory?, equipment: List<String>): MeasurementType {
        val n = name.lowercase()
        val bodyOnly = equipment.isNotEmpty() && equipment.all { it.equals("Body Only", ignoreCase = true) }
        return when {
            category == ExerciseCategory.CARDIO -> MeasurementType.DISTANCE_AND_DURATION
            category == ExerciseCategory.STRETCHING -> MeasurementType.DURATION
            durationWords.any { it in n } -> MeasurementType.DURATION
            carryWords.any { it in n } -> MeasurementType.WEIGHT_AND_DISTANCE
            "assisted" in n -> MeasurementType.ASSISTED_BODYWEIGHT
            "machine" !in n && bodyweightLoadable.any { it in n } -> MeasurementType.WEIGHTED_BODYWEIGHT
            bodyOnly -> MeasurementType.REPS_ONLY
            else -> MeasurementType.WEIGHT_AND_REPS
        }
    }

    fun isUnilateral(name: String): Boolean = name.lowercase().let { n -> unilateralWords.any { it in n } }
}

/**
 * Exercise input validation rules (FR-006, data-model.md validation).
 * Muscle group is restricted to the enum set by type (Enums.kt).
 */
object ExerciseValidation {
    fun validate(input: CreateExerciseInput) {
        if (input.name.isBlank()) {
            throw ValidationException("name", "Exercise name must not be blank")
        }
    }

    fun validate(input: UpdateExerciseInput) {
        if (input.name.isBlank()) {
            throw ValidationException("name", "Exercise name must not be blank")
        }
    }
}
