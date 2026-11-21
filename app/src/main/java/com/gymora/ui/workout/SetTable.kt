package com.gymora.ui.workout

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gymora.domain.calculator.SetLabels
import com.gymora.domain.calculator.SetSummary
import com.gymora.domain.model.ActiveExercise
import com.gymora.domain.model.ActiveSet
import com.gymora.domain.model.PreviousPerformance
import com.gymora.domain.model.SetValue
import com.gymora.domain.model.MeasurementType
import com.gymora.ui.components.LocalWeightUnit
import com.gymora.ui.components.SetEntry
import com.gymora.ui.components.SetInputRow
import com.gymora.ui.components.TextButton
import com.gymora.ui.components.setColumnLabels
import com.gymora.ui.theme.GymoraShapes
import com.gymora.ui.theme.GymoraThemeTokens
import com.gymora.ui.theme.LabelCaps

// Tabular set logging for design languages with GymoraStyle.tableSets: each exercise is a flat section
// on the page (no card) with SET / PREVIOUS / value columns / check, and completed rows tint full width.

private val SetColumn = 32.dp
private val CheckColumn = 44.dp
private val CellGap = 8.dp
private val RowHorizontalPadding = 16.dp
private const val PREVIOUS_WEIGHT = 1.4f

/** One exercise as a section: accent name with option buttons, the set table, then ADD SET. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ExerciseTableSection(
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
    val scheme = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = RowHorizontalPadding, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = exercise.exerciseName,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
                    .combinedClickable(
                        onClick = { if (hasGuide) showGuide = !showGuide },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuOpen = true
                        },
                        onLongClickLabel = "Exercise options",
                    )
                    .padding(vertical = 12.dp),
            )
            if (hasGuide) {
                SoftIconButton(
                    onClick = { showGuide = !showGuide },
                    contentDescription = if (showGuide) "Hide form guide" else "Show form guide",
                    highlighted = showGuide,
                ) { tint -> Icon(Icons.Outlined.Info, contentDescription = null, tint = tint) }
                Spacer(Modifier.width(8.dp))
            }
            Box {
                SoftIconButton(
                    onClick = { menuOpen = true },
                    contentDescription = "Options for ${exercise.exerciseName}",
                    highlighted = false,
                    filled = false,
                ) { tint -> Icon(Icons.Filled.MoreHoriz, contentDescription = null, tint = tint) }
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
        }

        if (showGuide) {
            Box(Modifier.padding(horizontal = RowHorizontalPadding)) { FormGuide(exercise) }
        }

        val sets = exercise.sets
        val type = sets.firstOrNull()?.measurementType
        if (type != null) SetTableHeader(type)

        val labels = SetLabels.of(sets)
        val background = scheme.background
        sets.forEachIndexed { index, set ->
            key(set.id) {
                SwipeToDeleteSet(onDelete = { onDeleteSet(set.id) }, rowColor = background, shape = null) {
                    SetTableRow(
                        set = set,
                        label = labels[index],
                        previous = previousPerformance?.sets?.getOrNull(index),
                        onValuesChanged = { onSetValuesChanged(set.id, it) },
                        onToggleComplete = { onToggleComplete(set.id, it) },
                    )
                }
            }
        }

        TextButton(
            onClick = { onAddSet(exercise.workoutExerciseId) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = RowHorizontalPadding, vertical = 4.dp),
        ) {
            Text(
                text = if (sets.any { it.side != null }) "ADD SET (BOTH SIDES)" else "ADD SET",
                style = LabelCaps.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize),
            )
        }
    }
}

/** Column captions; the inner value group mirrors [SetInputRow]'s spacing so columns line up. */
@Composable
private fun SetTableHeader(type: MeasurementType) {
    val unit = LocalWeightUnit.current
    val labels = setColumnLabels(type, unit)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = RowHorizontalPadding, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CellGap),
    ) {
        HeaderCell("SET", Modifier.width(SetColumn))
        HeaderCell("PREVIOUS", Modifier.weight(PREVIOUS_WEIGHT))
        Row(
            modifier = Modifier.weight(labels.size.toFloat()),
            horizontalArrangement = Arrangement.spacedBy(CellGap),
        ) {
            labels.forEach { HeaderCell(it, Modifier.weight(1f)) }
        }
        Box(Modifier.width(CheckColumn), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Done",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Text(
        text = text,
        style = LabelCaps,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
private fun SetTableRow(
    set: ActiveSet,
    label: String,
    previous: SetValue?,
    onValuesChanged: (SetEntry) -> Unit,
    onToggleComplete: (Boolean) -> Unit,
) {
    val extra = GymoraThemeTokens.extraColors
    val scheme = MaterialTheme.colorScheme
    val unit = LocalWeightUnit.current
    val done = set.isCompleted
    val rowColor by animateColorAsState(
        targetValue = if (done) extra.setCompleteRow else Color.Transparent,
        label = "setRowTint",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowColor)
            .padding(horizontal = RowHorizontalPadding, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CellGap),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(SetColumn),
        )
        Text(
            text = previous?.let {
                SetSummary.describe(
                    set.measurementType, it.weight, it.weightUnit, it.reps,
                    it.durationSeconds, it.distanceMeters, unit,
                )
            } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(PREVIOUS_WEIGHT),
        )
        SetInputRow(
            key = set.id,
            type = set.measurementType,
            initial = SetEntry(set.weight, set.reps, set.durationSeconds, set.distanceMeters),
            hint = previous?.let { SetEntry(it.weight, it.reps, it.durationSeconds, it.distanceMeters) },
            onChange = onValuesChanged,
            compact = true,
            completed = done,
            modifier = Modifier.weight(set.measurementType.fields.size.toFloat()),
        )
        // FR-025 / FR-061: completion is marked with a check icon, not color alone.
        val checkFill by animateColorAsState(
            targetValue = if (done) extra.setComplete else scheme.surfaceContainerHighest,
            label = "setCheckFill",
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(CheckColumn)
                .height(36.dp)
                .clip(GymoraShapes.input)
                .background(checkFill)
                .toggleable(
                    value = done,
                    // Unchecking is always allowed; checking needs a value.
                    enabled = done || set.hasLoggableValue(),
                    role = Role.Checkbox,
                    onValueChange = onToggleComplete,
                ),
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = if (done) "Mark set incomplete" else "Complete set",
                // Light accents (cyan, lime) need a dark check; deeper ones (green, blue) a white one.
                tint = if (!done) extra.textDisabled else if (extra.setComplete.luminance() > 0.5f) Color.Black else Color.White,
            )
        }
    }
}

/** Small rounded icon button; [filled] gives it the soft accent-tinted container. */
@Composable
private fun SoftIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    highlighted: Boolean,
    filled: Boolean = true,
    icon: @Composable (tint: Color) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 48.dp, height = 36.dp)
            .clip(GymoraShapes.input)
            .background(
                when {
                    highlighted -> scheme.primary
                    filled -> scheme.primaryContainer
                    else -> Color.Transparent
                },
            )
            .semantics { this.contentDescription = contentDescription }
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        icon(if (highlighted) scheme.onPrimary else scheme.primary)
    }
}
