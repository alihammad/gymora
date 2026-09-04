package com.gymora.ui.history

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.gymora.domain.model.ActiveExercise
import com.gymora.domain.model.Exercise
import com.gymora.ui.workout.formatElapsed
import java.time.format.DateTimeFormatter

/**
 * Read-only historical workout detail (FR-041): every exercise and set exactly
 * as performed, rendered from snapshot rows (FR-043, FR-056).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    onBack: () -> Unit,
    onExerciseHistory: (Long) -> Unit = {},
    viewModel: WorkoutDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.detail?.session?.routineNameSnapshot ?: "Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.detail != null && !uiState.isLoading) {
                        if (uiState.isEditing) {
                            TextButton(onClick = viewModel::save) { Text("Save") }
                            TextButton(onClick = viewModel::cancelEdit) { Text("Cancel") }
                        } else {
                            // FR-042: history is read-only unless Edit is explicitly chosen.
                            IconButton(onClick = viewModel::enterEditMode) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit workout")
                            }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(innerPadding).padding(16.dp))
        } else {
            val detail = uiState.detail
            if (detail == null) {
                Text(
                    text = uiState.errorMessage ?: "Workout not found.",
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
                    item {
                        Column {
                            Text(
                                text = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
                                    .format(detail.session.startedAt.atZone(java.time.ZoneId.systemDefault())),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            detail.session.endedAt?.let { ended ->
                                Text(
                                    text = "Duration: ${formatElapsed(java.time.Duration.between(detail.session.startedAt, ended).seconds)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    // Workout-level notes: read-only in view mode, editable in edit mode.
                    if (uiState.isEditing || detail.session.notes != null) {
                        item {
                            WorkoutNotesSection(
                                notes = detail.session.notes,
                                isEditing = uiState.isEditing,
                                editableNotes = uiState.editableNotes,
                                onNotesChanged = viewModel::onWorkoutNotesChanged,
                            )
                        }
                    }
                    items(detail.exercises, key = { it.workoutExerciseId }) { exercise ->
                        if (uiState.isEditing) {
                            EditableExerciseCard(
                                exercise = exercise,
                                editableSets = uiState.editableSets,
                                onWeightChanged = viewModel::onWeightChanged,
                                onRepsChanged = viewModel::onRepsChanged,
                                onSetNotesChanged = viewModel::onSetNotesChanged,
                                onToggleComplete = viewModel::onToggleComplete,
                                onRemove = { viewModel.onRemoveExerciseClicked(exercise.workoutExerciseId) },
                            )
                        } else {
                            HistoricalExerciseCard(
                                exercise = exercise,
                                onExerciseHistory = { exercise.exerciseId?.let(onExerciseHistory) },
                            )
                        }
                    }
                    if (uiState.isEditing) {
                        item {
                            Button(
                                onClick = viewModel::onAddExerciseClicked,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Text("Add exercise")
                            }
                        }
                    }
                }
            }
        }
    }

    // Add-exercise picker dialog (edit mode, FR-042).
    if (uiState.showAddExercise) {
        AlertDialog(
            onDismissRequest = viewModel::onAddExerciseDismissed,
            title = { Text("Add exercise") },
            text = {
                LazyColumn {
                    items(uiState.libraryExercises, key = { it.id }) { exercise ->
                        TextButton(
                            onClick = { viewModel.onExerciseSelected(exercise.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(exercise.name, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = viewModel::onAddExerciseDismissed) { Text("Cancel") }
            },
        )
    }

    // Remove-exercise confirmation (edit mode, FR-042).
    uiState.pendingRemoveExerciseId?.let { id ->
        AlertDialog(
            onDismissRequest = viewModel::onRemoveExerciseDismissed,
            title = { Text("Remove exercise?") },
            text = { Text("This removes the exercise from this workout only.") },
            confirmButton = {
                TextButton(onClick = viewModel::onRemoveExerciseConfirmed) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onRemoveExerciseDismissed) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun WorkoutNotesSection(
    notes: String?,
    isEditing: Boolean,
    editableNotes: String,
    onNotesChanged: (String) -> Unit,
) {
    if (isEditing) {
        OutlinedTextField(
            value = editableNotes,
            onValueChange = onNotesChanged,
            label = { Text("Workout notes") },
            modifier = Modifier.fillMaxWidth(),
        )
    } else if (!notes.isNullOrBlank()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(text = notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun HistoricalExerciseCard(
    exercise: ActiveExercise,
    onExerciseHistory: () -> Unit = {},
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
                if (exercise.exerciseId != null) {
                    IconButton(onClick = onExerciseHistory) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = "View history for ${exercise.exerciseName}",
                        )
                    }
                }
            }
            exercise.sets.forEach { set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Set ${set.setNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(0.3f),
                    )
                    Text(
                        text = "${set.weight?.toString() ?: "—"} × ${set.reps?.toString() ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(0.5f),
                    )
                    if (set.isCompleted) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableExerciseCard(
    exercise: ActiveExercise,
    editableSets: Map<Long, EditableSet>,
    onWeightChanged: (Long, String) -> Unit,
    onRepsChanged: (Long, String) -> Unit,
    onSetNotesChanged: (Long, String) -> Unit,
    onToggleComplete: (Long) -> Unit,
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
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Remove ${exercise.exerciseName} from this workout",
                    )
                }
            }
            exercise.sets.forEach { set ->
                val editable = editableSets[set.id] ?: return@forEach
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Set ${set.setNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(0.25f),
                        )
                        OutlinedTextField(
                            value = editable.weight,
                            onValueChange = { onWeightChanged(set.id, it) },
                            label = { Text("Weight") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = editable.reps,
                            onValueChange = { onRepsChanged(set.id, it) },
                            label = { Text("Reps") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onToggleComplete(set.id) }) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = if (editable.isCompleted) {
                                    "Mark set incomplete"
                                } else {
                                    "Complete set"
                                },
                                tint = if (editable.isCompleted) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = editable.notes,
                        onValueChange = { onSetNotesChanged(set.id, it) },
                        label = { Text("Set notes") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
