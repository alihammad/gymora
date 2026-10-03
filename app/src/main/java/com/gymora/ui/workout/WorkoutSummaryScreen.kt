package com.gymora.ui.workout

import com.gymora.ui.components.Card
import com.gymora.ui.components.GymoraLoading
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.gymora.ui.components.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.domain.model.WorkoutSummary
import java.time.format.DateTimeFormatter

/**
 * Workout summary screen (FR-035): routine name, date, duration, exercise
 * count, set count, total reps, total volume, and a per-exercise breakdown
 * of completed sets.
 */
@Composable
fun WorkoutSummaryScreen(
    onDone: () -> Unit,
    viewModel: WorkoutSummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        GymoraLoading(modifier = Modifier.padding(24.dp))
        return
    }

    val summary = uiState.summary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Workout Complete!", style = MaterialTheme.typography.headlineMedium)

        if (summary == null) {
            Text("Summary unavailable.")
        } else {
            SummaryRow("Workout", summary.routineNameSnapshot)
            SummaryRow(
                "Date",
                DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
                    .format(summary.date.atZone(java.time.ZoneId.systemDefault())),
            )
            SummaryRow("Duration", formatElapsed(summary.duration.seconds), emphasize = true)
            SummaryRow("Exercises", summary.exerciseCount.toString())
            SummaryRow("Completed sets", summary.completedSetCount.toString(), emphasize = true)
            SummaryRow("Total reps", summary.totalReps.toString(), emphasize = true)
            SummaryRow("Total volume", "%.1f %s".format(summary.totalVolume, uiState.displayUnit.name.lowercase()), emphasize = true)

            Text("Per-exercise breakdown", style = MaterialTheme.typography.titleMedium)
            summary.perExerciseBreakdown.forEach { exercise ->
                SummaryRow(exercise.exerciseName, "${exercise.completedSetCount} sets completed")
            }
        }

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Done")
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasize: Boolean = false) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = if (emphasize) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleMedium,
                color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
