package com.gymora.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.domain.model.ActiveExercise
import com.gymora.ui.workout.formatElapsed
import java.time.format.DateTimeFormatter

/**
 * Read-only historical workout detail (FR-041): every exercise and set exactly
 * as performed, rendered from snapshot rows (FR-043, FR-056).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    onBack: () -> Unit,
    viewModel: WorkoutDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.detail?.session?.routineNameSnapshot ?: "Workout") },
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
        } else {
            val detail = uiState.detail
            if (detail == null) {
                Text(
                    text = uiState.errorMessage ?: "Workout not found.",
                    modifier = Modifier.padding(innerPadding).padding(16.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Column {
                            Text(
                                text = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
                                    .format(detail.session.startedAt.atZone(java.time.ZoneId.systemDefault())),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            detail.session.endedAt?.let { ended ->
                                Text(
                                    text = "Duration: ${formatElapsed(java.time.Duration.between(detail.session.startedAt, ended).seconds)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    items(detail.exercises, key = { it.workoutExerciseId }) { exercise ->
                        HistoricalExerciseCard(exercise = exercise)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoricalExerciseCard(exercise: ActiveExercise) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = exercise.exerciseName,
                style = MaterialTheme.typography.titleMedium,
            )
            exercise.sets.forEach { set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Set ${set.setNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(0.3f),
                    )
                    Text(
                        text = "${set.weight?.toString() ?: "—"} × ${set.reps?.toString() ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(0.5f),
                    )
                    if (set.isCompleted) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
