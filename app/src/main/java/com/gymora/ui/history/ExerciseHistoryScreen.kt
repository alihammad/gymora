package com.gymora.ui.history

import com.gymora.ui.components.GymoraLoading
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.gymora.ui.components.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.gymora.ui.components.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import com.gymora.ui.components.FilterChip
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gymora.domain.calculator.WorkoutCalculators
import com.gymora.domain.model.WeightUnit
import com.gymora.ui.components.ChartPoint
import com.gymora.ui.components.ProgressChartCard
import com.gymora.domain.model.ActiveSet
import com.gymora.domain.calculator.SetFormat
import com.gymora.domain.calculator.SetSummary
import com.gymora.ui.components.LocalWeightUnit
import com.gymora.domain.model.ExercisePerformance
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Exercise history screen (FR-044, PRD-§17): all performances of one exercise
 * across completed workouts, newest first, with all sets shown.
 * Reachable from exercise library and workout detail.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseHistoryScreen(
    onBack: () -> Unit,
    viewModel: ExerciseHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= uiState.performances.size - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.onLoadMore()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress & History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            GymoraLoading(modifier = Modifier.padding(innerPadding).padding(16.dp))
        } else if (uiState.performances.isEmpty()) {
            Text(
                text = "No history for this exercise yet.",
                modifier = Modifier.padding(innerPadding).padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                state = listState,
            ) {
                item(key = "progress-chart") {
                    ExerciseProgress(uiState.performances)
                }
                // The same exercise can appear more than once in a session, so sessionId alone isn't unique.
                itemsIndexed(
                    uiState.performances,
                    key = { index, it -> "${it.sessionId}-$index" },
                ) { _, performance ->
                    PerformanceCard(performance)
                }
                if (uiState.isLoadingMore) {
                    item {
                        GymoraLoading(modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

private enum class ProgressMetric(val label: String, val unit: String) {
    ONE_RM("Est. 1RM", "kg"),
    TOP_WEIGHT("Top weight", "kg"),
    VOLUME("Volume", "kg"),
    LONGEST_TIME("Longest set", "min"),
    TOTAL_DISTANCE("Distance", "km"),
}

private const val SECONDS_PER_MINUTE = 60.0

/** Progress chart over time for one exercise, switchable between metrics (weights in kg). */
@Composable
private fun ExerciseProgress(performances: List<ExercisePerformance>) {
    // Offer only metrics this exercise has data for, e.g. time for planks.
    val metrics = remember(performances) {
        val done = performances.flatMap { it.sets }.filter { it.isCompleted }
        buildList {
            if (done.any { it.measurementType.countsWeightAsLoad && (it.weight ?: 0.0) > 0 && (it.reps ?: 0) > 0 }) {
                addAll(listOf(ProgressMetric.ONE_RM, ProgressMetric.TOP_WEIGHT, ProgressMetric.VOLUME))
            }
            if (done.any { (it.durationSeconds ?: 0) > 0 }) add(ProgressMetric.LONGEST_TIME)
            if (done.any { (it.distanceMeters ?: 0.0) > 0 }) add(ProgressMetric.TOTAL_DISTANCE)
        }.ifEmpty { listOf(ProgressMetric.ONE_RM) }
    }
    var metric by remember(metrics) { mutableStateOf(metrics.first()) }
    val points = remember(performances, metric) {
        val fmt = DateTimeFormatter.ofPattern("d MMM")
        performances.reversed().mapNotNull { perf ->
            metricValue(perf.sets.filter { it.isCompleted }, metric)
                ?.let { ChartPoint(fmt.format(perf.date.atZone(ZoneId.systemDefault())), it) }
        }
    }
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            metrics.forEach { m ->
                FilterChip(
                    selected = metric == m,
                    onClick = { metric = m },
                    label = { Text(m.label) },
                )
            }
        }
        ProgressChartCard(title = metric.label, unit = metric.unit, points = points)
    }
}

@Composable
private fun PerformanceCard(performance: ExercisePerformance) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm") }
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = dateFormatter.format(
                    performance.date.atZone(ZoneId.systemDefault()),
                ),
                style = MaterialTheme.typography.titleSmall,
            )
            performance.sets.forEach { set ->
                SetSummaryRow(set)
            }
        }
    }
}

/** One session's value for [metric], or null when it has no matching sets. */
private fun metricValue(sets: List<ActiveSet>, metric: ProgressMetric): Double? {
    val lifts = sets
        .filter { it.measurementType.countsWeightAsLoad && (it.weight ?: 0.0) > 0 && (it.reps ?: 0) > 0 }
        .map { set ->
            WorkoutCalculators.convertWeight(set.weight!!, set.weightUnit ?: WeightUnit.KG, WeightUnit.KG) to set.reps!!
        }
    return when (metric) {
        ProgressMetric.ONE_RM -> lifts.maxOfOrNull { (w, r) -> WorkoutCalculators.estimatedOneRepMax(w, r) }
        ProgressMetric.TOP_WEIGHT -> lifts.maxOfOrNull { it.first }
        ProgressMetric.VOLUME -> lifts.takeIf { it.isNotEmpty() }?.sumOf { (w, r) -> w * r }
        ProgressMetric.LONGEST_TIME -> sets.mapNotNull { it.durationSeconds }.maxOrNull()?.div(SECONDS_PER_MINUTE)
        ProgressMetric.TOTAL_DISTANCE -> sets.mapNotNull { it.distanceMeters }
            .takeIf { it.isNotEmpty() }?.sum()?.div(WorkoutCalculators.METERS_PER_KM)
    }
}

@Composable
private fun SetSummaryRow(set: ActiveSet) {
    val summary = SetSummary.describe(
        set.measurementType, set.weight, set.weightUnit, set.reps,
        set.durationSeconds, set.distanceMeters, LocalWeightUnit.current,
    )
    val label = set.side?.let { " (${it.shortLabel})" }.orEmpty()
    Text(
        text = "Set ${set.setNumber}$label: $summary",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(start = 8.dp, top = 2.dp),
    )
}
