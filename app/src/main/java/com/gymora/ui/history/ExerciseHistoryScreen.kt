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
}

/** Progress chart over time for one exercise, switchable between metrics (all in kg). */
@Composable
private fun ExerciseProgress(performances: List<ExercisePerformance>) {
    var metric by remember { mutableStateOf(ProgressMetric.ONE_RM) }
    val points = remember(performances, metric) {
        val fmt = DateTimeFormatter.ofPattern("d MMM")
        performances.reversed().mapNotNull { perf ->
            val sets = perf.sets
                .filter { it.isCompleted && it.weight != null && it.weight > 0 && (it.reps ?: 0) > 0 }
                .map { set ->
                    val kg = WorkoutCalculators.convertWeight(
                        set.weight!!, set.weightUnit ?: WeightUnit.KG, WeightUnit.KG,
                    )
                    kg to set.reps!!
                }
            if (sets.isEmpty()) {
                null
            } else {
                val value = when (metric) {
                    ProgressMetric.ONE_RM -> sets.maxOf { (w, r) -> WorkoutCalculators.estimatedOneRepMax(w, r) }
                    ProgressMetric.TOP_WEIGHT -> sets.maxOf { it.first }
                    ProgressMetric.VOLUME -> sets.sumOf { (w, r) -> w * r }
                }
                ChartPoint(fmt.format(perf.date.atZone(ZoneId.systemDefault())), value)
            }
        }
    }
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProgressMetric.entries.forEach { m ->
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

@Composable
private fun SetSummaryRow(set: ActiveSet) {
    val weightText = buildString {
        if (set.weight != null) append("${set.weight}")
        if (set.weightUnit != null) append(" ${set.weightUnit.name.lowercase()}")
    }
    val repsText = if (set.reps != null) "${set.reps} reps" else ""
    Text(
        text = "Set ${set.setNumber}: $weightText × $repsText",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(start = 8.dp, top = 2.dp),
    )
}
