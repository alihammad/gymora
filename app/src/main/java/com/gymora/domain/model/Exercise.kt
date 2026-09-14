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
)

/** Input for editing an exercise (FR-007). */
data class UpdateExerciseInput(
    val name: String,
    val muscleGroup: MuscleGroup? = null,
    val description: String? = null,
    val notes: String? = null,
)

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
