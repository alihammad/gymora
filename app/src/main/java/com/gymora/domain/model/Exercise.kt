package com.gymora.domain.model

/**
 * Exercise library entry (FR-005..FR-010). Pure-Kotlin mirror of the Room
 * entity without persistence annotations (plan.md Structure Decision).
 */
data class Exercise(
    val id: Long,
    val name: String,
    val muscleGroup: MuscleGroup?,
    val description: String?,
    val notes: String?,
    val isCustom: Boolean,
    val isDeleted: Boolean,
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
