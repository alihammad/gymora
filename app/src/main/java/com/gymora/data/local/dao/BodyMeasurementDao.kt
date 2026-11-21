package com.gymora.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gymora.data.local.entity.BodyMeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMeasurementDao {

    /** All entries, newest first. */
    @Query("SELECT * FROM body_measurements ORDER BY measured_at DESC, id DESC")
    fun observeAll(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurements ORDER BY measured_at")
    suspend fun getAllOnce(): List<BodyMeasurementEntity>

    @Query("SELECT * FROM body_measurements WHERE id = :id")
    suspend fun getById(id: Long): BodyMeasurementEntity?

    /** Own (not imported) entries measured in [fromMs, toMs), newest first. */
    @Query(
        "SELECT * FROM body_measurements WHERE external_id IS NULL " +
            "AND measured_at >= :fromMs AND measured_at < :toMs ORDER BY measured_at DESC, id DESC",
    )
    suspend fun getOwnBetween(fromMs: Long, toMs: Long): List<BodyMeasurementEntity>

    @Insert
    suspend fun insert(entity: BodyMeasurementEntity): Long

    @Update
    suspend fun update(entity: BodyMeasurementEntity)

    @Query("DELETE FROM body_measurements WHERE id = :id")
    suspend fun deleteById(id: Long)
}
