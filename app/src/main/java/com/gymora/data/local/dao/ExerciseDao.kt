package com.gymora.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gymora.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    /** Active (non-deleted) library for browsing, ordered by muscle group then name (FR-008). */
    @Query(
        "SELECT * FROM exercises WHERE deleted_at IS NULL " +
            "ORDER BY muscle_group IS NULL, muscle_group, name COLLATE NOCASE",
    )
    fun observeActiveLibrary(): Flow<List<ExerciseEntity>>

    /** Case-insensitive name search excluding soft-deleted; includes custom (FR-008). */
    @Query(
        "SELECT * FROM exercises WHERE deleted_at IS NULL AND name LIKE '%' || :query || '%' " +
            "COLLATE NOCASE ORDER BY name COLLATE NOCASE",
    )
    suspend fun search(query: String): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observeById(id: Long): Flow<ExerciseEntity?>

    /** Case-insensitive name lookup; used by the routine seeder to resolve seed names to ids. */
    @Query("SELECT * FROM exercises WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getByName(name: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE deleted_at IS NULL")
    suspend fun getAllActiveOnce(): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercises WHERE deleted_at IS NULL")
    suspend fun count(): Int

    @Insert
    suspend fun insert(entity: ExerciseEntity): Long

    @Update
    suspend fun update(entity: ExerciseEntity)

    /** Soft delete (R-03): keeps the row for historical referential integrity. */
    @Query("UPDATE exercises SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)
}
