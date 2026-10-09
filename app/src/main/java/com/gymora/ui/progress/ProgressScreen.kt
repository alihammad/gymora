package com.gymora.ui.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.domain.calculator.WorkoutCalculators
import com.gymora.domain.model.PersonalRecord
import com.gymora.domain.model.WeightUnit
import com.gymora.ui.components.AngularPanel
import com.gymora.ui.components.FilterChip
import com.gymora.ui.components.GymoraLoading
import com.gymora.ui.components.MetricTile
import com.gymora.ui.components.SectionHeader
import com.gymora.ui.components.Sparkline
import com.gymora.ui.components.TopAppBar
import com.gymora.ui.theme.DisplayHero
import com.gymora.ui.theme.LabelCaps
import java.util.Locale

/**
 * Performance dashboard: this week's overview, volume, strength progression, consistency and
 * personal records, followed by every exercise and routine trained in the chosen period.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onBack: () -> Unit,
    onExerciseClick: (Long) -> Unit,
    onRoutineClick: (Long) -> Unit,
    onRecords: () -> Unit = {},
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var selectedExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionHeader("Weekly overview") }
            item { WeeklyOverview(state) }
            item { VolumePanel(state) }

            if (state.isLoading) {
                item { GymoraLoading(modifier = Modifier.padding(16.dp)) }
            } else if (state.exercises.isEmpty() && state.routines.isEmpty()) {
                item { PeriodChips(state, viewModel::onPeriodSelected) }
                item {
                    Text(
                        "No completed workouts in this period yet. Finish a workout to see progress here.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            } else {
                item { SectionHeader("Strength progression") }
                item { PeriodChips(state, viewModel::onPeriodSelected) }
                if (state.exercises.isNotEmpty()) {
                    item {
                        StrengthPanel(
                            rows = state.exercises,
                            selectedId = selectedExerciseId,
                            unit = state.weightUnit,
                            onSelect = { selectedExerciseId = it },
                            onOpen = onExerciseClick,
                        )
                    }
                }
            }

            item { SectionHeader("Consistency & records") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ConsistencyPanel(state, Modifier.weight(1f))
                    RecordsPanel(state, onRecords, Modifier.weight(1f))
                }
            }

            if (!state.isLoading && (state.exercises.isNotEmpty() || state.routines.isNotEmpty())) {
                if (state.exercises.isNotEmpty()) {
                    item { SectionHeader("Exercises · est. 1RM") }
                    item { RowsCard(state.exercises, onExerciseClick) }
                }
                if (state.routines.isNotEmpty()) {
                    item { SectionHeader("Workouts · volume") }
                    item { RowsCard(state.routines, onRoutineClick) }
                }
            }
        }
    }
}

@Composable
private fun PeriodChips(state: ProgressUiState, onSelect: (ProgressPeriod) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ProgressPeriod.entries.forEach { p ->
            FilterChip(
                selected = state.period == p,
                onClick = { onSelect(p) },
                label = { Text(p.label) },
            )
        }
    }
}

@Composable
private fun WeeklyOverview(state: ProgressUiState) {
    val week = state.week
    val goal = state.weeklyProgress
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            AngularPanel(Modifier.weight(1f)) {
                MetricTile(
                    value = week.workouts.toString(),
                    label = if (goal != null) "Workouts of ${goal.goal}" else "Workouts",
                    accent = scheme.primary,
                )
            }
            AngularPanel(Modifier.weight(1f)) {
                MetricTile(
                    value = (goal?.streakWeeks ?: 0).toString(),
                    unit = "wk",
                    label = "Streak",
                    accent = scheme.primary,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            AngularPanel(Modifier.weight(1f)) {
                MetricTile(value = formatDuration(week.durationSeconds), label = "Duration")
            }
            AngularPanel(Modifier.weight(1f)) {
                MetricTile(
                    value = formatVolume(week.volumeKg, state.weightUnit),
                    unit = state.weightUnit.name.lowercase(),
                    label = "Volume",
                )
            }
        }
    }
}

@Composable
private fun VolumePanel(state: ProgressUiState) {
    val week = state.week
    val unit = state.weightUnit
    AngularPanel(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("VOLUME · THIS WEEK", style = LabelCaps, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(formatVolume(week.volumeKg, unit), style = DisplayHero.copy(fontSize = 44.sp, lineHeight = 44.sp))
                Text(
                    " ${unit.name.lowercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            if (week.volumeKg > 0) {
                WeekVolumeChart(
                    daily = week.dailyVolumeKg,
                    todayIndex = week.todayIndex,
                    average = week.averageTrainingDayKg,
                    description = "Volume this week ${formatVolume(week.volumeKg, unit)} ${unit.name.lowercase()}, " +
                        "average ${formatVolume(week.averageTrainingDayKg, unit)} per training day",
                )
                Text(
                    "Dashed line: average ${formatVolume(week.averageTrainingDayKg, unit)} ${unit.name.lowercase()} " +
                        "per training day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "No workouts logged this week yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StrengthPanel(
    rows: List<ProgressRow>,
    selectedId: Long?,
    unit: WeightUnit,
    onSelect: (Long) -> Unit,
    onOpen: (Long) -> Unit,
) {
    val choices = rows.take(6)
    val selected = rows.firstOrNull { it.series.id == selectedId } ?: rows.first()
    val values = selected.series.values.map { toDisplay(it, unit) }
    val latest = values.lastOrNull() ?: 0.0
    val change = if (values.size > 1) latest - values.first() else null
    val unitLabel = unit.name.lowercase()
    AngularPanel(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                choices.forEach { row ->
                    FilterChip(
                        selected = row.series.id == selected.series.id,
                        onClick = { onSelect(row.series.id) },
                        label = { Text(row.series.name, maxLines = 1) },
                    )
                }
            }
            Text("EST. 1RM · ${selected.series.name.uppercase()}", style = LabelCaps, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    String.format(Locale.US, "%.0f", latest),
                    style = DisplayHero.copy(fontSize = 44.sp, lineHeight = 44.sp),
                )
                Text(
                    " $unitLabel",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            if (change != null) {
                val up = change >= 0
                Text(
                    (if (up) "▲ +" else "▼ ") + String.format(Locale.US, "%.0f %s", change, unitLabel) + " since first session",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (up) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
            StrengthLineChart(
                values = values,
                description = "Estimated one rep max for ${selected.series.name}, ${values.size} sessions, " +
                    "latest ${String.format(Locale.US, "%.0f", latest)} $unitLabel",
                modifier = Modifier.fillMaxWidth().height(150.dp),
            )
            Text(
                "VIEW ${selected.series.name.uppercase()} HISTORY",
                style = LabelCaps,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onOpen(selected.series.id) }
                    .padding(vertical = 14.dp),
            )
        }
    }
}

@Composable
private fun ConsistencyPanel(state: ProgressUiState, modifier: Modifier = Modifier) {
    val goal = state.weeklyProgress
    val fraction = if (goal != null && goal.goal > 0) goal.done.toFloat() / goal.goal else 0f
    AngularPanel(modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("WEEKLY CONSISTENCY", style = LabelCaps, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            ConsistencyRing(fraction, Modifier.size(110.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                if (goal != null) "${goal.done} of ${goal.goal} workouts" else "No goal set",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecordsPanel(state: ProgressUiState, onRecords: () -> Unit, modifier: Modifier = Modifier) {
    val unit = state.weightUnit
    val label = unit.name.lowercase()
    val records = state.records
    AngularPanel(modifier, onClick = onRecords) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Text("PERSONAL RECORDS", style = LabelCaps, color = MaterialTheme.colorScheme.onSurfaceVariant)
            RecordLine("Heaviest", records?.heaviestWeight, "%.1f $label", unit)
            RecordLine("Est. 1RM", records?.bestEstimatedOneRepMax, "%.1f $label", unit)
            RecordLine("Best volume", records?.largestWorkoutVolume, "%.0f $label", unit)
        }
    }
}

@Composable
private fun RecordLine(title: String, record: PersonalRecord?, format: String, unit: WeightUnit) {
    Column {
        Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = record?.let { String.format(Locale.US, format, toDisplay(it.value, unit)) } ?: "—",
            style = MaterialTheme.typography.titleLarge,
        )
        record?.let {
            Text(it.exerciseName, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
    }
}

@Composable
private fun RowsCard(rows: List<ProgressRow>, onClick: (Long) -> Unit) {
    AngularPanel(Modifier.fillMaxWidth()) {
        Column {
            rows.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
                ProgressRowItem(row, onClick = { onClick(row.series.id) })
            }
        }
    }
}

@Composable
private fun ProgressRowItem(row: ProgressRow, onClick: () -> Unit) {
    val values = row.series.values
    val sessionText = if (values.size == 1) "1 session" else "${values.size} sessions"
    val change = if (values.size > 1 && values.first() > 0) {
        (values.last() - values.first()) / values.first() * 100
    } else {
        null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(row.series.name, style = MaterialTheme.typography.titleMedium)
            Text(
                listOfNotNull(row.subtitle, sessionText).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (change != null) {
                val up = change >= 0
                Text(
                    (if (up) "▲ +" else "▼ ") + String.format(Locale.US, "%.0f%%", change),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (up) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }
        Sparkline(values = values, modifier = Modifier.width(110.dp).height(44.dp))
    }
}

private fun toDisplay(kg: Double, unit: WeightUnit): Double =
    if (unit == WeightUnit.KG) kg else WorkoutCalculators.convertWeight(kg, WeightUnit.KG, WeightUnit.LB)

private fun formatVolume(kg: Double, unit: WeightUnit): String =
    String.format(Locale.getDefault(), "%,.0f", toDisplay(kg, unit))

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
