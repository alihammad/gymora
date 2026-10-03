package com.gymora.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.gymora.ui.components.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import com.gymora.ui.components.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Rest timer bar shown during active workout after a set is completed
 * (FR-031, FR-032). Independent of the workout duration ticker.
 * Actions: Skip, +30s, Restart.
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Rest: ${formatElapsed(state.remainingSeconds)}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onSkip) {
            Text("Skip")
        }
        OutlinedButton(onClick = onAdd30s) {
            Text("+30s")
        }
        OutlinedButton(onClick = onRestart) {
            Text("Restart")
        }
    }
}
