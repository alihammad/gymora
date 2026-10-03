package com.gymora.ui.records

import com.gymora.ui.components.GymoraLoading
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.domain.calculator.WorkoutCalculators
import com.gymora.domain.model.PersonalRecord
import com.gymora.domain.model.WeightUnit
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Personal records screen (FR-047, PRD-§18): heaviest weight, highest reps,
 * best estimated 1RM (Epley), and largest workout volume — each with
 * exercise name and date context.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    onBack: () -> Unit,
    viewModel: RecordsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personal Records") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            GymoraLoading(modifier = Modifier.padding(innerPadding).padding(16.dp))
        } else {
            val records = uiState.records
            val displayUnit = uiState.displayUnit
            val unitLabel = displayUnit.name.lowercase()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RecordCard(
                    title = "Heaviest Weight",
                    record = records?.heaviestWeight,
                    formatValue = { "%.1f %s".format(toDisplay(it, displayUnit), unitLabel) },
                )
                RecordCard(
                    title = "Most Reps",
                    record = records?.highestReps,
                    formatValue = { "%.0f reps".format(it) },
                )
                RecordCard(
                    title = "Best Estimated 1RM (Epley)",
                    record = records?.bestEstimatedOneRepMax,
                    formatValue = { "%.1f %s".format(toDisplay(it, displayUnit), unitLabel) },
                )
                RecordCard(
                    title = "Largest Workout Volume",
                    record = records?.largestWorkoutVolume,
                    formatValue = { "%.1f %s".format(toDisplay(it, displayUnit), unitLabel) },
                )
            }
        }
    }
}

/**
 * Converts a record value from the canonical KG unit to the user's display unit.
 * Records are computed in KG at the data boundary (FR-049, R-04).
 */
private fun toDisplay(kgValue: Double, displayUnit: WeightUnit): Double =
    if (displayUnit == WeightUnit.KG) {
        kgValue
    } else {
        WorkoutCalculators.convertWeight(kgValue, WeightUnit.KG, WeightUnit.LB)
    }

@Composable
private fun RecordCard(
    title: String,
    record: PersonalRecord?,
    formatValue: (Double) -> String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            if (record != null) {
                Text(
                    text = formatValue(record.value),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = record.exerciseName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = DateTimeFormatter.ofPattern("d MMM yyyy")
                        .format(record.date.atZone(ZoneId.systemDefault())),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "No data yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
