package com.gymora.ui.routines

import com.gymora.ui.components.GymoraLoading
import com.gymora.ui.theme.GymoraShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import com.gymora.ui.components.Button
import com.gymora.ui.components.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import com.gymora.ui.components.OutlinedButton
import com.gymora.ui.components.OutlinedTextField
import androidx.compose.material3.Scaffold
import com.gymora.ui.components.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import com.gymora.ui.components.TextButton
import com.gymora.ui.components.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.domain.model.RoutineDetail
import com.gymora.domain.model.RoutineExerciseDetail
import com.gymora.domain.model.SetTemplate
import com.gymora.ui.components.ChartPoint
import com.gymora.ui.components.ConfirmDialog
import com.gymora.ui.components.ProgressChartCard
import com.gymora.ui.theme.OnTileAccents
import com.gymora.ui.theme.TileAccents
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Routine detail/editor (FR-011..FR-018): shows the routine name, its
 * exercises with sets and reps and a volume progress chart. The name and
 * description are edited through the pencil action; sets and exercises are
 * edited inline. Duplicate and delete live in the top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(
    onBack: () -> Unit,
    viewModel: RoutineEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditDetails by remember { mutableStateOf(false) }

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
                title = {
                    Text(
                        uiState.routine?.header?.name ?: "Routine",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDetails = true }, enabled = uiState.routine != null) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit name and description")
                    }
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
            GymoraLoading(modifier = Modifier.padding(innerPadding).padding(16.dp))
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
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    RoutineHeader(routine)

                    ProgressChartCard(
                        title = "Training volume per workout",
                        unit = "kg",
                        points = remember(uiState.progress) {
                            val fmt = DateTimeFormatter.ofPattern("d MMM")
                            uiState.progress.map {
                                ChartPoint(
                                    label = fmt.format(it.date.atZone(ZoneId.systemDefault())),
                                    value = it.volumeKg,
                                )
                            }
                        },
                    )

                    Text("Exercises", style = MaterialTheme.typography.titleMedium)
                    routine.exercises.forEach { exercise ->
                        ExerciseCard(
                            exercise = exercise,
                            onAddSet = { viewModel.onAddSetTemplate(exercise.routineExerciseId) },
                            onRemove = { viewModel.onRemoveExercise(exercise.routineExerciseId) },
                            onUpdateTemplate = viewModel::onUpdateSetTemplate,
                            onDeleteTemplate = viewModel::onDeleteSetTemplate,
                        )
                    }

                    OutlinedButton(
                        onClick = viewModel::onAddExerciseClicked,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Add exercise")
                    }
                }
            }
        }
    }

    if (showEditDetails) {
        uiState.routine?.let { routine ->
            EditDetailsDialog(
                initialName = routine.header.name,
                initialDescription = routine.header.description.orEmpty(),
                onSave = { name, description ->
                    viewModel.onRename(name)
                    viewModel.onDescriptionChanged(description)
                    showEditDetails = false
                },
                onDismiss = { showEditDetails = false },
            )
        }
    }

    if (uiState.showExercisePicker) {
        var query by remember { mutableStateOf("") }
        val filtered = remember(uiState.libraryExercises, query) {
            val q = query.trim()
            if (q.isEmpty()) {
                uiState.libraryExercises
            } else {
                uiState.libraryExercises.filter {
                    it.name.contains(q, ignoreCase = true) ||
                        it.muscleGroup?.displayName?.contains(q, ignoreCase = true) == true
                }
            }
        }
        AlertDialog(
            onDismissRequest = viewModel::onExercisePickerDismissed,
            title = { Text("Add exercise") },
            text = {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search exercises") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (filtered.isEmpty()) {
                        Text(
                            "No exercises match \"${query.trim()}\".",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    } else {
                        LazyColumn {
                            items(filtered, key = { it.id }) { exercise ->
                                ListItem(
                                    headlineContent = { Text(exercise.name) },
                                    supportingContent = {
                                        Text(exercise.muscleGroup?.displayName ?: "")
                                    },
                                    trailingContent = {
                                        IconButton(onClick = { viewModel.onExercisePicked(exercise.id) }) {
                                            Icon(Icons.Filled.Add, contentDescription = "Add ${exercise.name}")
                                        }
                                    },
                                    modifier = Modifier.clickable { viewModel.onExercisePicked(exercise.id) },
                                )
                            }
                        }
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
private fun RoutineHeader(routine: RoutineDetail) {
    val accentIndex = (routine.header.id % TileAccents.size).toInt()
    val accent = TileAccents[accentIndex]
    val sets = routine.exercises.sumOf { it.setTemplates.size }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(56.dp).background(accent, CircleShape),
        ) {
            Icon(
                routineIcon(routine.header.name),
                contentDescription = null,
                tint = OnTileAccents[accentIndex],
                modifier = Modifier.size(28.dp),
            )
        }
        Column {
            Text(
                "${routine.exercises.size} exercises · $sets sets",
                style = MaterialTheme.typography.titleMedium,
            )
            routine.header.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EditDetailsDialog(
    initialName: String,
    initialDescription: String,
    onSave: (name: String, description: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit routine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = name.isBlank(),
                    supportingText = {
                        if (name.isBlank()) Text("Routine name must not be blank")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), description) }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ExerciseCard(
    exercise: RoutineExerciseDetail,
    onAddSet: () -> Unit,
    onRemove: () -> Unit,
    onUpdateTemplate: (Long, com.gymora.domain.model.SetTemplateInput) -> Unit,
    onDeleteTemplate: (Long) -> Unit,
) {
    Card(
        shape = GymoraShapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove ${exercise.exerciseName}")
                }
            }

            exercise.setTemplates.forEach { template ->
                SetTemplateRow(
                    template = template,
                    onUpdate = { input -> onUpdateTemplate(template.id, input) },
                    onDelete = { onDeleteTemplate(template.id) },
                )
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onAddSet,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Add set")
            }
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Set ${template.setNumber}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = "${template.targetReps} reps" +
                template.targetWeight?.let { " × $it ${template.weightUnit?.name?.lowercase().orEmpty()}" }.orEmpty(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { showEditDialog = true }) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit set ${template.setNumber}")
        }
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
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Target weight (optional)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                    ),
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
