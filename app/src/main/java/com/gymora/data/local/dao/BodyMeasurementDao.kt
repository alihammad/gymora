package com.gymora.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gymora.data.local.entity.BodyMeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMeasurementDao {

    /** All entries, newest first. */
    @Query("SELECT * FROM body_measurements ORDER BY measured_at DESC, id DESC")
    fun observeAll(): Flow<List<BodyMeasurementEntity>>

    @Insert
    suspend fun insert(entity: BodyMeasurementEntity): Long
}
