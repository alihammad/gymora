package com.gymora.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gymora.domain.model.DaySteps
import com.gymora.ui.theme.GymoraShapes
import java.text.NumberFormat
import java.time.format.TextStyle
import java.util.Locale

/**
 * Daily steps as a bar chart (opened from the Home steps card): last 7 or 30 days, a
 * dashed line at the daily goal, and bars that reach it drawn in the accent colour.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepHistorySheet(
    history: List<DaySteps>,
    days: Int,
    goal: Int,
    onRangeSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val format = NumberFormat.getIntegerInstance()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = GymoraShapes.sheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Step history", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = days == HomeViewModel.STEP_HISTORY_WEEK,
                    onClick = { onRangeSelected(HomeViewModel.STEP_HISTORY_WEEK) },
                    label = { Text("7 days") },
                )
                FilterChip(
                    selected = days == HomeViewModel.STEP_HISTORY_MONTH,
                    onClick = { onRangeSelected(HomeViewModel.STEP_HISTORY_MONTH) },
                    label = { Text("30 days") },
                )
            }

            if (history.isEmpty()) {
                Text(
                    "Loading…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val tracked = history.filter { it.steps > 0 }
                val average = if (tracked.isEmpty()) 0 else tracked.sumOf { it.steps } / tracked.size
                val best = history.maxBy { it.steps }
                val goalDays = history.count { it.steps >= goal }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Stat("Daily average", format.format(average))
                    Stat("Best day", format.format(best.steps))
                    Stat("Goal reached", "$goalDays of ${history.size}")
                }
                StepBars(history = history, goal = goal)
                Text(
                    "Dashed line: your goal of ${format.format(goal)} steps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StepBars(history: List<DaySteps>, goal: Int) {
    val scheme = MaterialTheme.colorScheme
    val format = NumberFormat.getIntegerInstance()
    val reached = scheme.primary
    val missed = scheme.tertiary
    val goalLine = scheme.onSurfaceVariant
    val max = maxOf(goal, history.maxOf { it.steps }) * HEADROOM
    val description = "Bar chart of steps per day. " +
        history.joinToString(". ") { "${it.date}: ${format.format(it.steps)}" }

    Column(modifier = Modifier.semantics { contentDescription = description }) {
        Canvas(modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT)) {
            val slot = size.width / history.size
            val barWidth = slot * BAR_FILL
            history.forEachIndexed { index, day ->
                val barHeight = size.height * (day.steps / max)
                drawRoundRect(
                    color = if (day.steps >= goal) reached else missed,
                    topLeft = Offset(index * slot + (slot - barWidth) / 2, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 4, barWidth / 4),
                )
            }
            val goalY = size.height * (1 - goal / max)
            drawLine(
                color = goalLine,
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = Stroke.DefaultMiter / 4,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f)),
            )
        }
        // Weekday letters under a week; under a month, only every few days so labels fit.
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            history.forEachIndexed { index, day ->
                val label = when {
                    history.size <= 7 -> day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                    index % MONTH_LABEL_EVERY == 0 || index == history.lastIndex -> day.date.dayOfMonth.toString()
                    else -> ""
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    softWrap = false,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private val CHART_HEIGHT = 180.dp
private const val HEADROOM = 1.1f
private const val BAR_FILL = 0.7f
private const val MONTH_LABEL_EVERY = 5
