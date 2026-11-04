package com.gymora.domain.model

/**
 * Starting values for new sets. A newly added exercise's first set repeats the
 * last set logged for it; never-logged exercises get fixed defaults. Later sets
 * copy the set before them (done by the callers).
 */
object SetDefaults {
    const val REPS = 10
    const val DURATION_SECONDS = 60

    /** An empty barbell in the user's unit. */
    fun weight(unit: WeightUnit): Double = if (unit == WeightUnit.LB) 45.0 else 20.0

    /** First set on [side]: the last logged set of that side if any, else defaults for [type]. */
    fun firstSet(
        type: MeasurementType,
        previous: PreviousPerformance?,
        side: Side?,
        unit: WeightUnit,
    ): SetValue {
        val last = previous?.sets?.lastOrNull { it.side == side } ?: previous?.sets?.lastOrNull()
        if (last != null) return last.copy(side = side)
        // Added/assist weight starts at zero; only a lifted load gets a default.
        val loaded = type == MeasurementType.WEIGHT_AND_REPS || type == MeasurementType.WEIGHT_AND_DISTANCE
        return SetValue(
            weight = if (loaded) weight(unit) else null,
            weightUnit = if (loaded) unit else null,
            reps = if (SetField.REPS in type.fields) REPS else null,
            durationSeconds = if (type == MeasurementType.DURATION) DURATION_SECONDS else null,
            distanceMeters = null,
            side = side,
        )
    }
}
