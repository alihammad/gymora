package com.gymora.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gymora.domain.calculator.WeeklyProgress
import com.gymora.ui.components.Card

/**
 * Weekly goal and streak, shown under the week strip: "2 of 3 workouts this
 * week" with one bar segment per goal workout, and the weekly streak.
 */
@Composable
fun WeeklyGoalCard(progress: WeeklyProgress, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val description = buildString {
        append("${progress.done} of ${progress.goal} workouts this week")
        if (progress.goalMet) append(", goal reached")
        append(". ${progress.streakWeeks} week streak.")
    }
    Card(modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description }) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (progress.goalMet) "Weekly goal reached" else "This week",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (progress.goalMet) scheme.primary else scheme.onSurfaceVariant,
                )
                Text(
                    text = "${progress.done} of ${progress.goal} workouts",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(progress.goal) { index ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .background(
                                    if (index < progress.done) scheme.primary else scheme.surfaceContainerHighest,
                                    RoundedCornerShape(3.dp),
                                ),
                        ) {}
                    }
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(start = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (progress.streakWeeks > 0) scheme.primary else scheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = progress.streakWeeks.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                Text(
                    text = "week streak",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}
