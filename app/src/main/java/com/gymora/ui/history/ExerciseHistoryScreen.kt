package com.gymora.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
                title = { Text("Exercise History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(innerPadding).padding(16.dp))
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
                items(uiState.performances, key = { it.sessionId }) { performance ->
                    PerformanceCard(performance)
                }
                if (uiState.isLoadingMore) {
                    item {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
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