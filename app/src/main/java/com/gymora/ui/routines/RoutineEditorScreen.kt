package com.gymora.ui.routines

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.domain.model.RoutineDetail
import com.gymora.domain.model.RoutineExerciseDetail
import com.gymora.domain.model.SetTemplate
import com.gymora.domain.model.SupersetRules
import com.gymora.domain.calculator.SetFormat
import com.gymora.domain.calculator.SetSummary
import com.gymora.domain.model.MeasurementType
import com.gymora.domain.model.SetField
import com.gymora.ui.components.LocalWeightUnit
import com.gymora.ui.components.SetEntry
import com.gymora.ui.components.SetInputRow
import com.gymora.ui.components.ConfirmDialog
import com.gymora.ui.components.SupersetBlock
import com.gymora.ui.components.SupersetLinkButton
import com.gymora.ui.components.ChartPoint
import com.gymora.ui.components.ProgressChartCard
import com.gymora.ui.theme.GymoraThemeTokens
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Routine detail/editor (FR-011..FR-018): shows the routine name, its
 * exercises with sets and reps and a volume progress chart. The name and
 * description are edited inline and persisted with Save; sets and exercises
 * are edited inline. The top-bar delete action removes the workout after
 * confirmation; history performed from it is kept.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(
    onBack: () -> Unit,
    viewModel: RoutineEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val exit = { viewModel.onExit(onBack) }

    BackHandler(onBack = exit)

    uiState.message?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.routine?.header?.name ?: "Workout",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = exit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.routine != null) {
                        IconButton(onClick = viewModel::onDeleteRequested) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete workout")
                        }
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
                    text = "Workout not found.",
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
                    WorkoutDetailsForm(
                        routine = routine,
                        isNewWorkout = uiState.isNewWorkout,
                        onSave = viewModel::onSaveDetails,
                    )

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
                    ExerciseList(
                        exercises = routine.exercises,
                        viewModel = viewModel,
                    )

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

    if (uiState.showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete workout",
            message = "Delete \"${uiState.routine?.header?.name.orEmpty()}\"? " +
                "Workouts already performed from it stay in history.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            onConfirm = { viewModel.onDeleteConfirmed(onBack) },
            onDismiss = viewModel::onDeleteDismissed,
        )
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
}

@Composable
private fun RoutineHeader(routine: RoutineDetail) {
    val extra = GymoraThemeTokens.extraColors
    val accentIndex = (routine.header.id % extra.tileAccents.size).toInt()
    val accent = extra.tileAccents[accentIndex]
    val sets = routine.exercises.sumOf { it.setTemplates.size }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(56.dp).background(accent, CircleShape),
        ) {
            Icon(
                painterResource(routineIcon(routine.header.name)),
                contentDescription = null,
                tint = extra.onTileAccents[accentIndex],
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            "${routine.exercises.size} exercises · $sets sets",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/**
 * Inline name/description fields. A freshly created workout starts with an
 * empty, focused name field so the placeholder "New Workout" is not kept by
 * accident.
 */
@Composable
private fun WorkoutDetailsForm(
    routine: RoutineDetail,
    isNewWorkout: Boolean,
    onSave: (name: String, description: String) -> Unit,
) {
    val savedName = routine.header.name
    val savedDescription = routine.header.description.orEmpty()
    var name by rememberSaveable(routine.header.id) {
        mutableStateOf(if (isNewWorkout) "" else savedName)
    }
    var description by rememberSaveable(routine.header.id) { mutableStateOf(savedDescription) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(routine.header.id) {
        if (isNewWorkout) focusRequester.requestFocus()
    }

    val hasChanges = isNewWorkout || name.trim() != savedName || description != savedDescription
    val canSave = name.isNotBlank() && hasChanges
    val save = {
        focusManager.clearFocus()
        onSave(name.trim(), description)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Workout name") },
            placeholder = { Text("e.g. Push Day") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = save,
            enabled = canSave,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text("Save")
        }
    }
}

/**
 * Exercises grouped into supersets. Between every two adjacent exercises sits a
 * toggle that links them into a superset or splits an existing one there.
 */
@Composable
private fun ExerciseList(
    exercises: List<RoutineExerciseDetail>,
    viewModel: RoutineEditorViewModel,
) {
    val blocks = SupersetRules.blocks(exercises) { it.supersetGroup }
    val card: @Composable (RoutineExerciseDetail) -> Unit = { exercise ->
        ExerciseCard(
            exercise = exercise,
            onAddSet = { viewModel.onAddSetTemplate(exercise.routineExerciseId) },
            onRemove = { viewModel.onRemoveExercise(exercise.routineExerciseId) },
            onUpdateTemplate = viewModel::onUpdateSetTemplate,
            onDeleteTemplate = viewModel::onDeleteSetTemplate,
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        blocks.forEachIndexed { blockIndex, block ->
            if (block.size > 1) {
                SupersetBlock {
                    block.forEachIndexed { index, exercise ->
                        card(exercise)
                        if (index < block.lastIndex) {
                            SupersetLinkButton(
                                linked = true,
                                onClick = { viewModel.onUnlinkSuperset(exercise.routineExerciseId) },
                            )
                        }
                    }
                }
            } else {
                card(block.single())
            }
            if (blockIndex < blocks.lastIndex) {
                SupersetLinkButton(
                    linked = false,
                    onClick = { viewModel.onLinkSuperset(block.last().routineExerciseId) },
                )
            }
        }
    }
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (exercise.isUnilateral) {
                        Text(
                            text = "Each set is done per side",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove ${exercise.exerciseName}")
                }
            }

            exercise.setTemplates.forEach { template ->
                SetTemplateRow(
                    template = template,
                    type = exercise.measurementType,
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
    type: MeasurementType,
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
            text = SetSummary.describe(
                type, template.targetWeight, template.weightUnit, template.targetReps.takeIf { it > 0 },
                template.targetDurationSeconds, template.targetDistanceMeters, LocalWeightUnit.current,
            ),
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
            type = type,
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
    type: MeasurementType,
    onSave: (com.gymora.domain.model.SetTemplateInput) -> Unit,
    onDismiss: () -> Unit,
) {
    var entry by remember {
        mutableStateOf(
            SetEntry(
                weight = template.targetWeight,
                reps = template.targetReps.takeIf { it > 0 || type.has(SetField.REPS) },
                durationSeconds = template.targetDurationSeconds,
                distanceMeters = template.targetDistanceMeters,
            ),
        )
    }
    val weightUnit = template.weightUnit ?: LocalWeightUnit.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit set ${template.setNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Targets (${type.displayName.lowercase()})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SetInputRow(
                    key = template.id,
                    type = type,
                    initial = entry,
                    onChange = { entry = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        com.gymora.domain.model.SetTemplateInput(
                            targetReps = entry.reps ?: 0,
                            targetWeight = entry.weight,
                            weightUnit = entry.weight?.let { weightUnit },
                            measurementType = type,
                            targetDurationSeconds = entry.durationSeconds,
                            targetDistanceMeters = entry.distanceMeters,
                        ),
                    )
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
