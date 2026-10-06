package com.gymora.domain.calculator

import com.gymora.domain.model.RoutineSummary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/** Progress towards the weekly goal plus the running weekly streak. */
data class WeeklyProgress(
    /** Workouts completed this week (Monday–Sunday). */
    val done: Int,
    val goal: Int,
    /** Consecutive weeks, up to and including this one once its goal is met, that hit the goal. */
    val streakWeeks: Int,
) {
    val goalMet: Boolean get() = done >= goal
}

/**
 * Habit rules: weekly goal, streaks, reminder timing and the suggested next
 * routine. Pure functions, unit-tested on the JVM (Constitution IX).
 */
object EngagementCalculators {

    /** Monday of the week containing [date]; weeks run Monday–Sunday like the Home week strip. */
    fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /**
     * [workoutDates] holds one local date per completed workout (repeats allowed).
     * The current week extends the streak once its goal is met; until then it
     * neither counts nor breaks it, so the streak survives an unfinished week.
     */
    fun weeklyProgress(workoutDates: List<LocalDate>, goal: Int, today: LocalDate): WeeklyProgress {
        val perWeek = workoutDates.groupingBy { weekStart(it) }.eachCount()
        val thisWeek = weekStart(today)
        val done = perWeek[thisWeek] ?: 0
        var streak = if (done >= goal) 1 else 0
        var week = thisWeek.minusWeeks(1)
        while ((perWeek[week] ?: 0) >= goal) {
            streak++
            week = week.minusWeeks(1)
        }
        return WeeklyProgress(done = done, goal = goal, streakWeeks = streak)
    }

    /**
     * The first moment strictly after [now] that falls on one of [days] at [time],
     * or null when [days] is empty (reminders off).
     */
    fun nextReminder(now: LocalDateTime, days: Set<DayOfWeek>, time: LocalTime): LocalDateTime? {
        if (days.isEmpty()) return null
        return (0L..7L).asSequence()
            .map { now.toLocalDate().plusDays(it).atTime(time) }
            .first { it.dayOfWeek in days && it.isAfter(now) }
    }

    /**
     * The routine to suggest next: the one after the most recently performed
     * routine in home-screen order (wrapping around), so a split is followed
     * in rotation. Falls back to the first routine when none was performed yet.
     */
    fun nextRoutine(routines: List<RoutineSummary>): RoutineSummary? {
        val last = routines.withIndex()
            .filter { it.value.lastPerformedAt != null }
            .maxByOrNull { it.value.lastPerformedAt!! }
            ?: return routines.firstOrNull()
        return routines[(last.index + 1) % routines.size]
    }

    /** Weekdays packed as bits (Monday = bit 0) for storage. */
    fun daysToMask(days: Set<DayOfWeek>): Int = days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

    fun maskToDays(mask: Int): Set<DayOfWeek> =
        DayOfWeek.entries.filterTo(sortedSetOf()) { mask and (1 shl (it.value - 1)) != 0 }
}
