package com.gymora.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymora.ui.components.OutlinedButton
import com.gymora.ui.components.TextButton
import com.gymora.ui.theme.DisplayMetric
import com.gymora.ui.theme.GymoraShapes
import com.gymora.ui.theme.LabelCaps

/**
 * Rest timer panel shown during an active workout after a set is completed
 * (FR-031, FR-032): large lime countdown with Skip, +30s and Restart.
 * Independent of the workout duration ticker.
 */
@Composable
fun RestTimerBar(
    state: RestTimerState,
    onSkip: () -> Unit,
    onAdd30s: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isRunning) return

    val minutes = state.remainingSeconds / 60
    val seconds = state.remainingSeconds % 60
    val clock = "%02d:%02d".format(minutes, seconds)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(GymoraShapes.card)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f).semantics { contentDescription = "Rest timer, $clock remaining" }) {
                Text("SET REST TIMER", style = LabelCaps, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = clock,
                    style = DisplayMetric.copy(fontSize = 48.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            TextButton(onClick = onSkip) { Text("Skip") }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onAdd30s, modifier = Modifier.weight(1f)) { Text("+30s") }
            OutlinedButton(onClick = onRestart, modifier = Modifier.weight(1f)) { Text("Restart") }
        }
    }
}
