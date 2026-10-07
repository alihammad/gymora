package com.gymora.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Exercise library entry (FR-005..FR-010, BR-04/08/20, data-model.md).
 * Soft delete via [deletedAt]; name is not unique (spec Assumption).
 */
@Entity(
    tableName = "exercises",
    indices = [
        Index(value = ["deleted_at", "name"]),
    ],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "muscle_group") val muscleGroup: String?,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    // Extended descriptive metadata from docs/exercises.json.
    @ColumnInfo(name = "type") val type: String? = null,
    @ColumnInfo(name = "difficulty_level") val difficultyLevel: String? = null,
    @ColumnInfo(name = "force_type") val forceType: String? = null,
    @ColumnInfo(name = "mechanics") val mechanics: String? = null,
    @ColumnInfo(name = "category") val category: String? = null,
    @ColumnInfo(name = "instructions_json") val instructionsJson: String? = null,
    @ColumnInfo(name = "muscle_groups_json") val muscleGroupsJson: String? = null,
    @ColumnInfo(name = "equipment_json") val equipmentJson: String? = null,
    // Tracking and coaching (v5).
    @ColumnInfo(name = "measurement_type", defaultValue = "'WEIGHT_AND_REPS'")
    val measurementType: String = "WEIGHT_AND_REPS",
    @ColumnInfo(name = "is_unilateral", defaultValue = "0") val isUnilateral: Boolean = false,
    @ColumnInfo(name = "form_cues_json") val formCuesJson: String? = null,
    /** File name inside filesDir/exercise_media; relative so backups restore anywhere. */
    @ColumnInfo(name = "media_file") val mediaFile: String? = null,
)
