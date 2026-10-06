package com.gymora.ui.profile

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.domain.calculator.WorkoutCalculators
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.model.WorkoutStat
import com.gymora.ui.calendar.WorkoutCalendar
import com.gymora.ui.components.Card
import com.gymora.ui.components.FilterChip
import com.gymora.ui.components.GymoraLoading
import com.gymora.ui.components.TopAppBar
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MILLIS_PER_HOUR = 3_600_000.0

/**
 * Profile: bar charts of workout duration, volume and reps (one bar per workout,
 * filterable by period) followed by a month calendar, swiped left/right one month
 * per view, that labels every day trained with the routine done that day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onWorkoutClick: (Long) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.refresh() }

    val zone = remember { ZoneId.systemDefault() }

    Scaffold(topBar = { TopAppBar(title = { Text("Profile") }) }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProfilePeriod.entries.forEach { p ->
                        FilterChip(
                            selected = state.period == p,
                            onClick = { viewModel.onPeriodSelected(p) },
                            label = { Text(p.label) },
                        )
                    }
                }
            }
            if (state.isLoading) {
                item { GymoraLoading(modifier = Modifier.padding(16.dp)) }
            } else {
                item { ChartsSection(state.chartWorkouts, state.weightUnit, zone) }
                item {
                    Text(
                        "Calendar",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                item {
                    WorkoutCalendar(
                        workouts = state.allWorkouts,
                        onDayClick = { _, workouts -> onWorkoutClick(workouts.first().sessionId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartsSection(workouts: List<WorkoutStat>, unit: WeightUnit, zone: ZoneId) {
    if (workouts.isEmpty()) {
        Text(
            "No completed workouts in this period yet. Finish a workout to see your charts here.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        return
    }
    val dateFormat = remember { DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()) }
    val labels = remember(workouts) {
        workouts.map { it.startedAt.atZone(zone).toLocalDate().format(dateFormat) }
    }
    val durationBars = remember(workouts) {
        workouts.mapIndexed { i, w -> Bar(labels[i], w.duration.toMillis() / MILLIS_PER_HOUR) }
    }
    val volumeBars = remember(workouts, unit) {
        workouts.mapIndexed { i, w ->
            Bar(labels[i], WorkoutCalculators.convertWeight(w.volumeKg, WeightUnit.KG, unit))
        }
    }
    val repBars = remember(workouts) {
        workouts.mapIndexed { i, w -> Bar(labels[i], w.totalReps.toDouble()) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChartCard("Duration (hours)", durationBars) { formatNumber(it, decimals = 1) }
        ChartCard("Volume (${unit.name.lowercase(Locale.US)})", volumeBars, ::formatCompact)
        ChartCard("Reps", repBars, ::formatCompact)
    }
}

@Composable
private fun ChartCard(title: String, bars: List<Bar>, formatValue: (Double) -> String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ScrollableBarChart(
                bars = bars,
                formatValue = formatValue,
                description = "$title bar chart, ${bars.size} workouts from ${bars.first().label} to ${bars.last().label}",
            )
        }
    }
}

private fun formatNumber(value: Double, decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", value)

/** 1234 -> "1.2k", 950 -> "950". */
private fun formatCompact(value: Double): String =
    if (value >= 1000) formatNumber(value / 1000, 1).removeSuffix(".0") + "k" else formatNumber(value, 0)
