package com.gymora.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Daily step count from the phone's built-in step sensor and, when the user turned it
 * on, Health Connect. The sensor needs no account or network.
 */
interface StepRepository {

    /** False on phones without a hardware step counter. */
    val isSupported: Boolean

    /**
     * Today's steps, or null when no source is available (sensor not allowed and Health
     * Connect steps not on). With [useSensor] the sensor is listened to while collected
     * and its readings recorded; pass it only when the activity-recognition permission is
     * held. Health Connect is polled while collected. When both sources report, the larger
     * count wins, so steps are never counted twice. Rolls over at midnight.
     */
    fun observeTodaySteps(useSensor: Boolean): Flow<Int?>

    /**
     * Steps for each of the last [days] days (oldest first, today last, days without data
     * as 0). Merges the sensor history with Health Connect's when that is turned on,
     * taking the larger count per day. Today updates live.
     */
    fun observeHistory(days: Int): Flow<List<com.gymora.domain.model.DaySteps>>

    /**
     * Records the current sensor reading without the app being open (the midnight job), so
     * steps taken while the app was closed land on the right day. No-op without the
     * sensor or its permission.
     */
    suspend fun recordSnapshot()
}
