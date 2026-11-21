package com.gymora.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** How a [BodyMetric] is stored and converted for display. */
enum class BodyMetricKind {
    /** Stored in kg; shown in kg or lb. */
    WEIGHT,

    /** Stored in cm; shown in cm or inches. */
    LENGTH,

    /** Stored in kcal; never converted. */
    ENERGY,
}

/**
 * Everything that can be recorded in a body-measurement entry, in form order.
 * Charts and history read a metric's values with [series].
 */
enum class BodyMetric(val label: String, val kind: BodyMetricKind) {
    WEIGHT("Weight", BodyMetricKind.WEIGHT),
    CALORIES("Caloric intake", BodyMetricKind.ENERGY),
    NECK("Neck", BodyMetricKind.LENGTH),
    SHOULDERS("Shoulders", BodyMetricKind.LENGTH),
    CHEST("Chest", BodyMetricKind.LENGTH),
    LEFT_BICEP("Left bicep", BodyMetricKind.LENGTH),
    RIGHT_BICEP("Right bicep", BodyMetricKind.LENGTH),
    LEFT_ARM("Left arm", BodyMetricKind.LENGTH),
    RIGHT_ARM("Right arm", BodyMetricKind.LENGTH),
    WAIST_ABOVE_NAVEL("Waist above navel", BodyMetricKind.LENGTH),
    WAIST("Waist", BodyMetricKind.LENGTH),
    WAIST_BELOW_NAVEL("Waist below navel", BodyMetricKind.LENGTH),
    HIPS("Hips", BodyMetricKind.LENGTH),
    LEFT_THIGH("Left thigh", BodyMetricKind.LENGTH),
    RIGHT_THIGH("Right thigh", BodyMetricKind.LENGTH),
    LEFT_CALF("Left calf", BodyMetricKind.LENGTH),
    RIGHT_CALF("Right calf", BodyMetricKind.LENGTH),
}

/**
 * One recorded body-measurement entry; any metric may be absent. Weight in kg,
 * lengths in cm, caloric intake in kcal. Own entries are kept one per calendar
 * date; weigh-ins imported from Health Connect ([imported]) are separate entries.
 */
data class BodyMeasurement(
    val id: Long,
    val measuredAt: Instant,
    val values: Map<BodyMetric, Double>,
    val imported: Boolean = false,
) {
    val isEmpty: Boolean get() = values.isEmpty()

    operator fun get(metric: BodyMetric): Double? = values[metric]

    fun date(zone: ZoneId = ZoneId.systemDefault()): LocalDate = measuredAt.atZone(zone).toLocalDate()
}

/** A metric's value on one date. */
data class MetricPoint(val date: LocalDate, val value: Double)

/**
 * [metric]'s history, oldest first, one point per date. When several entries on one
 * date have the value (e.g. an own entry and an imported weigh-in) the latest wins.
 * [entries] may be in any order.
 */
fun List<BodyMeasurement>.series(metric: BodyMetric, zone: ZoneId = ZoneId.systemDefault()): List<MetricPoint> =
    filter { it[metric] != null }
        .sortedBy { it.measuredAt }
        .associateBy { it.date(zone) }
        .map { (date, entry) -> MetricPoint(date, entry[metric]!!) }
        .sortedBy { it.date }

/**
 * The most recent value of each metric, taken independently: logging only
 * weight today does not hide last month's chest size. [date] is the newest entry.
 */
data class LatestMeasurements(
    val date: Instant?,
    val values: Map<BodyMetric, Double>,
) {
    operator fun get(metric: BodyMetric): Double? = values[metric]

    companion object {
        /** [entries] must be newest first. */
        fun from(entries: List<BodyMeasurement>) = LatestMeasurements(
            date = entries.firstOrNull()?.measuredAt,
            values = BodyMetric.entries.mapNotNull { metric ->
                entries.firstNotNullOfOrNull { it[metric] }?.let { metric to it }
            }.toMap(),
        )
    }
}
