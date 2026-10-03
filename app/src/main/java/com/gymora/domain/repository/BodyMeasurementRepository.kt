package com.gymora.domain.repository

import com.gymora.domain.model.BodyMeasurement
import kotlinx.coroutines.flow.Flow

interface BodyMeasurementRepository {

    /** All entries, newest first. */
    fun observeAll(): Flow<List<BodyMeasurement>>

    /** Saves a new entry stamped now. [entry].id is ignored. */
    suspend fun save(entry: BodyMeasurement)
}
