package com.gymora.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Routine template header (FR-011..FR-015, data-model.md). */
@Entity(
    tableName = "routines",
    indices = [Index(value = ["position"])],
)
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** Ordered routine↔exercise link (FR-016, FR-017, data-model.md). */
@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
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
        Index(value = ["routine_id", "position"], unique = true),
        Index(value = ["exercise_id"]),
    ],
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "routine_id") val routineId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "notes") val notes: String?,
    /** Adjacent rows sharing a non-null group form a superset (v3). */
    @ColumnInfo(name = "superset_group") val supersetGroup: Long? = null,
)

/** Planned set within a routine exercise (FR-018, data-model.md). */
@Entity(
    tableName = "set_templates",
    foreignKeys = [
        ForeignKey(
            entity = RoutineExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["routine_exercise_id", "set_number"], unique = true),
    ],
)
data class SetTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "routine_exercise_id") val routineExerciseId: Long,
    @ColumnInfo(name = "set_number") val setNumber: Int,
    @ColumnInfo(name = "target_reps") val targetReps: Int,
    @ColumnInfo(name = "target_weight") val targetWeight: Double?,
    @ColumnInfo(name = "target_weight_unit") val targetWeightUnit: String?,
    @ColumnInfo(name = "measurement_type") val measurementType: String,
)
