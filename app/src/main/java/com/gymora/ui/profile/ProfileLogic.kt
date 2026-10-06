package com.gymora.ui.profile

import com.gymora.domain.model.WorkoutStat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Time window applied to every profile chart. [months] = null means all time. */
enum class ProfilePeriod(val label: String, val months: Long?) {
    MONTH_1("1 month", 1),
    MONTHS_3("3 months", 3),
    MONTHS_6("6 months", 6),
    YEAR_1("1 year", 12),
    ALL("All time", null),
}

/** Workouts that started on or after the period's cutoff date; input order is kept. */
fun filterByPeriod(
    stats: List<WorkoutStat>,
    period: ProfilePeriod,
    today: LocalDate,
    zone: ZoneId,
): List<WorkoutStat> {
    val months = period.months ?: return stats
    val cutoff = today.minusMonths(months)
    return stats.filter { it.startedAt.atZone(zone).toLocalDate() >= cutoff }
}

/** Y axis of a bar chart: runs 0..[max] in steps of [step]. */
data class AxisScale(val max: Double, val step: Double) {
    val ticks: List<Double> get() = generateSequence(0.0) { it + step }.takeWhile { it <= max + step / 2 }.toList()
}

/** "Nice" axis: the smallest 1/2/5 × 10^k step that covers [maxValue] in at most [maxIntervals] intervals. */
fun niceAxis(maxValue: Double, maxIntervals: Int = 5): AxisScale {
    if (maxValue <= 0.0) return AxisScale(max = 1.0, step = 0.25)
    val magnitude = 10.0.pow(floor(log10(maxValue / maxIntervals)))
    val step = listOf(1.0, 2.0, 5.0, 10.0)
        .map { it * magnitude }
        .first { ceil(maxValue / it) <= maxIntervals }
    return AxisScale(max = ceil(maxValue / step) * step, step = step)
}
