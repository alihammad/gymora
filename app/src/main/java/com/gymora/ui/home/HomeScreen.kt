package com.gymora.ui.home

import com.gymora.ui.theme.GymoraShapes
import com.gymora.ui.components.GymoraLoading
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import com.gymora.ui.components.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.ui.graphics.vector.ImageVector
import com.gymora.ui.components.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.gymora.ui.components.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onCreateExercise: () -> Unit,
    onMyRoutines: () -> Unit,
    onRecentWorkouts: () -> Unit,
    onHistory: () -> Unit,
    onRecords: () -> Unit,
    onBody: () -> Unit,
    onProgress: () -> Unit,
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
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                FloatingActionButton(
                    onClick = { menuOpen = true },
                    elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Create")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("New workout") },
                        onClick = {
                            menuOpen = false
                            onCreateRoutine()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("New exercise") },
                        onClick = {
                            menuOpen = false
                            onCreateExercise()
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            WeekStrip(
                weekStart = uiState.weekStart,
                workoutDays = uiState.workoutDays,
                onShiftWeek = viewModel::onShiftWeek,
                onDayClick = viewModel::onDaySelected,
            )

            uiState.weeklyProgress?.let { progress ->
                WeeklyGoalCard(
                    progress = progress,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            uiState.selectedDay?.let { day ->
                DayWorkoutsSheet(
                    day = day,
                    workouts = uiState.selectedDayWorkouts,
                    onDismiss = viewModel::onDayDismissed,
                )
            }

            // Single-line chips in a horizontally scrollable row: nothing wraps
            // or squeezes on narrow screens.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickLinkChip("My Workouts", Icons.Filled.FormatListBulleted, onMyRoutines)
                QuickLinkChip("Recent", Icons.Filled.Schedule, onRecentWorkouts)
                QuickLinkChip("History", Icons.Filled.History, onHistory)
                QuickLinkChip("Progress", Icons.AutoMirrored.Filled.ShowChart, onProgress)
                QuickLinkChip("Records", Icons.Filled.EmojiEvents, onRecords)
                QuickLinkChip("Body", Icons.Filled.MonitorWeight, onBody)
            }

            when {
                uiState.isLoading -> {
                    GymoraLoading(modifier = Modifier.padding(16.dp))
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

/** Bottom sheet listing the exercises performed on the tapped calendar day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayWorkoutsSheet(
    day: java.time.LocalDate,
    workouts: List<com.gymora.domain.model.WorkoutDetail>,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = GymoraShapes.sheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = day.format(
                    java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()),
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            if (workouts.isEmpty()) {
                Text(
                    text = "No workouts on this day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            workouts.forEach { workout ->
                Text(
                    text = workout.session.routineNameSnapshot,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (workout.exercises.isEmpty()) {
                    Text("No exercises recorded", style = MaterialTheme.typography.bodyMedium)
                }
                workout.exercises.forEach { exercise ->
                    val done = exercise.sets.count { it.isCompleted }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = exercise.exerciseName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "$done ${if (done == 1) "set" else "sets"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickLinkChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, maxLines = 1, softWrap = false) },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            labelColor = MaterialTheme.colorScheme.onSurface,
            leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = GymoraShapes.chip,
    )
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
            FilledIconButton(onClick = onStart) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Start ${routine.name}",
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
