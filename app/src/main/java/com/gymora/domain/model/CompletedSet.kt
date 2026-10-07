package com.gymora.domain.model

/** A performed set used for volume calculation (BR-12). */
data class CompletedSet(
    val weight: Double?,
    val reps: Int?,
    val weightUnit: WeightUnit?,
    val isCompleted: Boolean,
    /** Null means a plain weighted set; assistance never counts as load. */
    val measurementType: MeasurementType? = null,
)
