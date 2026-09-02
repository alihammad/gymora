package com.gymora.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.domain.model.RoutineSummary
import com.gymora.ui.components.EmptyState
import com.gymora.ui.components.EmptyStateCopy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home screen (FR-001, FR-002, FR-015, FR-059): routine cards with name,
 * exercise count, last-performed date, and START; drag-to-reorder; Create
 * Routine action; links to My Routines / Recent Workouts / History.
 * The START action is wired to StartWorkoutUseCase in US3 (T042).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRoutineClick: (Long) -> Unit,
    onCreateRoutine: () -> Unit,
    onMyRoutines: () -> Unit,
    onRecentWorkouts: () -> Unit,
    onHistory: () -> Unit,
    onStartWorkout: ((Long) -> Unit)? = null,
    onRecentWorkoutClick: ((Long) -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Refresh the recent-workouts section whenever Home becomes visible again.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadRecentWorkouts()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Gymora") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateRoutine) {
                Icon(Icons.Filled.Add, contentDescription = "Create Routine")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onMyRoutines) { Text("My Routines") }
                TextButton(onClick = onRecentWorkouts) { Text("Recent Workouts") }
                TextButton(onClick = onHistory) { Text("History") }
            }

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }

                uiState.routines.isEmpty() -> {
                    EmptyState(
                        message = EmptyStateCopy.NO_ROUTINES,
                        actionLabel = EmptyStateCopy.CREATE_ROUTINE_ACTION,
                        onAction = onCreateRoutine,
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(uiState.routines, key = { _, routine -> routine.id }) { index, routine ->
                            RoutineCard(
                                routine = routine,
                                onClick = { onRoutineClick(routine.id) },
                                onStart = { onStartWorkout?.invoke(routine.id) },
                            )
                        }
                        // Recent Workouts section (T050a, FR-002, spec Assumption).
                        if (uiState.recentWorkouts.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Recent Workouts",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            items(
                                uiState.recentWorkouts,
                                key = { workout -> "recent-${workout.id}" },
                            ) { workout ->
                                RecentWorkoutRow(
                                    workout = workout,
                                    onClick = { onRecentWorkoutClick?.invoke(workout.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineCard(
    routine: RoutineSummary,
    onClick: () -> Unit,
    onStart: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = routine.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "${routine.exerciseCount} exercises",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                routine.lastPerformedAt?.let { timestamp ->
                    Text(
                        text = "Last performed ${formatDate(timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onStart) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Start ${routine.name}",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))

/** Recent-workout row (T050a, FR-002): name, date, duration; tap opens detail. */
@Composable
private fun RecentWorkoutRow(
    workout: com.gymora.domain.model.HistoryEntry,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = workout.routineNameSnapshot, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = formatDate(workout.startedAt.toEpochMilli()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = com.gymora.ui.workout.formatElapsed(workout.duration.seconds),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
