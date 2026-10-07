package com.gymora.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import com.gymora.ui.components.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import com.gymora.ui.components.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.gymora.ui.components.OutlinedButton
import com.gymora.ui.components.TextButton
import com.gymora.ui.components.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.data.backup.BackupService
import com.gymora.domain.model.Theme
import com.gymora.health.HealthConnectSync
import com.gymora.ui.components.ConfirmDialog
import java.time.LocalDate
import com.gymora.domain.model.WeightUnit

private val RESTORE_MIME_TYPES = arrayOf(BackupService.MIME_TYPE, "application/octet-stream")
private val IMPORT_MIME_TYPES = arrayOf("text/*", "application/csv", "application/vnd.ms-excel")

/**
 * Settings screen (FR-048, FR-050, FR-051, PRD-§39): weight unit selector,
 * default rest duration, and theme selector — all persisted in Room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> uri?.let(viewModel::onExportCsv) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::onImportCsv) }
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupService.MIME_TYPE),
    ) { uri -> uri?.let(viewModel::onExportBackup) }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::onRestoreFilePicked) }
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(viewModel::onAutoBackupFolderPicked) }
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        HealthConnectSync.permissionContract(),
        viewModel::onHealthPermissionsResult,
    )

    LaunchedEffect(uiState.requestHealthPermissions) {
        if (uiState.requestHealthPermissions) {
            viewModel.onHealthPermissionRequestLaunched()
            healthPermissionLauncher.launch(HealthConnectSync.PERMISSIONS)
        }
    }

    LaunchedEffect(uiState.transferMessage) {
        uiState.transferMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.onTransferMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsSection(title = "Weekly Goal") {
                WeeklyGoalSetting(goal = settings.weeklyGoal, onGoalChanged = viewModel::onWeeklyGoalChanged)
            }

            SettingsSection(title = "Workout Reminders") {
                ReminderSetting(
                    days = settings.reminderDays,
                    time = settings.reminderTime,
                    showTimePicker = uiState.showReminderTimePicker,
                    onDayToggled = viewModel::onReminderDayToggled,
                    onTimeClicked = viewModel::onReminderTimeClicked,
                    onTimeSelected = viewModel::onReminderTimeSelected,
                    onTimeDismissed = viewModel::onReminderTimeDismissed,
                )
            }

            // Weight unit (FR-048)
            SettingsSection(title = "Weight Unit") {
                WeightUnit.entries.forEach { unit ->
                    SettingsRadioRow(
                        label = unit.name,
                        selected = settings.weightUnit == unit,
                        onClick = { viewModel.onWeightUnitSelected(unit) },
                    )
                }
            }

            // Default rest duration (FR-050)
            SettingsSection(title = "Default Rest Duration") {
                val presets = listOf(30, 60, 90, 120, 180)
                presets.forEach { seconds ->
                    val label = when {
                        seconds < 60 -> "${seconds}s"
                        else -> "${seconds / 60} min"
                    }
                    SettingsRadioRow(
                        label = label,
                        selected = settings.defaultRestSeconds == seconds,
                        onClick = { viewModel.onRestDurationSelected(seconds) },
                    )
                }
                // Custom option
                val isCustom = settings.defaultRestSeconds !in presets
                SettingsRadioRow(
                    label = if (isCustom) "Custom (${settings.defaultRestSeconds}s)" else "Custom…",
                    selected = isCustom,
                    onClick = { viewModel.onCustomRestClicked() },
                )
            }

            // Theme (FR-051)
            SettingsSection(title = "Theme") {
                Theme.entries.forEach { theme ->
                    SettingsRadioRow(
                        label = theme.displayName,
                        selected = settings.theme == theme,
                        onClick = { viewModel.onThemeSelected(theme) },
                    )
                }
            }

            // Import / export workouts as CSV
            SettingsSection(title = "Data") {
                Text(
                    text = "Export completed workouts to a CSV file, or import workouts from one " +
                        "(one row per set; columns: Start, End, Workout, Workout Notes, Exercise, " +
                        "Set, Weight, Unit, Reps, Set Notes, Seconds, Distance m, Side).",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("gymora-workouts.csv") },
                        enabled = !uiState.transferInProgress,
                    ) { Text("Export CSV") }
                    OutlinedButton(
                        onClick = { importLauncher.launch(IMPORT_MIME_TYPES) },
                        enabled = !uiState.transferInProgress,
                    ) { Text("Import CSV") }
                }
            }

            SettingsSection(title = "Backup & Restore") {
                BackupSetting(
                    settings = settings,
                    busy = uiState.transferInProgress,
                    onExport = { backupLauncher.launch("gymora-backup-${LocalDate.now()}.zip") },
                    onRestore = { restoreLauncher.launch(RESTORE_MIME_TYPES) },
                    onPickFolder = { folderLauncher.launch(null) },
                    onIntervalSelected = viewModel::onAutoBackupIntervalSelected,
                    onBackupNow = viewModel::onBackupNow,
                )
            }

            SettingsSection(title = "Health Connect") {
                HealthConnectSetting(
                    enabled = settings.healthConnectEnabled,
                    availability = uiState.healthAvailability,
                    permissionsGranted = uiState.healthPermissionsGranted,
                    busy = uiState.transferInProgress,
                    onToggle = viewModel::onHealthConnectToggled,
                    onSyncNow = viewModel::onHealthSyncNow,
                )
            }
        }
    }

    uiState.pendingRestoreUri?.let {
        ConfirmDialog(
            title = "Replace all data?",
            message = "Restoring replaces every workout, routine, exercise, measurement and setting " +
                "on this phone with the backup's. This can't be undone; back up first if unsure.",
            confirmLabel = "Restore",
            dismissLabel = "Cancel",
            onConfirm = { viewModel.onRestoreDecision(confirmed = true) },
            onDismiss = { viewModel.onRestoreDecision(confirmed = false) },
        )
    }

    // Custom rest duration dialog
    if (uiState.showCustomRestDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onCustomRestDismissed,
            title = { Text("Custom Rest Duration") },
            text = {
                OutlinedTextField(
                    value = uiState.customRestSeconds,
                    onValueChange = viewModel::onCustomRestSecondsChanged,
                    label = { Text("Seconds") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::onCustomRestConfirmed,
                    enabled = uiState.customRestSeconds.toIntOrNull()?.let { it > 0 } == true,
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onCustomRestDismissed) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            content()
        }
    }
}

@Composable
private fun SettingsRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
