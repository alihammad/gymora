package com.gymora.domain.model

import java.time.Instant

/**
 * A single personal record with exercise and date context (FR-047).
 */
data class PersonalRecord(
    val value: Double,
    val exerciseName: String,
    val date: Instant,
    val sessionId: Long,
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

/** Which kind of personal best a [SessionRecord] beat. */
enum class SessionRecordKind(val label: String) {
    HEAVIEST_WEIGHT("Heaviest weight"),
    BEST_ESTIMATED_ONE_REP_MAX("Best est. 1RM"),
    MOST_REPS("Most reps"),
}

/**
 * A personal best set in one workout, beating every earlier completed workout
 * of the same exercise. [value] is kg for weight kinds, a rep count for [SessionRecordKind.MOST_REPS].
 */
data class SessionRecord(
    val exerciseName: String,
    val kind: SessionRecordKind,
    val value: Double,
)
