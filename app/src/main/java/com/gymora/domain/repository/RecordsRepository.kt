package com.gymora.domain.repository

import com.gymora.domain.model.PersonalRecords

/**
 * Computes personal records on demand from completed workout sessions (FR-047, R-09).
 * Each record carries exercise name and date context.
 */
interface RecordsRepository {
    suspend fun getPersonalRecords(): PersonalRecords
}