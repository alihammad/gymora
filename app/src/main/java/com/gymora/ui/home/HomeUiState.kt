package com.gymora.ui.home

import com.gymora.domain.model.RoutineSummary

/** UI state for the home screen (FR-001, FR-002, FR-015). */
data class HomeUiState(
    val routines: List<RoutineSummary> = emptyList(),
    /** Monday of the week shown in the calendar strip. */
    val weekStart: java.time.LocalDate = java.time.LocalDate.now()
        .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)),
    /** Days in the shown week with at least one completed workout. */
    val workoutDays: Set<java.time.LocalDate> = emptySet(),
    /** Day tapped in the calendar strip; null when the day sheet is closed. */
    val selectedDay: java.time.LocalDate? = null,
    /** Workouts completed on [selectedDay]. */
    val selectedDayWorkouts: List<com.gymora.domain.model.WorkoutDetail> = emptyList(),
    /** Weekly goal progress and streak; null until loaded. */
    val weeklyProgress: com.gymora.domain.calculator.WeeklyProgress? = null,
    /** Unfinished workout, if any; shown as a resume banner. */
    val activeWorkout: com.gymora.domain.model.ActiveWorkout? = null,
    /** Steps today from the step sensor and/or Health Connect; null when no source is available. */
    val stepsToday: Int? = null,
    val stepGoal: Int = com.gymora.domain.model.Settings.DEFAULT_STEP_GOAL,
    /** False on phones without a step counter sensor. */
    val stepsSupported: Boolean = true,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
