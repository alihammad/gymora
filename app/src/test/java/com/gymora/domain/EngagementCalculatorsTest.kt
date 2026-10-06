package com.gymora.domain

import com.gymora.domain.calculator.EngagementCalculators
import com.gymora.domain.model.RoutineSummary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EngagementCalculatorsTest {

    // Wednesday; its week runs Mon 5 – Sun 11 October 2026.
    private val today = LocalDate.of(2026, 10, 7)
    private val thisMonday = LocalDate.of(2026, 10, 5)

    /** [count] workouts in the week starting [monday]. */
    private fun week(monday: LocalDate, count: Int) = List(count) { monday.plusDays(it.toLong() % 7) }

    @Test
    fun countsWorkoutsInTheCurrentMondayToSundayWeek() {
        val dates = listOf(thisMonday.minusDays(1), thisMonday, today, today)
        val progress = EngagementCalculators.weeklyProgress(dates, goal = 4, today = today)
        assertEquals(3, progress.done)
        assertEquals(false, progress.goalMet)
    }

    @Test
    fun streakCountsConsecutiveGoalWeeksIncludingAMetCurrentWeek() {
        val dates = week(thisMonday, 3) + week(thisMonday.minusWeeks(1), 3) + week(thisMonday.minusWeeks(2), 4)
        assertEquals(3, EngagementCalculators.weeklyProgress(dates, goal = 3, today = today).streakWeeks)
    }

    @Test
    fun unfinishedCurrentWeekNeitherCountsNorBreaksTheStreak() {
        val dates = week(thisMonday, 1) + week(thisMonday.minusWeeks(1), 3) + week(thisMonday.minusWeeks(2), 3)
        assertEquals(2, EngagementCalculators.weeklyProgress(dates, goal = 3, today = today).streakWeeks)
    }

    @Test
    fun aMissedWeekEndsTheStreak() {
        val dates = week(thisMonday.minusWeeks(1), 3) + week(thisMonday.minusWeeks(2), 2) +
            week(thisMonday.minusWeeks(3), 3)
        assertEquals(1, EngagementCalculators.weeklyProgress(dates, goal = 3, today = today).streakWeeks)
    }

    @Test
    fun noWorkoutsMeansNoStreak() {
        val progress = EngagementCalculators.weeklyProgress(emptyList(), goal = 3, today = today)
        assertEquals(0, progress.done)
        assertEquals(0, progress.streakWeeks)
    }

    @Test
    fun nextReminderIsLaterTodayWhenTheTimeHasNotPassed() {
        val now = today.atTime(9, 0)
        val next = EngagementCalculators.nextReminder(now, setOf(DayOfWeek.WEDNESDAY), LocalTime.of(18, 0))
        assertEquals(today.atTime(18, 0), next)
    }

    @Test
    fun nextReminderSkipsToTheNextChosenDayOnceTodaysTimeHasPassed() {
        val now = today.atTime(18, 0) // exactly at reminder time: already fired
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        assertEquals(
            LocalDate.of(2026, 10, 12).atTime(18, 0),
            EngagementCalculators.nextReminder(now, days, LocalTime.of(18, 0)),
        )
    }

    @Test
    fun nextReminderWrapsAFullWeekForASingleDay() {
        val now = LocalDateTime.of(2026, 10, 7, 19, 0)
        assertEquals(
            LocalDate.of(2026, 10, 14).atTime(18, 0),
            EngagementCalculators.nextReminder(now, setOf(DayOfWeek.WEDNESDAY), LocalTime.of(18, 0)),
        )
    }

    @Test
    fun noReminderDaysMeansNoReminder() {
        assertNull(EngagementCalculators.nextReminder(today.atTime(9, 0), emptySet(), LocalTime.NOON))
    }

    @Test
    fun nextRoutineFollowsTheLastPerformedOneInHomeOrder() {
        val routines = listOf(
            RoutineSummary(1, "Push", 5, lastPerformedAt = 100),
            RoutineSummary(2, "Pull", 5, lastPerformedAt = 300),
            RoutineSummary(3, "Legs", 5, lastPerformedAt = 200),
        )
        assertEquals(3L, EngagementCalculators.nextRoutine(routines)?.id)
    }

    @Test
    fun nextRoutineWrapsAroundAfterTheLastRoutine() {
        val routines = listOf(
            RoutineSummary(1, "Push", 5, lastPerformedAt = 100),
            RoutineSummary(2, "Pull", 5, lastPerformedAt = 300),
        )
        assertEquals(1L, EngagementCalculators.nextRoutine(routines)?.id)
    }

    @Test
    fun nextRoutineIsTheFirstWhenNothingWasPerformed() {
        val routines = listOf(RoutineSummary(1, "Push", 5, null), RoutineSummary(2, "Pull", 5, null))
        assertEquals(1L, EngagementCalculators.nextRoutine(routines)?.id)
        assertNull(EngagementCalculators.nextRoutine(emptyList()))
    }

    @Test
    fun reminderDaysRoundTripThroughTheStoredMask() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY)
        val mask = EngagementCalculators.daysToMask(days)
        assertEquals(0b1001001, mask)
        assertEquals(days, EngagementCalculators.maskToDays(mask))
    }
}
