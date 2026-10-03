package com.gymora.ui.progress

import com.gymora.ui.components.GymoraLoading
import com.gymora.ui.theme.GymoraShapes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.gymora.ui.components.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import com.gymora.ui.components.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.gymora.ui.components.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.ui.components.Sparkline
import java.util.Locale

/**
 * Progress overview: every exercise and routine trained in the chosen period
 * with a sparkline and the change since its first session. Tap for details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onBack: () -> Unit,
    onExerciseClick: (Long) -> Unit,
    onRoutineClick: (Long) -> Unit,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

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
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProgressPeriod.entries.forEach { p ->
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
            } else if (state.exercises.isEmpty() && state.routines.isEmpty()) {
                item {
                    Text(
                        "No completed workouts in this period yet. Finish a workout to see progress here.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            } else {
                if (state.exercises.isNotEmpty()) {
                    item { SectionTitle("Exercises · est. 1RM") }
                    item { RowsCard(state.exercises, onExerciseClick) }
                }
                if (state.routines.isNotEmpty()) {
                    item { SectionTitle("Routines · volume") }
                    item { RowsCard(state.routines, onRoutineClick) }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun RowsCard(rows: List<ProgressRow>, onClick: (Long) -> Unit) {
    Card(
        shape = GymoraShapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
