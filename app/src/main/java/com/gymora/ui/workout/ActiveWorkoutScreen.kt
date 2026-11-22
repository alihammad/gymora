package com.gymora.ui.workout

import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.unit.sp
import com.gymora.ui.theme.gymoraStyle
import com.gymora.ui.components.GymBackdrops
import com.gymora.ui.components.gymBackdrop
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.gymora.ui.components.SectionHeader
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapVert
import com.gymora.ui.components.ReorderExerciseRow
import com.gymora.ui.components.ReorderableList
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
import com.gymora.domain.model.SetField
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
import com.gymora.ui.components.SupersetLinkButton
import com.gymora.ui.components.ExercisePickerDialog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Active workout screen (FR-022..FR-037): timestamp-derived timer, per-set
 * weight/reps entry with immediate saving, add set/exercise, skip/remove
 * exercise, finish with confirmation, cancel with confirmation.
 * Optimized for fast gym use: large targets, minimal typing (FR-022, FR-062).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    var reordering by rememberSaveable { mutableStateOf(false) }
    // Exercise awaiting delete confirmation, and the one a superset is being set up from.
    var pendingRemoveId by rememberSaveable { mutableStateOf<Long?>(null) }
    var supersetAnchorId by rememberSaveable { mutableStateOf<Long?>(null) }
    val allExercises = uiState.activeWorkout?.exercises.orEmpty()
    // Table layouts (Slate) log sets in a flat SET / PREVIOUS / values grid instead of cards.
    val table = gymoraStyle.tableSets
    val itemPadding = if (table) Modifier.padding(horizontal = 16.dp) else Modifier

    Scaffold(
        bottomBar = {
            if (allExercises.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        // Keep the bar (and the content padding Scaffold derives from it) above the
                        // system navigation bar and, while typing, above the keyboard — the app is
                        // edge-to-edge so the window doesn't resize for the IME on its own.
                        .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RestTimerBar(
                        state = uiState.restTimer,
                        onSkip = viewModel::onRestTimerSkip,
                        onAdd30s = viewModel::onRestTimerAdd30s,
                        onRestart = viewModel::onRestTimerRestart,
                        // Shrink to clock + Skip while the keyboard is up so the set rows stay visible.
                        compact = WindowInsets.isImeVisible,
                    )
                }
            }
        },
        topBar = {
            if (table) {
                TopAppBar(
                    title = {
                        Text(
                            text = formatElapsed(uiState.elapsedSeconds),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Minimise workout")
                        }
                    },
                    actions = {
                        if (reordering) {
                            IconButton(onClick = { reordering = false }) {
                                Icon(Icons.Filled.Check, contentDescription = "Done reordering")
                            }
                        }
                        com.gymora.ui.components.TextButton(onClick = viewModel::onFinishRequested) {
                            Text("FINISH", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp))
                        }
                    },
                )
                return@Scaffold
            }
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
                actions = {
                    if (allExercises.size > 1) {
                        IconButton(onClick = { reordering = !reordering }) {
                            Icon(
                                if (reordering) Icons.Filled.Check else Icons.Filled.SwapVert,
                                contentDescription = if (reordering) "Done reordering" else "Reorder exercises",
                            )
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
                            .padding(horizontal = if (table) 0.dp else 16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                    if (table) {
                        item(key = "workout-title") {
                            Column(modifier = itemPadding.padding(top = 12.dp, bottom = 4.dp)) {
                                Text(
                                    text = workout.session.routineNameSnapshot ?: "Workout",
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                                Text(
                                    text = formatElapsed(uiState.elapsedSeconds),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else if (allExercises.isNotEmpty() && !reordering) {
                        item(key = "all-header") { SectionHeader("All exercises") }
                    }
                    if (reordering) {
                        item(key = "reorder-list") {
                            Box(itemPadding) { ReorderableList(
                                items = workout.exercises,
                                key = { it.workoutExerciseId },
                                onMove = viewModel::onMoveExercise,
                            ) { exercise, handle, isDragging ->
                                ReorderExerciseRow(
                                    name = exercise.exerciseName,
                                    inSuperset = exercise.supersetGroup != null,
                                    handle = handle,
                                    isDragging = isDragging,
                                )
                            } }
                        }
                    }
                    val blocks = if (reordering) emptyList() else SupersetRules.blocks(workout.exercises) { it.supersetGroup }
                    val numbers = SupersetRules.numbers(blocks)
                    itemsIndexed(blocks, key = { _, block -> block.first().workoutExerciseId }) { index, block ->
                        // Every exercise stays expanded; nothing collapses.
                        Column {
                            ExerciseBlock(
                                block,
                                numbers[index],
                                onRemoveSuperset = { viewModel.onRemoveSuperset(it.workoutExerciseId) },
                            ) { exercise ->
                                if (table) {
                                    ExerciseTableSection(
                                        exercise = exercise,
                                        previousPerformance = exercise.exerciseId?.let {
                                            uiState.previousPerformanceMap[it]
                                        },
                                        canSuperset = allExercises.size > 1,
                                        onSetValuesChanged = viewModel::onSetValuesChanged,
                                        onToggleComplete = viewModel::onToggleComplete,
                                        onAddSet = viewModel::onAddSet,
                                        onDeleteSet = viewModel::onDeleteSet,
                                        onRemove = { pendingRemoveId = exercise.workoutExerciseId },
                                        onSupersetWith = { supersetAnchorId = exercise.workoutExerciseId },
                                        onLeaveSuperset = { viewModel.onLeaveSuperset(exercise.workoutExerciseId) },
                                        onReorder = { reordering = true },
                                    )
                                    return@ExerciseBlock
                                }
                                ExerciseCard(
                                    exercise = exercise,
                                    previousPerformance = exercise.exerciseId?.let {
                                        uiState.previousPerformanceMap[it]
                                    },
                                    canSuperset = allExercises.size > 1,
                                    onSetValuesChanged = viewModel::onSetValuesChanged,
                                    onToggleComplete = viewModel::onToggleComplete,
                                    onAddSet = viewModel::onAddSet,
                                    onDeleteSet = viewModel::onDeleteSet,
                                    onRemove = { pendingRemoveId = exercise.workoutExerciseId },
                                    onSupersetWith = { supersetAnchorId = exercise.workoutExerciseId },
                                    onLeaveSuperset = { viewModel.onLeaveSuperset(exercise.workoutExerciseId) },
                                    onReorder = { reordering = true },
                                )
                            }
                            // Same one-tap link as the routine editor, so exercises added mid-workout can be paired.
                            if (index < blocks.lastIndex) {
                                SupersetLinkButton(
                                    linked = false,
                                    onClick = { viewModel.onLinkSupersetWithNext(block.last().workoutExerciseId) },
                                    modifier = itemPadding.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                    if (table) {
                        item(key = "table-actions") {
                            Column(
                                modifier = itemPadding.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Button(
                                    onClick = viewModel::onAddExerciseClicked,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.primary,
                                    ),
                                ) {
                                    Text("ADD EXERCISES", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp))
                                }
                                Button(
                                    onClick = viewModel::onCancelRequested,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                ) {
                                    Text("CANCEL WORKOUT", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp))
                                }
                            }
                        }
                    }
                    if (!table) item {
                        OutlinedButton(
                            onClick = viewModel::onAddExerciseClicked,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text("Add exercise")
                        }
                    }
                    if (!table) item {
                        Button(
                            onClick = viewModel::onFinishRequested,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("FINISH WORKOUT")
                        }
                    }
                    if (!table) item {
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
        ExercisePickerDialog(
            exercises = uiState.libraryExercises,
            onPick = viewModel::onExercisePicked,
            onDismiss = viewModel::onExercisePickerDismissed,
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

    // Removing an exercise asks first; deleting a set does not.
    allExercises.firstOrNull { it.workoutExerciseId == pendingRemoveId }?.let { exercise ->
        ConfirmDialog(
            title = "Delete exercise?",
            message = "Remove ${exercise.exerciseName} and its sets from this workout?",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            onConfirm = {
                pendingRemoveId = null
                viewModel.onRemoveExercise(exercise.workoutExerciseId)
            },
            onDismiss = { pendingRemoveId = null },
        )
    }

    allExercises.firstOrNull { it.workoutExerciseId == supersetAnchorId }?.let { anchor ->
        SupersetPickerDialog(
            anchor = anchor,
            exercises = allExercises,
            onPick = { member ->
                supersetAnchorId = null
                viewModel.onCreateSuperset(anchor.workoutExerciseId, member.workoutExerciseId)
            },
            onDismiss = { supersetAnchorId = null },
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

/** Lists the other exercises to superset [anchor] with; members of its own superset are left out. */
@Composable
private fun SupersetPickerDialog(
    anchor: ActiveExercise,
    exercises: List<ActiveExercise>,
    onPick: (ActiveExercise) -> Unit,
    onDismiss: () -> Unit,
) {
    val candidates = exercises.filter {
        it.workoutExerciseId != anchor.workoutExerciseId &&
            (anchor.supersetGroup == null || it.supersetGroup != anchor.supersetGroup)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Superset ${anchor.exerciseName} with") },
        text = {
            if (candidates.isEmpty()) {
                Text("Every other exercise is already in this superset.")
            } else {
                LazyColumn {
                    items(candidates, key = { it.workoutExerciseId }) { exercise ->
                        ListItem(
                            headlineContent = { Text(exercise.exerciseName) },
                            supportingContent = if (exercise.supersetGroup != null) {
                                { Text("Leaves its current superset") }
                            } else {
                                null
                            },
                            modifier = Modifier.clickable { onPick(exercise) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExerciseCard(
    exercise: ActiveExercise,
    previousPerformance: PreviousPerformance?,
    canSuperset: Boolean,
    onSetValuesChanged: (Long, SetEntry) -> Unit,
    onToggleComplete: (Long, Boolean) -> Unit,
    onAddSet: (Long) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onRemove: () -> Unit,
    onSupersetWith: () -> Unit,
    onLeaveSuperset: () -> Unit,
    onReorder: () -> Unit,
) {
    var showGuide by rememberSaveable(exercise.workoutExerciseId) { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val hasGuide = exercise.formCues.isNotEmpty() || exercise.mediaFile != null
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Long-press the name for superset, reorder and delete options.
                Box(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuOpen = true
                                },
                                onLongClickLabel = "Exercise options",
                            )
                            .padding(vertical = 8.dp),
                    )
                    ExerciseOptionsMenu(
                        expanded = menuOpen,
                        onDismiss = { menuOpen = false },
                        canSuperset = canSuperset,
                        inSuperset = exercise.supersetGroup != null,
                        onSupersetWith = onSupersetWith,
                        onLeaveSuperset = onLeaveSuperset,
                        onReorder = onReorder,
                        onRemove = onRemove,
                    )
                }
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
                // Keyed so a deleted set's swipe state is not handed to the set that takes its place.
                key(set.id) {
                    SwipeToDeleteSet(onDelete = { onDeleteSet(set.id) }) {
                        SetRow(
                            set = set,
                            label = labels[index],
                            // FR-046: pre-fill from previous performance if available
                            previous = previousPerformance?.sets?.getOrNull(index),
                            onValuesChanged = { onSetValuesChanged(set.id, it) },
                            onToggleComplete = { onToggleComplete(set.id, it) },
                        )
                    }
                }
            }

            TextButton(onClick = { onAddSet(exercise.workoutExerciseId) }) {
                Text(if (exercise.sets.any { it.side != null }) "Add set (both sides)" else "Add set")
            }
        }
    }
}

/** Superset, reorder and delete options of one exercise. */
@Composable
internal fun ExerciseOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    canSuperset: Boolean,
    inSuperset: Boolean,
    onSupersetWith: () -> Unit,
    onLeaveSuperset: () -> Unit,
    onReorder: () -> Unit,
    onRemove: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (canSuperset) {
            DropdownMenuItem(
                text = { Text("Superset with…") },
                leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onSupersetWith()
                },
            )
        }
        if (inSuperset) {
            DropdownMenuItem(
                text = { Text("Remove from superset") },
                leadingIcon = { Icon(Icons.Filled.LinkOff, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onLeaveSuperset()
                },
            )
        }
        if (canSuperset) {
            DropdownMenuItem(
                text = { Text("Reorder exercises") },
                leadingIcon = { Icon(Icons.Filled.SwapVert, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onReorder()
                },
            )
        }
        DropdownMenuItem(
            text = { Text("Delete exercise") },
            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            onClick = {
                onDismiss()
                onRemove()
            },
        )
    }
}

/** Swipe a set right-to-left to delete it; no confirmation. */
@Composable
internal fun SwipeToDeleteSet(
    onDelete: () -> Unit,
    rowColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    shape: androidx.compose.ui.graphics.Shape? = RoundedCornerShape(14.dp),
    content: @Composable () -> Unit,
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) currentOnDelete()
            value == SwipeToDismissBoxValue.EndToStart
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (shape != null) Modifier.clip(shape) else Modifier)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 16.dp),
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete set",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        // Opaque like the card, so the delete background only shows where the row has moved.
        Box(modifier = Modifier.background(rowColor)) { content() }
    }
}

/** Demo media and form cues of the exercise. */
@Composable
internal fun FormGuide(exercise: ActiveExercise) {
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
                        // Unchecking is always allowed; checking needs a value.
                        enabled = done || set.hasLoggableValue(),
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

/** A set can only be checked once it has the value its measurement type needs (reps for rep-based sets). */
internal fun ActiveSet.hasLoggableValue(): Boolean =
    if (SetField.REPS in measurementType.fields) {
        (reps ?: 0) > 0
    } else {
        (durationSeconds ?: 0) > 0 || (distanceMeters ?: 0.0) > 0.0 || (weight ?: 0.0) > 0.0
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
