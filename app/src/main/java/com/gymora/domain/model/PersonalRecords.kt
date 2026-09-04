package com.gymora.domain.model

import java.time.Instant

/**
 * A single personal record with exercise and date context (FR-047).
 */
data class PersonalRecord(
    val value: Double,
    val exerciseName: String,
    val date: Instant,
)

/**
 * All personal records computed from completed sessions (FR-047, R-09).
 * Each record carries the exercise name and date when it was achieved.
 */
data class PersonalRecords(
    val heaviestWeight: PersonalRecord?,
    val highestReps: PersonalRecord?,
    val bestEstimatedOneRepMax: PersonalRecord?,
    val largestWorkoutVolume: PersonalRecord?,
)
