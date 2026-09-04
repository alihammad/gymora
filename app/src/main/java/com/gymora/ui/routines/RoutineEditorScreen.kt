package com.gymora.ui.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.domain.model.RoutineExerciseDetail
import com.gymora.domain.model.SetTemplate
import com.gymora.ui.components.ConfirmDialog

/**
 * Routine detail/editor (FR-011..FR-018): rename, description, add/remove/
 * reorder exercises, per-exercise notes, set template CRUD, duplicate,
 * delete-with-confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(
    onBack: () -> Unit,
    viewModel: RoutineEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onBack()
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
                title = { Text(uiState.routine?.header?.name ?: "Routine") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onDuplicate) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate routine")
                    }
                    IconButton(onClick = viewModel::onDeleteRequested) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete routine")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(innerPadding).padding(16.dp))
        } else {
            val routine = uiState.routine
            if (routine == null) {
                Text(
                    text = "Routine not found.",
                    modifier = Modifier.padding(innerPadding).padding(16.dp),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = routine.header.name,
                        onValueChange = viewModel::onRename,
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = routine.header.description.orEmpty(),
                        onValueChange = { viewModel.onDescriptionChanged(it) },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text("Exercises", style = MaterialTheme.typography.titleMedium)
                    routine.exercises.forEachIndexed { index, exercise ->
                        ExerciseRow(
                            exercise = exercise,
                            onAddSet = { viewModel.onAddSetTemplate(exercise.routineExerciseId) },
                            onRemove = { viewModel.onRemoveExercise(exercise.routineExerciseId) },
                            onNotesChanged = { notes ->
                                viewModel.onExerciseNotesChanged(exercise.routineExerciseId, notes)
                            },
                            onUpdateTemplate = viewModel::onUpdateSetTemplate,
                            onDeleteTemplate = viewModel::onDeleteSetTemplate,
                        )
                    }

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

    if (uiState.showExercisePicker) {
        AlertDialog(
            onDismissRequest = viewModel::onExercisePickerDismissed,
            title = { Text("Add exercise") },
            text = {
                LazyColumn {
                    items(uiState.libraryExercises, key = { it.id }) { exercise ->
                        ListItem(
                            headlineContent = { Text(exercise.name) },
                            supportingContent = {
                                Text(exercise.muscleGroup?.name?.lowercase() ?: "")
                            },
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
                TextButton(onClick = viewModel::onExercisePickerDismissed) {
                    Text("Cancel")
                }
            },
        )
    }

    if (uiState.showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete routine",
            message = "Delete \"${uiState.routine?.header?.name}\"? " +
                "Workouts already performed from it stay in history.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            onConfirm = viewModel::onDeleteConfirmed,
            onDismiss = viewModel::onDeleteDismissed,
        )
    }
}

@Composable
private fun ExerciseRow(
    exercise: RoutineExerciseDetail,
    onAddSet: () -> Unit,
    onRemove: () -> Unit,
    onNotesChanged: (String) -> Unit,
    onUpdateTemplate: (Long, com.gymora.domain.model.SetTemplateInput) -> Unit,
    onDeleteTemplate: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = exercise.exerciseName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove ${exercise.exerciseName}")
            }
        }

        OutlinedTextField(
            value = exercise.notes.orEmpty(),
            onValueChange = onNotesChanged,
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )

        exercise.setTemplates.forEach { template ->
            SetTemplateRow(
                template = template,
                onUpdate = { input -> onUpdateTemplate(template.id, input) },
                onDelete = { onDeleteTemplate(template.id) },
            )
        }

        TextButton(onClick = onAddSet) {
            Text("Add set")
        }
    }
}

@Composable
private fun SetTemplateRow(
    template: SetTemplate,
    onUpdate: (com.gymora.domain.model.SetTemplateInput) -> Unit,
    onDelete: () -> Unit,
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Set ${template.setNumber}: ${template.targetReps} reps" +
                template.targetWeight?.let { " × $it" }.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { showEditDialog = true }) { Text("Edit") }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete set ${template.setNumber}")
        }
    }

    if (showEditDialog) {
        SetTemplateEditDialog(
            template = template,
            onSave = { input ->
                onUpdate(input)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false },
        )
    }
}

@Composable
private fun SetTemplateEditDialog(
    template: SetTemplate,
    onSave: (com.gymora.domain.model.SetTemplateInput) -> Unit,
    onDismiss: () -> Unit,
) {
    var repsText by remember { mutableStateOf(template.targetReps.toString()) }
    var weightText by remember { mutableStateOf(template.targetWeight?.toString().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit set ${template.setNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { repsText = it },
                    label = { Text("Target reps") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Target weight (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val reps = repsText.toIntOrNull()
                    val weight = weightText.toDoubleOrNull()
                    if (isValidTemplate(reps, weight)) {
                        onSave(
                            com.gymora.domain.model.SetTemplateInput(
                                targetReps = reps!!,
                                targetWeight = weight,
                                weightUnit = template.weightUnit,
                                measurementType = template.measurementType,
                            ),
                        )
                    }
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun isValidTemplate(reps: Int?, weight: Double?): Boolean =
    reps != null && reps >= 0 && (weight == null || weight >= 0)
