package com.gymora.ui.progress

import com.gymora.domain.model.WorkoutStat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** This week's (Monday–Sunday) numbers for the dashboard. Volume is in kg. */
data class WeekOverview(
    val workouts: Int = 0,
    val durationSeconds: Long = 0,
    val volumeKg: Double = 0.0,
    /** Volume per day, Monday first; always 7 entries. */
    val dailyVolumeKg: List<Double> = List(7) { 0.0 },
    /** Index (0 = Monday) of [today] within the week. */
    val todayIndex: Int = 0,
) {
    /** Average over the days that had training; 0 when there were none. */
    val averageTrainingDayKg: Double
        get() = dailyVolumeKg.filter { it > 0 }.average().takeUnless { it.isNaN() } ?: 0.0
}

fun weekOverview(stats: List<WorkoutStat>, today: LocalDate, zone: ZoneId): WeekOverview {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val daily = DoubleArray(7)
    var workouts = 0
    var seconds = 0L
    stats.forEach { stat ->
        val day = stat.startedAt.atZone(zone).toLocalDate()
        val index = (day.toEpochDay() - monday.toEpochDay()).toInt()
        if (index in 0..6) {
            workouts++
            seconds += stat.duration.seconds
            daily[index] += stat.volumeKg
        }
    }
    return WeekOverview(
        workouts = workouts,
        durationSeconds = seconds,
        volumeKg = daily.sum(),
        dailyVolumeKg = daily.toList(),
        todayIndex = (today.toEpochDay() - monday.toEpochDay()).toInt(),
    )
}
