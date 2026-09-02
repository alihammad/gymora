package com.gymora.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gymora.ui.home.HomeScreen
import com.gymora.ui.library.ExerciseEditorScreen
import com.gymora.ui.library.ExerciseLibraryScreen
import com.gymora.ui.routines.RoutineEditorScreen
import com.gymora.ui.routines.RoutineListScreen
import com.gymora.ui.workout.ActiveWorkoutScreen
import com.gymora.ui.workout.WorkoutSummaryScreen

private data class BottomTab(
    val destination: Destinations,
    val label: String,
    val icon: ImageVector,
)

private val bottomTabs = listOf(
    BottomTab(Destinations.Home, "Home", Icons.Filled.Home),
    BottomTab(Destinations.History, "History", Icons.Filled.History),
    BottomTab(Destinations.Exercises, "Exercises", Icons.Filled.FitnessCenter),
    BottomTab(Destinations.Settings, "Settings", Icons.Filled.Settings),
)

/** Sentinel id for creating a new exercise (no existing id). */
const val NEW_EXERCISE_ID = 0L

/** Sentinel id for creating a new routine (no existing id). */
const val NEW_ROUTINE_ID = 0L

/**
 * Root navigation host (FR-004). Bottom nav for Home/History/Exercises/Settings;
 * dedicated destinations for routines, exercises, workouts, and records.
 * The active workout is a dedicated experience replacing the bottom nav.
 */
@Composable
fun GymoraNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // The active workout replaces the bottom nav with a dedicated experience (FR-004).
    val showBottomBar = currentRoute in bottomTabs.map { it.destination.route }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.destination.route,
                            onClick = {
                                navController.navigate(tab.destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destinations.Home.route) {
                HomeScreen(
                    onRoutineClick = { routineId ->
                        navController.navigate(Destinations.RoutineEditor.create(routineId))
                    },
                    onCreateRoutine = {
                        navController.navigate(Destinations.RoutineEditor.create(NEW_ROUTINE_ID))
                    },
                    onMyRoutines = { navController.navigate(Destinations.RoutineList.route) },
                    onRecentWorkouts = { navController.navigate(Destinations.History.route) },
                    onHistory = { navController.navigate(Destinations.History.route) },
                    onStartWorkout = { routineId ->
                        navController.navigate(Destinations.StartWorkout.create(routineId))
                    },
                )
            }
            composable(Destinations.History.route) { PlaceholderScreen("History") }
            composable(Destinations.Exercises.route) {
                ExerciseLibraryScreen(
                    onExerciseClick = { exerciseId ->
                        navController.navigate(Destinations.ExerciseEditor.create(exerciseId))
                    },
                    onCreateExercise = {
                        navController.navigate(Destinations.ExerciseEditor.create(NEW_EXERCISE_ID))
                    },
                )
            }
            composable(Destinations.Settings.route) { PlaceholderScreen("Settings") }
            composable(Destinations.RoutineList.route) {
                RoutineListScreen(
                    onBack = { navController.popBackStack() },
                    onRoutineClick = { routineId ->
                        navController.navigate(Destinations.RoutineEditor.create(routineId))
                    },
                )
            }
            composable(Destinations.RoutineEditor.route) {
                RoutineEditorScreen(onBack = { navController.popBackStack() })
            }
            composable(Destinations.ExerciseEditor.route) {
                ExerciseEditorScreen(onBack = { navController.popBackStack() })
            }
            composable(Destinations.ActiveWorkout.route) {
                ActiveWorkoutScreen(
                    onFinished = { sessionId ->
                        navController.navigate(Destinations.WorkoutSummary.create(sessionId)) {
                            popUpTo(Destinations.Home.route)
                        }
                    },
                    onDiscarded = {
                        navController.popBackStack(Destinations.Home.route, inclusive = false)
                    },
                )
            }
            composable(Destinations.StartWorkout.route) {
                ActiveWorkoutScreen(
                    onFinished = { sessionId ->
                        navController.navigate(Destinations.WorkoutSummary.create(sessionId)) {
                            popUpTo(Destinations.Home.route)
                        }
                    },
                    onDiscarded = {
                        navController.popBackStack(Destinations.Home.route, inclusive = false)
                    },
                )
            }
            composable(Destinations.WorkoutSummary.route) {
                WorkoutSummaryScreen(
                    onDone = {
                        navController.popBackStack(Destinations.Home.route, inclusive = false)
                    },
                )
            }
            composable(Destinations.WorkoutDetail.route) { PlaceholderScreen("Workout Detail") }
            composable(Destinations.ExerciseHistory.route) { PlaceholderScreen("Exercise History") }
            composable(Destinations.Records.route) { PlaceholderScreen("Records") }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
    }
}
