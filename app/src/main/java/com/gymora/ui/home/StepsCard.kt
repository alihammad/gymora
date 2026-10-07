package com.gymora.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gymora.ui.components.Card
import com.gymora.ui.components.TextButton
import java.text.NumberFormat

/**
 * Today's steps as a progress ring, like Google Fit: the count in the middle and
 * an arc that fills toward the daily goal (set in Settings). When the activity
 * recognition permission is missing, it explains why and offers to grant it.
 */
@Composable
fun StepsCard(
    steps: Int?,
    goal: Int,
    permissionGranted: Boolean,
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val format = NumberFormat.getIntegerInstance()
    val count = steps ?: 0
    val progress = if (goal > 0) (count.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val goalReached = permissionGranted && count >= goal
    val description = when {
        !permissionGranted -> "Step counter is off. Permission needed."
        else -> "${format.format(count)} of ${format.format(goal)} steps today" +
            if (goalReached) ", goal reached" else ""
    }
    Card(modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description }) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            StepsRing(
                progress = progress,
                steps = if (permissionGranted) format.format(count) else "–",
                goalReached = goalReached,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (goalReached) "Step goal reached" else "Steps today",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (goalReached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (permissionGranted) {
                    Text(
                        text = "Goal ${format.format(goal)} steps",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (goalReached) {
                            "Nice work today."
                        } else {
                            "${format.format((goal - count).coerceAtLeast(0))} to go"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "Count your steps with your phone's built-in sensor. Nothing leaves your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onGrantPermission) { Text("Turn on step counter") }
                }
            }
        }
    }
}

@Composable
private fun StepsRing(progress: Float, steps: String, goalReached: Boolean) {
    val animated by animateFloatAsState(targetValue = progress, label = "stepsProgress")
    val scheme = MaterialTheme.colorScheme
    val track = scheme.surfaceContainerHighest
    val arc = if (goalReached) scheme.primary else scheme.tertiary
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(RING_SIZE)) {
        Canvas(modifier = Modifier.size(RING_SIZE)) {
            val stroke = RING_STROKE.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (animated > 0f) {
                drawArc(
                    color = arc,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.AutoMirrored.Filled.DirectionsWalk,
                contentDescription = null,
                tint = arc,
                modifier = Modifier.size(20.dp),
            )
            Text(text = steps, style = MaterialTheme.typography.titleLarge)
        }
    }
}

private val RING_SIZE = 112.dp
private val RING_STROKE = 12.dp
