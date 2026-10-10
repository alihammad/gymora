package com.gymora.ui.workout

import androidx.compose.ui.unit.sp
import com.gymora.ui.components.ActionButton
import com.gymora.ui.components.GymBackdrops
import com.gymora.ui.components.gymBackdrop
import com.gymora.ui.components.AngularPanel
import com.gymora.ui.components.HeroCard
import com.gymora.ui.components.MuscleChip
import com.gymora.ui.components.SectionHeader
import com.gymora.ui.theme.DisplayHero
import com.gymora.ui.theme.LabelCaps
import com.gymora.ui.components.GymoraLoading
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import com.gymora.ui.components.Button
import com.gymora.ui.components.Card
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.domain.model.ActiveExercise
import com.gymora.domain.model.ActiveSet
import com.gymora.domain.model.PreviousPerformance
import com.gymora.domain.model.SetValue
import com.gymora.domain.model.SupersetRules
import com.gymora.domain.calculator.SetFormat
import com.gymora.domain.calculator.SetSummary
import com.gymora.domain.calculator.SetLabels
import com.gymora.ui.components.ExerciseMediaImage
import com.gymora.ui.components.FormCuesList
import com.gymora.ui.components.LocalWeightUnit
import com.gymora.ui.components.SetEntry
import com.gymora.ui.components.SetInputRow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.saveable.rememberSaveable
import com.gymora.ui.components.ConfirmDialog
import com.gymora.ui.components.ExerciseBlock
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
    onBack: () -> Unit = {},
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

    // The exercise in focus: the user's tap, else the first one with sets left to log.
    var focusedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val allExercises = uiState.activeWorkout?.exercises.orEmpty()
    val current = allExercises.firstOrNull { it.workoutExerciseId == focusedId }
        ?: allExercises.firstOrNull { ex -> ex.sets.any { !it.isCompleted } }
        ?: allExercises.lastOrNull()
    val nextSet = current?.sets?.firstOrNull { !it.isCompleted }

    Scaffold(
        bottomBar = {
            if (current != null) {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RestTimerBar(
                        state = uiState.restTimer,
                        onSkip = viewModel::onRestTimerSkip,
                        onAdd30s = viewModel::onRestTimerAdd30s,
                        onRestart = viewModel::onRestTimerRestart,
                    )
                    // Primary action: complete the next set, or add one when all are logged.
                    ActionButton(
                        text = if (nextSet != null) "Log set" else "Add set",
                        onClick = {
                            if (nextSet != null) viewModel.onToggleComplete(nextSet.id, true)
                            else viewModel.onAddSet(current.workoutExerciseId)
                        },
                    )
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.activeWorkout?.session?.routineNameSnapshot ?: "Workout")
                        Text(
                            text = formatElapsed(uiState.elapsedSeconds),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (uiState.isLoading) {
            GymoraLoading(modifier = Modifier.padding(innerPadding).padding(16.dp))
        } else {
            val workout = uiState.activeWorkout
            if (workout == null) {
                Text(
                    text = "Workout not found.",
                    modifier = Modifier.padding(innerPadding).padding(16.dp),
                )
            } else {
                Column(modifier = Modifier.fillMaxSize().gymBackdrop(GymBackdrops.Workout).padding(innerPadding)) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                    if (current != null) {
                        item(key = "current-exercise") {
                            CurrentExerciseHero(
                                exercise = current,
                                index = allExercises.indexOf(current),
                                total = allExercises.size,
                                nextSet = nextSet,
                                previousPerformance = current.exerciseId?.let { uiState.previousPerformanceMap[it] },
                                onSetValuesChanged = viewModel::onSetValuesChanged,
                            )
                        }
                        item(key = "all-header") { SectionHeader("All exercises") }
                    }
                    val blocks = SupersetRules.blocks(workout.exercises) { it.supersetGroup }
                    items(blocks, key = { it.first().workoutExerciseId }) { block ->
                        ExerciseBlock(block) { exercise ->
                            if (exercise.workoutExerciseId == current?.workoutExerciseId) {
                                ExerciseCard(
                                    exercise = exercise,
                                    previousPerformance = exercise.exerciseId?.let {
                                        uiState.previousPerformanceMap[it]
                                    },
                                    hideSetId = nextSet?.id,
                                    onSetValuesChanged = viewModel::onSetValuesChanged,
                                    onToggleComplete = viewModel::onToggleComplete,
                                    onAddSet = viewModel::onAddSet,
                                    onRemove = { viewModel.onRemoveExercise(exercise.workoutExerciseId) },
                                )
                            } else {
                                ExerciseSummaryRow(
                                    exercise = exercise,
                                    onFocus = { focusedId = exercise.workoutExerciseId },
                                    onRemove = { viewModel.onRemoveExercise(exercise.workoutExerciseId) },
                                )
                            }
                        }
                    }
                    item {
                        OutlinedButton(
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
                    item {
                        com.gymora.ui.components.TextButton(
                            onClick = viewModel::onCancelRequested,
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Text("Discard workout")
                        }
                    }
                    }
                }
            }
        }
    }

    // Exercise picker (FR-028)
    if (uiState.showExercisePicker) {
        var query by rememberSaveable { mutableStateOf("") }
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
                TextButton(onClick = viewModel::onExercisePickerDismissed) { Text("Cancel") }
            },
        )
    }

    // FR-028: "Add to routine" vs "This workout only"
    if (uiState.pendingAddExerciseId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onAddToRoutineDecision(false) },
            title = { Text("Add to workout?") },
            text = { Text("Also add this exercise to the workout for future sessions?") },
            confirmButton = {
                TextButton(onClick = { viewModel.onAddToRoutineDecision(true) }) {
                    Text("Add to workout")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAddToRoutineDecision(false) }) {
                    Text("This session only")
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

/** Focus panel for the exercise being trained: huge name, set position and big weight/reps entry. */
@Composable
private fun CurrentExerciseHero(
    exercise: ActiveExercise,
    index: Int,
    total: Int,
    nextSet: ActiveSet?,
    previousPerformance: PreviousPerformance?,
    onSetValuesChanged: (Long, SetEntry) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val displayUnit = LocalWeightUnit.current
    val setIndex = exercise.sets.indexOfFirst { it.id == nextSet?.id }
    val previous = previousPerformance?.sets?.getOrNull(setIndex.coerceAtLeast(0))
    HeroCard {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("EXERCISE ${index + 1} OF $total", style = LabelCaps, color = scheme.primary)
            Text(
                text = exercise.exerciseName.uppercase(),
                style = DisplayHero.copy(fontSize = 46.sp, lineHeight = 45.sp),
                color = Color.White,
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            val done = exercise.sets.count { it.isCompleted }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                MuscleChip(
                    text = if (nextSet != null) "SET ${setIndex + 1} OF ${exercise.sets.size}" else "ALL SETS DONE",
                    selected = true,
                )
                if (previous != null) {
                    Text(
                        text = "LAST: " + SetSummary.describe(
                            nextSet?.measurementType ?: exercise.sets.first().measurementType,
                            previous.weight, previous.weightUnit, previous.reps,
                            previous.durationSeconds, previous.distanceMeters, displayUnit,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
            if (nextSet != null) {
                SetInputRow(
                    key = nextSet.id,
                    type = nextSet.measurementType,
                    initial = SetEntry(nextSet.weight, nextSet.reps, nextSet.durationSeconds, nextSet.distanceMeters),
                    hint = previous?.let { SetEntry(it.weight, it.reps, it.durationSeconds, it.distanceMeters) },
                    onChange = { onSetValuesChanged(nextSet.id, it) },
                    large = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = "$done sets logged. Add another set or move to the next exercise.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

/** Collapsed row for an exercise that is not in focus; tap to bring it into focus. */
@Composable
private fun ExerciseSummaryRow(
    exercise: ActiveExercise,
    onFocus: () -> Unit,
    onRemove: () -> Unit,
) {
    val done = exercise.sets.count { it.isCompleted }
    val complete = exercise.sets.isNotEmpty() && done == exercise.sets.size
    AngularPanel(modifier = Modifier.fillMaxWidth(), onClick = onFocus) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (complete) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.exerciseName.uppercase(), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "$done of ${exercise.sets.size} sets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove ${exercise.exerciseName} from this workout")
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: ActiveExercise,
    previousPerformance: PreviousPerformance?,
    hideSetId: Long?,
    onSetValuesChanged: (Long, SetEntry) -> Unit,
    onToggleComplete: (Long, Boolean) -> Unit,
    onAddSet: (Long) -> Unit,
    onRemove: () -> Unit,
) {
    var showGuide by rememberSaveable(exercise.workoutExerciseId) { mutableStateOf(false) }
    val hasGuide = exercise.formCues.isNotEmpty() || exercise.mediaFile != null
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
                if (hasGuide) {
                    IconButton(onClick = { showGuide = !showGuide }) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = if (showGuide) "Hide form guide" else "Show form guide",
                            tint = if (showGuide) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                        )
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove ${exercise.exerciseName} from this workout")
                }
            }

            if (showGuide) FormGuide(exercise)

            // FR-045: show previous performance header if available
            if (previousPerformance != null && previousPerformance.sets.isNotEmpty()) {
                Text(
                    text = "Previous: ${formatPreviousDate(previousPerformance.date)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            val labels = SetLabels.of(exercise.sets)
            exercise.sets.forEachIndexed { index, set ->
                // The next set to log is edited in the hero above; showing it twice would desync.
                if (set.id == hideSetId) return@forEachIndexed
                SetRow(
                    set = set,
                    label = labels[index],
                    // FR-046: pre-fill from previous performance if available
                    previous = previousPerformance?.sets?.getOrNull(index),
                    onValuesChanged = { onSetValuesChanged(set.id, it) },
                    onToggleComplete = { onToggleComplete(set.id, it) },
                )
            }

            TextButton(onClick = { onAddSet(exercise.workoutExerciseId) }) {
                Text(if (exercise.sets.any { it.side != null }) "Add set (both sides)" else "Add set")
            }
        }
    }
}

/** Demo media and form cues of the exercise. */
@Composable
private fun FormGuide(exercise: ActiveExercise) {
    Column(
        modifier = Modifier.padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        exercise.mediaFile?.let { ExerciseMediaImage(it, "${exercise.exerciseName} demo") }
        if (exercise.formCues.isNotEmpty()) FormCuesList(exercise.formCues)
    }
}

@Composable
private fun SetRow(
    set: ActiveSet,
    label: String,
    previous: SetValue?,
    onValuesChanged: (SetEntry) -> Unit,
    onToggleComplete: (Boolean) -> Unit,
) {
    val displayUnit = LocalWeightUnit.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (set.isCompleted) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                } else {
                    Color.Transparent
                },
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(end = 4.dp),
            )
            SetInputRow(
                key = set.id,
                type = set.measurementType,
                initial = SetEntry(set.weight, set.reps, set.durationSeconds, set.distanceMeters),
                hint = previous?.let { SetEntry(it.weight, it.reps, it.durationSeconds, it.distanceMeters) },
                onChange = onValuesChanged,
                modifier = Modifier.weight(1f),
            )
            // FR-025 / FR-061: completion marked with a check icon, not color alone.
            // Rounded checkbox: filled accent + check when done, muted outline otherwise.
            val done = set.isCompleted
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (done) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    )
                    .toggleable(
                        value = done,
                        role = Role.Checkbox,
                        onValueChange = { onToggleComplete(it) },
                    ),
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = if (done) "Mark set incomplete" else "Complete set",
                    tint = if (done) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            }
        }
        // FR-045: show previous performance value beside today's set
        if (previous != null) {
            Text(
                text = "Last: " + SetSummary.describe(
                    set.measurementType, previous.weight, previous.weightUnit, previous.reps,
                    previous.durationSeconds, previous.distanceMeters, displayUnit,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 28.dp),
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
