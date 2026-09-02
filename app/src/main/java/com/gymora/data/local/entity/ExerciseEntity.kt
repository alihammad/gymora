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
)
