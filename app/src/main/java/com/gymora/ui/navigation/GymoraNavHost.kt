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

/**
 * Root navigation host (FR-004). Bottom nav for Home/History/Exercises/Settings;
 * dedicated destinations for routines, exercises, workouts, and records.
 * Screens are placeholders until their story phases land.
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
            composable(Destinations.Home.route) { PlaceholderScreen("Home") }
            composable(Destinations.History.route) { PlaceholderScreen("History") }
            composable(Destinations.Exercises.route) { PlaceholderScreen("Exercises") }
            composable(Destinations.Settings.route) { PlaceholderScreen("Settings") }
            composable(Destinations.RoutineList.route) { PlaceholderScreen("My Routines") }
            composable(Destinations.RoutineEditor.route) { PlaceholderScreen("Routine Editor") }
            composable(Destinations.ExerciseEditor.route) { PlaceholderScreen("Exercise Editor") }
            composable(Destinations.ActiveWorkout.route) { PlaceholderScreen("Active Workout") }
            composable(Destinations.WorkoutSummary.route) { PlaceholderScreen("Workout Summary") }
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
