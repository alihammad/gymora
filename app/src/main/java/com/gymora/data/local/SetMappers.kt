package com.gymora.data.local

import com.gymora.data.local.entity.SetTemplateEntity
import com.gymora.data.local.entity.WorkoutSetEntity
import com.gymora.domain.model.ActiveSet
import com.gymora.domain.model.CompletedSet
import com.gymora.domain.model.MeasurementType
import com.gymora.domain.model.SetTemplate
import com.gymora.domain.model.Side
import com.gymora.domain.model.WeightUnit
import java.time.Instant

/** Entity → domain mapping for sets, shared by every repository that reads them. */

fun WorkoutSetEntity.toDomain(): ActiveSet = ActiveSet(
    id = id,
    setNumber = setNumber,
    reps = reps,
    weight = weight,
    weightUnit = weightUnit?.let { WeightUnit.valueOf(it) },
    measurementType = parseMeasurementType(measurementType),
    isCompleted = isCompleted,
    completedAt = completedAt?.let { Instant.ofEpochMilli(it) },
    notes = notes,
    durationSeconds = durationSeconds,
    distanceMeters = distanceMeters,
    side = side?.let { raw -> Side.entries.firstOrNull { it.name == raw } },
)

/** Input for the volume calculator (BR-12). */
fun WorkoutSetEntity.toCompletedSet(): CompletedSet = CompletedSet(
    weight = weight,
    reps = reps,
    weightUnit = weightUnit?.let { WeightUnit.valueOf(it) },
    isCompleted = isCompleted,
    measurementType = parseMeasurementType(measurementType),
)

/** Whether this set's weight is load the lifter moved (assistance is not). */
val WorkoutSetEntity.weightIsLoad: Boolean
    get() = parseMeasurementType(measurementType).countsWeightAsLoad

fun SetTemplateEntity.toDomain(): SetTemplate = SetTemplate(
    id = id,
    setNumber = setNumber,
    targetReps = targetReps,
    targetWeight = targetWeight,
    weightUnit = targetWeightUnit?.let { WeightUnit.valueOf(it) },
    measurementType = parseMeasurementType(measurementType),
    targetDurationSeconds = targetDurationSeconds,
    targetDistanceMeters = targetDistanceMeters,
)

/** Unknown names (e.g. from a newer backup) fall back to weight & reps. */
fun parseMeasurementType(raw: String?): MeasurementType =
    MeasurementType.entries.firstOrNull { it.name == raw } ?: MeasurementType.WEIGHT_AND_REPS
