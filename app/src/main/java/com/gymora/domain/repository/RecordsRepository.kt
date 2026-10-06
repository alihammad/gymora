package com.gymora.domain.repository

import com.gymora.domain.model.PersonalRecords

/**
 * Computes personal records on demand from completed workout sessions (FR-047, R-09).
 * Each record carries exercise name and date context.
 */
interface RecordsRepository {
    suspend fun getPersonalRecords(): PersonalRecords

    /**
     * Personal bests set in [sessionId], at most one per exercise (weight beats
     * est. 1RM beats reps). Exercises never performed before are skipped: a
     * first attempt is a baseline, not a record.
     */
    suspend fun getSessionRecords(sessionId: Long): List<com.gymora.domain.model.SessionRecord>
}
