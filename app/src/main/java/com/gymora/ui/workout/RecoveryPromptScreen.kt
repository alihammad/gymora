package com.gymora.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import com.gymora.ui.components.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.gymora.ui.components.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymora.domain.calculator.WorkoutCalculators
import com.gymora.domain.model.ActiveWorkout
import com.gymora.ui.components.ConfirmDialog
import java.time.Instant

/**
 * Recovery prompt (FR-038): "Workout in progress — <name>, started X ago"
 * with a RESUME action, shown on launch when an unfinished workout exists.
 * Discard requires explicit confirmation (FR-037).
 */
@Composable
fun RecoveryPromptDialog(
    workout: ActiveWorkout,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showDiscardConfirm by remember { mutableStateOf(false) }

    val startedAgoMinutes = WorkoutCalculators
        .elapsed(workout.startedAt, Instant.now())
        .toMinutes()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Workout in progress") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${workout.session.routineNameSnapshot}, " +
                        "started $startedAgoMinutes minutes ago",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        },
        confirmButton = {
            Button(onClick = onResume) {
                Text("RESUME")
            }
        },
        dismissButton = {
            TextButton(onClick = { showDiscardConfirm = true }) {
                Text("Discard")
            }
        },
    )

    if (showDiscardConfirm) {
        ConfirmDialog(
            title = "Discard workout?",
            message = "Your progress will be permanently deleted.",
            confirmLabel = "Discard workout",
            dismissLabel = "Keep working out",
            onConfirm = {
                showDiscardConfirm = false
                onDiscard()
            },
            onDismiss = { showDiscardConfirm = false },
        )
    }
}
