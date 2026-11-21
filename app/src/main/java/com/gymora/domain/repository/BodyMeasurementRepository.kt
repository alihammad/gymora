package com.gymora.domain.repository

import com.gymora.domain.model.BodyMeasurement
import com.gymora.domain.model.BodyMetric
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface BodyMeasurementRepository {

    /** All entries, newest first. */
    fun observeAll(): Flow<List<BodyMeasurement>>

    /**
     * Records [values] as the measurements for [date]: replaces that date's own entry,
     * or creates one. Metrics missing from [values] are cleared on that date. Returns
     * the saved entry and any duplicate own entries on that date that were folded into it.
     */
    suspend fun saveForDate(date: LocalDate, values: Map<BodyMetric, Double>): BodySaveResult

    suspend fun delete(id: Long)
}

/** [id] is the saved entry; [removedIds] were other own entries on the same date, now deleted. */
data class BodySaveResult(val id: Long, val removedIds: List<Long>)
