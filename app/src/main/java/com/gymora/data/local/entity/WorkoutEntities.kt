package com.gymora.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One gym session — historical record (FR-019, FR-034, FR-056, data-model.md).
 * The partial UNIQUE index on status='ACTIVE' enforces at most one active
 * session at the persistence layer (BR-14, Constitution V).
 */
@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["status"]),
        Index(value = ["started_at"]),
        Index(value = ["routine_id"]),
    ],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "routine_id") val routineId: Long?,
    @ColumnInfo(name = "routine_name_snapshot") val routineNameSnapshot: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** Exercise as performed in a session (FR-055, FR-056, data-model.md). */
@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["session_id", "position"], unique = true),
        Index(value = ["exercise_id"]),
    ],
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long?,
    @ColumnInfo(name = "exercise_name_snapshot") val exerciseNameSnapshot: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "notes") val notes: String?,
    /** Adjacent rows sharing a non-null group form a superset (v3). */
    @ColumnInfo(name = "superset_group") val supersetGroup: Long? = null,
)

/** Set as actually performed (FR-023..FR-025, data-model.md). */
@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["workout_exercise_id", "set_number"], unique = true),
    ],
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "workout_exercise_id") val workoutExerciseId: Long,
    @ColumnInfo(name = "set_number") val setNumber: Int,
    @ColumnInfo(name = "reps") val reps: Int?,
    @ColumnInfo(name = "weight") val weight: Double?,
    @ColumnInfo(name = "weight_unit") val weightUnit: String?,
    @ColumnInfo(name = "measurement_type") val measurementType: String,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean,
    @ColumnInfo(name = "completed_at") val completedAt: Long?,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int? = null,
    @ColumnInfo(name = "distance_m") val distanceMeters: Double? = null,
    /** LEFT / RIGHT for unilateral exercises, else null (v5). */
    @ColumnInfo(name = "side") val side: String? = null,
)
