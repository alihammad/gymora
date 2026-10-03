package com.gymora.domain.model

import java.time.Instant

/** One recorded body-measurement entry; any value may be absent. Weight in kg, lengths in cm. */
data class BodyMeasurement(
    val id: Long,
    val measuredAt: Instant,
    val weightKg: Double?,
    val shouldersCm: Double?,
    val chestCm: Double?,
    val aboveNavelCm: Double?,
    val navelCm: Double?,
    val belowNavelCm: Double?,
    val thighCm: Double?,
) {
    val isEmpty: Boolean
        get() = listOf(weightKg, shouldersCm, chestCm, aboveNavelCm, navelCm, belowNavelCm, thighCm)
            .all { it == null }
}

/**
 * The most recent value of each measurement, taken independently: logging only
 * weight today does not hide last month's chest size. [date] is the newest entry.
 */
data class LatestMeasurements(
    val date: Instant?,
    val weightKg: Double?,
    val shouldersCm: Double?,
    val chestCm: Double?,
    val aboveNavelCm: Double?,
    val navelCm: Double?,
    val belowNavelCm: Double?,
    val thighCm: Double?,
) {
    companion object {
        /** [entries] must be newest first. */
        fun from(entries: List<BodyMeasurement>) = LatestMeasurements(
            date = entries.firstOrNull()?.measuredAt,
            weightKg = entries.firstNotNullOfOrNull { it.weightKg },
            shouldersCm = entries.firstNotNullOfOrNull { it.shouldersCm },
            chestCm = entries.firstNotNullOfOrNull { it.chestCm },
            aboveNavelCm = entries.firstNotNullOfOrNull { it.aboveNavelCm },
            navelCm = entries.firstNotNullOfOrNull { it.navelCm },
            belowNavelCm = entries.firstNotNullOfOrNull { it.belowNavelCm },
            thighCm = entries.firstNotNullOfOrNull { it.thighCm },
        )
    }
}
