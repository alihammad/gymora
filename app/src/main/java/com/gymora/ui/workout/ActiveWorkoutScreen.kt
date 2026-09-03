package com.gymora.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.domain.model.ActiveExercise
import com.gymora.domain.model.ActiveSet
import com.gymora.domain.model.PreviousPerformance
import com.gymora.domain.model.SetValue
import com.gymora.ui.components.ConfirmDialog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Active workout screen (FR-022..FR-037): timestamp-derived timer, per-set
 * weight/reps entry with immediate saving, add set/exercise, skip/remove
 * exercise, finish with confirmation, cancel with confirmation.
 * Optimized for fast gym use: large targets, minimal typing (FR-022, FR-062).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    onFinished: (Long) -> Unit,
    onDiscarded: () -> Unit,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.finishedSessionId) {
        uiState.finishedSessionId?.let { id ->
            if (id == -1L) onDiscarded() else onFinished(id)
        }
    }

    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.activeWorkout?.session?.routineNameSnapshot ?: "Workout")
                        Text(
                            text = formatElapsed(uiState.elapsedSeconds),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = viewModel::onCancelRequested) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel workout")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(innerPadding).padding(16.dp))
        } else {
            val workout = uiState.activeWorkout
            if (workout == null) {
                Text(
                    text = "Workout not found.",
                    modifier = Modifier.padding(innerPadding).padding(16.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(workout.exercises, key = { it.workoutExerciseId }) { exercise ->
                        ExerciseCard(
                            exercise = exercise,
                            previousPerformance = exercise.exerciseId?.let {
                                uiState.previousPerformanceMap[it]
                            },
                            onWeightChanged = viewModel::onWeightChanged,
                            onRepsChanged = viewModel::onRepsChanged,
                            onToggleComplete = viewModel::onToggleComplete,
                            onAddSet = viewModel::onAddSet,
                            onRemove = { viewModel.onRemoveExercise(exercise.workoutExerciseId) },
                        )
                    }
                    item {
                        Button(
                            onClick = viewModel::onAddExerciseClicked,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text("Add exercise")
                        }
                    }
                    item {
                        Button(
                            onClick = viewModel::onFinishRequested,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("FINISH WORKOUT")
                        }
                    }
                }
            }
        }
    }

    // Exercise picker (FR-028)
    if (uiState.showExercisePicker) {
        AlertDialog(
            onDismissRequest = viewModel::onExercisePickerDismissed,
            title = { Text("Add exercise") },
            text = {
                LazyColumn {
                    items(uiState.libraryExercises, key = { it.id }) { exercise ->
                        ListItem(
                            headlineContent = { Text(exercise.name) },
                            trailingContent = {
                                IconButton(onClick = { viewModel.onExercisePicked(exercise.id) }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add ${exercise.name}")
                                }
                            },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = viewModel::onExercisePickerDismissed) { Text("Cancel") }
            },
        )
    }

    // FR-028: "Add to routine" vs "This workout only"
    if (uiState.pendingAddExerciseId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onAddToRoutineDecision(false) },
            title = { Text("Add to routine?") },
            text = { Text("Also add this exercise to the routine for future workouts?") },
            confirmButton = {
                TextButton(onClick = { viewModel.onAddToRoutineDecision(true) }) {
                    Text("Add to routine")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAddToRoutineDecision(false) }) {
                    Text("This workout only")
                }
            },
        )
    }

    // FR-033: finish confirmation with duration/exercises/sets/volume
    if (uiState.showFinishConfirm) {
        val workout = uiState.activeWorkout
        val completedSets = workout?.exercises?.flatMap { it.sets }?.count { it.isCompleted } ?: 0
        ConfirmDialog(
            title = "Finish workout?",
            message = "Duration: ${formatElapsed(uiState.elapsedSeconds)}\n" +
                "Exercises: ${workout?.exercises?.size ?: 0}\n" +
                "Completed sets: $completedSets",
            confirmLabel = "Finish",
            dismissLabel = "Keep working out",
            onConfirm = viewModel::onFinishConfirmed,
            onDismiss = viewModel::onFinishDismissed,
        )
    }

    // FR-037: cancel confirmation — "Keep working out" / "Discard workout"
    if (uiState.showCancelConfirm) {
        ConfirmDialog(
            title = "Discard workout?",
            message = "Your progress will be permanently deleted.",
            confirmLabel = "Discard workout",
            dismissLabel = "Keep working out",
            onConfirm = viewModel::onDiscardConfirmed,
            onDismiss = viewModel::onKeepWorkingOut,
        )
    }
}

@Composable
private fun ExerciseCard(
    exercise: ActiveExercise,
    previousPerformance: PreviousPerformance?,
    onWeightChanged: (Long, String) -> Unit,
    onRepsChanged: (Long, String) -> Unit,
    onToggleComplete: (Long, Boolean) -> Unit,
    onAddSet: (Long) -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove ${exercise.exerciseName} from this workout")
                }
            }

            // FR-045: show previous performance header if available
            if (previousPerformance != null && previousPerformance.sets.isNotEmpty()) {
                Text(
                    text = "Previous: ${formatPreviousDate(previousPerformance.date)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            exercise.sets.forEachIndexed { index, set ->
                // FR-046: pre-fill from previous performance if available
                val prevSet = previousPerformance?.sets?.getOrNull(index)
                SetRow(
                    set = set,
                    preFillWeight = prevSet?.weight,
                    preFillReps = prevSet?.reps,
                    onWeightChanged = { onWeightChanged(set.id, it) },
                    onRepsChanged = { onRepsChanged(set.id, it) },
                    onToggleComplete = { onToggleComplete(set.id, it) },
                )
            }

            TextButton(onClick = { onAddSet(exercise.workoutExerciseId) }) {
                Text("Add set")
            }
        }
    }
}

@Composable
private fun SetRow(
    set: ActiveSet,
    preFillWeight: Double?,
    preFillReps: Int?,
    onWeightChanged: (String) -> Unit,
    onRepsChanged: (String) -> Unit,
    onToggleComplete: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "${set.setNumber}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(end = 4.dp),
        )
        OutlinedTextField(
            value = set.weight?.toString().orEmpty(),
            onValueChange = onWeightChanged,
            label = { Text("Weight") },
            placeholder = preFillWeight?.let { { Text(it.toString()) } },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = set.reps?.toString().orEmpty(),
            onValueChange = onRepsChanged,
            label = { Text("Reps") },
            placeholder = preFillReps?.let { { Text(it.toString()) } },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        // FR-045: show previous performance value beside today's set
        if (preFillWeight != null || preFillReps != null) {
            Column(modifier = Modifier.padding(start = 4.dp)) {
                if (preFillWeight != null) {
                    Text(
                        text = "${preFillWeight}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (preFillReps != null) {
                    Text(
                        text = "${preFillReps} reps",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        // FR-025 / FR-061: completion marked with a check icon, not color alone.
        IconButton(onClick = { onToggleComplete(!set.isCompleted) }) {
            Icon(
                Icons.Filled.Check,
                contentDescription = if (set.isCompleted) "Mark set incomplete" else "Complete set",
                tint = if (set.isCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )
        }
    }
}

/** 00:00:00 format (FR-021). */
internal fun formatElapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

/** Short date format for previous performance label (FR-045). */
private fun formatPreviousDate(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
