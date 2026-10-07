package com.gymora.domain.model

import java.time.Instant

/**
 * Active-workout models (FR-019..FR-039). Pure-Kotlin mirrors of the Room
 * historical entities without persistence annotations.
 */
data class WorkoutSession(
    val id: Long,
    val routineId: Long?,
    val routineNameSnapshot: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val status: SessionStatus,
    val notes: String?,
)

data class ActiveSet(
    val id: Long,
    val setNumber: Int,
    val reps: Int?,
    val weight: Double?,
    val weightUnit: WeightUnit?,
    val measurementType: MeasurementType,
    val isCompleted: Boolean,
    val completedAt: Instant?,
    val notes: String?,
    val durationSeconds: Int? = null,
    /** Stored in metres; shown in km or miles. */
    val distanceMeters: Double? = null,
    /** Set for unilateral exercises: which side this set was. */
    val side: Side? = null,
)

data class ActiveExercise(
    val workoutExerciseId: Long,
    val exerciseId: Long?,
    val exerciseName: String,
    val position: Int,
    val notes: String?,
    val sets: List<ActiveSet>,
    /** Most recent performance for pre-fill (FR-045) — populated in US6. */
    val previousPerformance: List<SetValue>? = null,
    /** Adjacent exercises sharing a non-null group form a superset. */
    val supersetGroup: Long? = null,
    /** Form cues and demo media of the library exercise, shown while training. */
    val formCues: List<String> = emptyList(),
    val mediaFile: String? = null,
)

data class ActiveWorkout(
    val session: WorkoutSession,
    val exercises: List<ActiveExercise>,
    val startedAt: Instant,
)

/** A single set's entered values. */
data class SetValue(
    val weight: Double?,
    val weightUnit: WeightUnit?,
    val reps: Int?,
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null,
    val side: Side? = null,
)

/** Summary returned by finish (FR-035). */
data class WorkoutSummary(
    val sessionId: Long,
    val routineNameSnapshot: String,
    val date: Instant,
    val duration: java.time.Duration,
    val exerciseCount: Int,
    val completedSetCount: Int,
    val totalReps: Int,
    val totalVolume: Double,
    val perExerciseBreakdown: List<ExerciseSummary>,
)

data class ExerciseSummary(
    val exerciseName: String,
    val completedSetCount: Int,
)

/** Read-only historical workout graph (FR-041). */
data class WorkoutDetail(
    val session: WorkoutSession,
    val exercises: List<ActiveExercise>,
)

/** History list entry (FR-040). */
data class HistoryEntry(
    val id: Long,
    val routineNameSnapshot: String,
    val startedAt: Instant,
    val duration: java.time.Duration,
)

/** One performance of an exercise (FR-044). */
data class ExercisePerformance(
    val sessionId: Long,
    val date: Instant,
    val sets: List<ActiveSet>,
)

/** Most recent performance of an exercise for pre-fill (FR-045). */
data class PreviousPerformance(
    val exerciseId: Long,
    val date: Instant,
    val sets: List<SetValue>,
)

/** One completed session of a routine, for the routine progress chart. */
data class RoutineSessionPoint(
    val sessionId: Long,
    val date: Instant,
    val volumeKg: Double,
    val completedSets: Int,
)

/** Per-session values for one exercise or routine, oldest first (progress overview). */
data class ProgressSeries(
    val id: Long,
    val name: String,
    val values: List<Double>,
)

/** One completed workout's headline numbers, for the profile charts and calendar. */
data class WorkoutStat(
    val sessionId: Long,
    val routineName: String,
    val startedAt: Instant,
    val duration: java.time.Duration,
    val volumeKg: Double,
    val totalReps: Int,
)
