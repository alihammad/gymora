package com.gymora.ui.navigation

/**
 * Navigation destinations for the Gymora nav graph (FR-004).
 * Bottom-nav tabs: Home, History, Exercises, Profile, Settings.
 * Push destinations: routine list/detail/editor, exercise editor, active workout,
 * workout summary, workout detail, exercise history, records.
 */
sealed class Destinations(val route: String) {
    data object Home : Destinations("home")
    data object History : Destinations("history")
    data object Exercises : Destinations("exercises")
    data object Profile : Destinations("profile")
    data object Settings : Destinations("settings")

    data object RoutineList : Destinations("routines")
    data object RoutineEditor : Destinations("routine/{routineId}") {
        fun create(routineId: Long) = "routine/$routineId"
        const val ARG = "routineId"
    }
    data object ExerciseEditor : Destinations("exercise/{exerciseId}") {
        fun create(exerciseId: Long) = "exercise/$exerciseId"
        const val ARG = "exerciseId"
    }
    data object ExerciseDetail : Destinations("exercise/detail/{exerciseId}") {
        fun create(exerciseId: Long) = "exercise/detail/$exerciseId"
        const val ARG = "exerciseId"
    }
    data object ExerciseFilter : Destinations("exercise/filter/{kind}/{value}") {
        fun create(kind: String, value: String) =
            "exercise/filter/$kind/${android.net.Uri.encode(value)}"
        const val KIND_ARG = "kind"
        const val VALUE_ARG = "value"
    }
    data object ActiveWorkout : Destinations("workout/active/{sessionId}") {
        fun create(sessionId: Long) = "workout/active/$sessionId"
        const val ARG = "sessionId"
    }
    data object StartWorkout : Destinations("workout/start/{routineId}") {
        fun create(routineId: Long) = "workout/start/$routineId"
        const val ARG = "routineId"
    }
    data object WorkoutSummary : Destinations("workout/summary/{sessionId}") {
        fun create(sessionId: Long) = "workout/summary/$sessionId"
        const val ARG = "sessionId"
    }
    data object WorkoutDetail : Destinations("workout/detail/{sessionId}") {
        fun create(sessionId: Long) = "workout/detail/$sessionId"
        const val ARG = "sessionId"
    }
    data object ExerciseHistory : Destinations("exercise/history/{exerciseId}") {
        fun create(exerciseId: Long) = "exercise/history/$exerciseId"
        const val ARG = "exerciseId"
    }
    data object Records : Destinations("records")
    data object Body : Destinations("body")
    data object Progress : Destinations("progress")
}
