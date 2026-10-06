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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.gymora.ui.components.OutlinedButton
import com.gymora.ui.theme.GymoraShapes
import kotlinx.coroutines.launch

/**
 * Workout summary screen (FR-035): a shareable card with routine name, date,
 * duration, volume, sets, reps, personal records and streak, followed by a
 * per-exercise breakdown of completed sets.
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
            // The card is recorded into a layer so Share exports exactly what is on screen.
            val cardLayer = rememberGraphicsLayer()
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            WorkoutShareCard(
                summary = summary,
                records = uiState.records,
                streakWeeks = uiState.streakWeeks,
                unit = uiState.displayUnit,
                modifier = Modifier
                    .clip(GymoraShapes.card)
                    .drawWithContent {
                        cardLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(cardLayer)
                    },
            )
            OutlinedButton(
                onClick = {
                    scope.launch { shareWorkoutImage(context, cardLayer.toImageBitmap().asAndroidBitmap()) }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Share")
            }

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
private fun SummaryRow(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
