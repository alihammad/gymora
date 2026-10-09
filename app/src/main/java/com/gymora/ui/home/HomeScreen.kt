package com.gymora.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.gymora.ui.components.ActionButton
import com.gymora.ui.components.AngularPanel
import com.gymora.ui.components.HeroCard
import com.gymora.ui.components.SectionHeader
import com.gymora.ui.components.SegmentedProgress
import com.gymora.ui.routines.routineIcon
import com.gymora.ui.theme.DisplayHero
import com.gymora.ui.theme.LabelCaps
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    onHistory: () -> Unit,
    onRecords: () -> Unit,
    onBody: () -> Unit,
    onProgress: () -> Unit,
    onStartWorkout: ((Long) -> Unit)? = null,
    onResumeWorkout: ((Long) -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var blockedStart by remember { mutableStateOf(false) }

    // Only one workout can be active: START on the active routine resumes it,
    // START on another routine explains why it can't begin.
    val startRoutine: (Long) -> Unit = start@{ routineId ->
        val active = uiState.activeWorkout
        when {
            active == null -> onStartWorkout?.invoke(routineId)
            active.session.routineId == routineId -> onResumeWorkout?.invoke(active.session.id)
            else -> blockedStart = true
        }
    }
    if (blockedStart) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { blockedStart = false },
            title = { Text("Workout in progress") },
            text = {
                Text("Finish or discard \"${uiState.activeWorkout?.session?.routineNameSnapshot.orEmpty()}\" before starting another workout.")
            },
            confirmButton = {
                com.gymora.ui.components.TextButton(onClick = {
                    blockedStart = false
                    uiState.activeWorkout?.let { onResumeWorkout?.invoke(it.session.id) }
                }) { Text("Resume") }
            },
            dismissButton = {
                com.gymora.ui.components.TextButton(onClick = { blockedStart = false }) { Text("Cancel") }
            },
        )
    }

    // Step counter: needs the activity-recognition permission on Android 10+. Re-checked
    // on resume, so granting it from system settings also takes effect.
    val context = LocalContext.current
    val stepPermissionNeeded = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    var stepPermissionGranted by remember { mutableStateOf(hasStepPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> stepPermissionGranted = granted }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) stepPermissionGranted = hasStepPermission(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    androidx.compose.runtime.LaunchedEffect(stepPermissionGranted) {
        viewModel.startStepTracking(stepPermissionGranted)
    }

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
        // One lazy list for the whole page, so everything scrolls together.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            // Room under the last routine so the create button never covers it.
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp),
        ) {
            item(key = "week-strip") {
                WeekStrip(
                    weekStart = uiState.weekStart,
                    workoutDays = uiState.workoutDays,
                    onShiftWeek = viewModel::onShiftWeek,
                    onDayClick = viewModel::onDaySelected,
                )
            }

            // Hero: the workout to do next (or resume), with the weekly completion progress.
            if (!uiState.isLoading) {
                val active = uiState.activeWorkout
                val next = nextRoutine(uiState.routines)
                if (active != null || next != null) {
                    item(key = "today-hero") {
                        TodayWorkoutHero(
                            name = active?.session?.routineNameSnapshot ?: next!!.name,
                            exerciseCount = if (active != null) active.exercises.size else next!!.exerciseCount,
                            resuming = active != null,
                            startedAt = active?.startedAt,
                            progress = uiState.weeklyProgress,
                            onClick = {
                                if (active != null) onResumeWorkout?.invoke(active.session.id)
                                else onRoutineClick(next!!.id)
                            },
                            onAction = {
                                if (active != null) onResumeWorkout?.invoke(active.session.id)
                                else startRoutine(next!!.id)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            // Hidden only on phones with no sensor and no Health Connect steps.
            if (uiState.stepsSupported || uiState.stepsToday != null) {
                item(key = "steps") {
                    StepsCard(
                        steps = uiState.stepsToday,
                        goal = uiState.stepGoal,
                        permissionGranted = stepPermissionGranted || uiState.stepsToday != null,
                        onGrantPermission = {
                            if (stepPermissionNeeded) permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        },
                        onClick = viewModel::onStepsClicked,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            // Single-line chips in a horizontally scrollable row: nothing wraps
            // or squeezes on narrow screens.
            item(key = "quick-links") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickLinkChip("My Workouts", Icons.Filled.FormatListBulleted, onMyRoutines)
                    QuickLinkChip("History", Icons.Filled.History, onHistory)
                    QuickLinkChip("Progress", Icons.AutoMirrored.Filled.ShowChart, onProgress)
                    QuickLinkChip("Records", Icons.Filled.EmojiEvents, onRecords)
                    QuickLinkChip("Body", Icons.Filled.MonitorWeight, onBody)
                }
            }

            when {
                uiState.isLoading -> item(key = "loading") {
                    GymoraLoading(modifier = Modifier.padding(16.dp))
                }

                uiState.routines.isEmpty() -> item(key = "empty") {
                    EmptyState(
                        message = EmptyStateCopy.NO_ROUTINES,
                        actionLabel = EmptyStateCopy.CREATE_ROUTINE_ACTION,
                        onAction = onCreateRoutine,
                    )
                }

                else -> {
                    val heroId = if (uiState.activeWorkout == null) nextRoutine(uiState.routines)?.id else null
                    val others = uiState.routines.filter { it.id != heroId }
                    if (others.isNotEmpty()) {
                        item(key = "routines-header") {
                            SectionHeader("Your workouts", modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                    itemsIndexed(others, key = { _, routine -> routine.id }) { _, routine ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        RoutineCard(
                            routine = routine,
                            onClick = { onRoutineClick(routine.id) },
                            onStart = { startRoutine(routine.id) },
                        )
                    }
                    }
                }
            }
        }

        // Sheets are modal windows, so they live outside the scrolling list.
        if (uiState.showStepHistory) {
            StepHistorySheet(
                history = uiState.stepHistory,
                days = uiState.stepHistoryDays,
                goal = uiState.stepGoal,
                onRangeSelected = viewModel::onStepHistoryRangeSelected,
                onDismiss = viewModel::onStepHistoryDismissed,
            )
        }

        uiState.selectedDay?.let { day ->
            DayWorkoutsSheet(
                day = day,
                workouts = uiState.selectedDayWorkouts,
                onDismiss = viewModel::onDayDismissed,
            )
        }
    }
}

/** The routine to do next: never performed first, otherwise the one performed longest ago. */
private fun nextRoutine(routines: List<RoutineSummary>): RoutineSummary? =
    routines.minByOrNull { it.lastPerformedAt ?: Long.MIN_VALUE }

/** Today's Workout hero: name, size, weekly progress and the dominant START / RESUME action. */
@Composable
private fun TodayWorkoutHero(
    name: String,
    exerciseCount: Int,
    resuming: Boolean,
    startedAt: java.time.Instant?,
    progress: com.gymora.domain.calculator.WeeklyProgress?,
    onClick: () -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var elapsed by remember { mutableStateOf(0L) }
    androidx.compose.runtime.LaunchedEffect(startedAt) {
        while (startedAt != null) {
            elapsed = java.time.Duration.between(startedAt, java.time.Instant.now()).seconds
            kotlinx.coroutines.delay(1000)
        }
    }
    val scheme = MaterialTheme.colorScheme
    HeroCard(
        modifier = modifier,
        onClick = onClick,
        watermark = {
            Icon(
                painter = painterResource(routineIcon(name)),
                contentDescription = null,
                tint = scheme.primary.copy(alpha = 0.18f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(180.dp),
            )
        },
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = if (resuming) "WORKOUT IN PROGRESS" else "TODAY'S WORKOUT",
                style = LabelCaps,
                color = scheme.primary,
            )
            Text(
                text = name.uppercase(),
                style = DisplayHero,
                color = Color.White,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 56.dp),
            )
            val detail = if (resuming) {
                "${com.gymora.ui.workout.formatElapsed(elapsed)} elapsed"
            } else {
                "$exerciseCount ${if (exerciseCount == 1) "exercise" else "exercises"}"
            }
            Text(text = detail, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.85f))
            if (progress != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SegmentedProgress(done = progress.done, total = progress.goal)
                    Text(
                        text = "${progress.done} of ${progress.goal} workouts this week" +
                            if (progress.streakWeeks > 0) " · ${progress.streakWeeks} wk streak" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
            ActionButton(
                text = if (resuming) "Resume workout" else "Start workout",
                onClick = onAction,
                icon = Icons.Filled.PlayArrow,
            )
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
    val scheme = MaterialTheme.colorScheme
    AngularPanel(modifier = Modifier.fillMaxWidth(), onClick = onClick, accentBar = scheme.primary) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(routineIcon(routine.name)),
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(36.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(text = routine.name.uppercase(), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = buildString {
                        append("${routine.exerciseCount} exercises")
                        routine.lastPerformedAt?.let { append(" · last ${formatDate(it)}") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(GymoraShapes.chip)
                    .background(scheme.primary)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onStart),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Start ${routine.name}",
                    tint = scheme.onPrimary,
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))

/** Before Android 10 the step sensor needs no runtime permission. */
private fun hasStepPermission(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
        PackageManager.PERMISSION_GRANTED
