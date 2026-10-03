package com.gymora.domain.model

/**
 * Routine template models (FR-011..FR-018). Pure-Kotlin mirrors of the Room
 * entities without persistence annotations (plan.md Structure Decision).
 */
data class RoutineSummary(
    val id: Long,
    val name: String,
    val exerciseCount: Int,
    val lastPerformedAt: Long?,
)

data class RoutineHeader(
    val id: Long,
    val name: String,
    val description: String?,
    val position: Int,
)

data class SetTemplate(
    val id: Long,
    val setNumber: Int,
    val targetReps: Int,
    val targetWeight: Double?,
    val weightUnit: WeightUnit?,
    val measurementType: MeasurementType,
)

data class SetTemplateInput(
    val targetReps: Int,
    val targetWeight: Double?,
    val weightUnit: WeightUnit?,
    val measurementType: MeasurementType,
)

data class RoutineExerciseDetail(
    val routineExerciseId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val position: Int,
    val notes: String?,
    val setTemplates: List<SetTemplate>,
)

data class RoutineDetail(
    val header: RoutineHeader,
    val exercises: List<RoutineExerciseDetail>,
)

/**
 * Routine business rules (FR-011, FR-014, R-10). Single implementation of the
 * duplicate-naming rule (Constitution IX).
 */
object RoutineRules {

    fun validateName(name: String) {
        if (name.isBlank()) {
            throw ValidationException("name", "Workout name must not be blank")
        }
    }

    /** Derived duplicate name, e.g. "Chest Workout" → "Chest Workout Copy" (R-10). */
    fun duplicateName(name: String): String = "$name Copy"
}
