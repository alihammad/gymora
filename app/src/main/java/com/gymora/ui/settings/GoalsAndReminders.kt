package com.gymora.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gymora.domain.model.Settings
import com.gymora.ui.components.TextButton
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Weekly goal stepper (drives the Home card, streak and widget). */
@Composable
fun WeeklyGoalSetting(goal: Int, onGoalChanged: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Workouts per week", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Hit it every week to build your streak.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { onGoalChanged(goal - 1) }, enabled = goal > Settings.MIN_WEEKLY_GOAL) {
            Icon(Icons.Filled.Remove, contentDescription = "Fewer workouts per week")
        }
        Text(goal.toString(), style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = { onGoalChanged(goal + 1) }, enabled = goal < Settings.MAX_WEEKLY_GOAL) {
            Icon(Icons.Filled.Add, contentDescription = "More workouts per week")
        }
    }
}

/**
 * Reminder schedule: one toggle per weekday plus the time of day. Turning the
 * first day on asks for the notification permission on Android 13+.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSetting(
    days: Set<DayOfWeek>,
    time: LocalTime,
    showTimePicker: Boolean,
    onDayToggled: (DayOfWeek) -> Unit,
    onTimeClicked: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    onTimeDismissed: () -> Unit,
) {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(hasNotificationPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> permissionGranted = granted }
    val timeText = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { day ->
                DayToggle(
                    day = day,
                    selected = day in days,
                    onClick = {
                        val enabling = days.isEmpty() && day !in days
                        if (enabling && !permissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        onDayToggled(day)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onTimeClicked)
                .padding(vertical = 8.dp),
        ) {
            Text("Remind me at", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(timeText, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = when {
                days.isEmpty() -> "Off. Pick the days you plan to train."
                !permissionGranted ->
                    "Notifications are blocked for Gymora. Allow them in system settings to get reminders."
                else -> "${describeDays(days)} at $timeText, unless you've already trained that day."
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (days.isNotEmpty() && !permissionGranted) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute)
        AlertDialog(
            onDismissRequest = onTimeDismissed,
            title = { Text("Reminder time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = { onTimeSelected(LocalTime.of(state.hour, state.minute)) }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = onTimeDismissed) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DayToggle(day: DayOfWeek, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val fullName = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .widthIn(max = 44.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(if (selected) scheme.primary else scheme.surfaceContainerHigh)
            .then(if (selected) Modifier else Modifier.border(1.dp, scheme.outline, CircleShape))
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics {
                contentDescription = "$fullName reminder"
                stateDescription = if (selected) "On" else "Off"
            },
    ) {
        Text(
            text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) scheme.onPrimary else scheme.onSurface,
        )
    }
}

private fun describeDays(days: Set<DayOfWeek>): String = when (days) {
    DayOfWeek.entries.toSet() -> "Every day"
    DayOfWeek.entries.take(5).toSet() -> "Weekdays"
    else -> days.sorted().joinToString { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}

private fun hasNotificationPermission(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
