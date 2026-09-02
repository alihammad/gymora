package com.gymora.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gymora.data.local.entity.RoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Query("SELECT * FROM routines ORDER BY position")
    fun observeAll(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines ORDER BY position")
    suspend fun getAllOnce(): List<RoutineEntity>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getById(id: Long): RoutineEntity?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM routines")
    suspend fun nextPosition(): Int

    @Insert
    suspend fun insert(entity: RoutineEntity): Long

    @Update
    suspend fun update(entity: RoutineEntity)

    /** Template-only deletion; history is protected separately (FR-013, BR-03). */
    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE routines SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)
}
