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

/** Muscle group grouping for the exercise library (FR-005). */
enum class MuscleGroup {
    CHEST,
    BACK,
    SHOULDERS,
    ARMS,
    LEGS,
}

/**
 * Workout session lifecycle status (R-12). Discarded sessions are deleted permanently
 * (BR-15) and never appear as a stored status.
 */
enum class SessionStatus {
    ACTIVE,
    COMPLETED,
}
