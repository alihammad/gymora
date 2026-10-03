package com.gymora.ui.profile

import com.gymora.domain.model.WorkoutStat
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileLogicTest {

    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 3)

    private fun stat(date: LocalDate, name: String = "Push", id: Long = 1) = WorkoutStat(
        sessionId = id,
        routineName = name,
        startedAt = date.atTime(10, 0).toInstant(zone),
        duration = Duration.ofMinutes(60),
        volumeKg = 1000.0,
        totalReps = 50,
    )

    @Test
    fun filterKeepsOnlyWorkoutsInsideThePeriod() {
        val stats = listOf(
            stat(LocalDate.of(2025, 9, 1), id = 1),
            stat(LocalDate.of(2026, 3, 20), id = 2),
            stat(LocalDate.of(2026, 7, 10), id = 3),
            stat(LocalDate.of(2026, 9, 3), id = 4), // exactly one month back: included
            stat(LocalDate.of(2026, 9, 2), id = 5),
        )
        fun ids(p: ProfilePeriod) = filterByPeriod(stats, p, today, zone).map { it.sessionId }

        assertEquals(listOf(4L), ids(ProfilePeriod.MONTH_1))
        assertEquals(listOf(3L, 4L, 5L), ids(ProfilePeriod.MONTHS_3))
        assertEquals(listOf(3L, 4L, 5L), ids(ProfilePeriod.MONTHS_6))
        assertEquals(listOf(2L, 3L, 4L, 5L), ids(ProfilePeriod.YEAR_1))
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), ids(ProfilePeriod.ALL))
    }

    @Test
    fun niceAxisCoversMaxWithRoundSteps() {
        assertEquals(AxisScale(max = 2.0, step = 0.5), niceAxis(1.7))
        assertEquals(AxisScale(max = 15000.0, step = 5000.0), niceAxis(11300.0))
        assertEquals(AxisScale(max = 100.0, step = 20.0), niceAxis(85.0))
        assertEquals(AxisScale(max = 1.0, step = 0.2), niceAxis(0.9))
        assertEquals(listOf(0.0, 20.0, 40.0, 60.0, 80.0, 100.0), niceAxis(85.0).ticks)
    }

    @Test
    fun niceAxisHandlesNoData() {
        assertEquals(1.0, niceAxis(0.0).max, 0.0)
    }

    @Test
    fun calendarMonthsRunNewestFirstFromTheFirstWorkout() {
        val months = calendarMonths(listOf(stat(LocalDate.of(2026, 8, 15))), today, zone)
        assertEquals(listOf(YearMonth.of(2026, 10), YearMonth.of(2026, 9), YearMonth.of(2026, 8)), months)
    }

    @Test
    fun calendarShowsCurrentMonthWhenThereAreNoWorkouts() {
        assertEquals(listOf(YearMonth.of(2026, 10)), calendarMonths(emptyList(), today, zone))
    }

    @Test
    fun dayLabelNamesTheRoutineAndCountsExtras() {
        assertEquals("Push", dayLabel(listOf(stat(today, "Push"))))
        assertEquals("Push +1", dayLabel(listOf(stat(today, "Push"), stat(today, "Legs"))))
    }
}
