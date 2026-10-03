package com.gymora.ui.home

import com.gymora.domain.model.HistoryEntry
import com.gymora.domain.model.RoutineSummary

/** UI state for the home screen (FR-001, FR-002, FR-015). */
data class HomeUiState(
    val routines: List<RoutineSummary> = emptyList(),
    val recentWorkouts: List<HistoryEntry> = emptyList(),
    /** Monday of the week shown in the calendar strip. */
    val weekStart: java.time.LocalDate = java.time.LocalDate.now()
        .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)),
    /** Days in the shown week with at least one completed workout. */
    val workoutDays: Set<java.time.LocalDate> = emptySet(),
    /** Day tapped in the calendar strip; null when the day sheet is closed. */
    val selectedDay: java.time.LocalDate? = null,
    /** Workouts completed on [selectedDay]. */
    val selectedDayWorkouts: List<com.gymora.domain.model.WorkoutDetail> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
