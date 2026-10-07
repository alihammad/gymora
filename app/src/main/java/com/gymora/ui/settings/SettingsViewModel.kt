package com.gymora.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.data.backup.AutoBackupScheduler
import com.gymora.data.backup.BackupException
import com.gymora.data.backup.BackupService
import com.gymora.data.backup.BackupSummary
import com.gymora.data.transfer.CsvImportException
import com.gymora.data.transfer.CsvImportResult
import com.gymora.data.transfer.WorkoutCsvService
import com.gymora.domain.model.Settings
import com.gymora.domain.model.Theme
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.repository.SettingsRepository
import com.gymora.health.HealthConnectAvailability
import com.gymora.health.HealthConnectSync
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val settings: Settings = Settings.DEFAULTS,
    val showCustomRestDialog: Boolean = false,
    val customRestSeconds: String = "",
    val transferInProgress: Boolean = false,
    val transferMessage: String? = null,
    val showReminderTimePicker: Boolean = false,
    /** A picked backup file waiting for the user to confirm replacing all data. */
    val pendingRestoreUri: Uri? = null,
    val healthAvailability: HealthConnectAvailability = HealthConnectAvailability.NOT_SUPPORTED,
    val healthPermissionsGranted: Boolean = false,
    /** One-shot: the screen should launch Health Connect's permission request. */
    val requestHealthPermissions: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val csvService: WorkoutCsvService,
    private val backupService: BackupService,
    private val healthConnectSync: HealthConnectSync,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        refreshHealthStatus()
    }

    fun onWeightUnitSelected(unit: WeightUnit) {
        viewModelScope.launch { settingsRepository.setWeightUnit(unit) }
    }

    fun onRestDurationSelected(seconds: Int) {
        viewModelScope.launch { settingsRepository.setDefaultRestDuration(seconds) }
    }

    fun onThemeSelected(theme: Theme) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun onWeeklyGoalChanged(goal: Int) {
        viewModelScope.launch { settingsRepository.setWeeklyGoal(goal) }
    }

    /** Adds or removes [day] from the reminder schedule; the app reschedules the alarm on save. */
    fun onReminderDayToggled(day: DayOfWeek) {
        val settings = _uiState.value.settings
        val days = if (day in settings.reminderDays) settings.reminderDays - day else settings.reminderDays + day
        viewModelScope.launch { settingsRepository.setReminder(days, settings.reminderTime) }
    }

    fun onReminderTimeClicked() {
        _uiState.update { it.copy(showReminderTimePicker = true) }
    }

    fun onReminderTimeDismissed() {
        _uiState.update { it.copy(showReminderTimePicker = false) }
    }

    fun onReminderTimeSelected(time: LocalTime) {
        _uiState.update { it.copy(showReminderTimePicker = false) }
        val days = _uiState.value.settings.reminderDays
        viewModelScope.launch { settingsRepository.setReminder(days, time) }
    }

    fun onCustomRestClicked() {
        _uiState.update { it.copy(showCustomRestDialog = true, customRestSeconds = "") }
    }

    fun onCustomRestDismissed() {
        _uiState.update { it.copy(showCustomRestDialog = false) }
    }

    fun onCustomRestConfirmed() {
        val seconds = _uiState.value.customRestSeconds.toIntOrNull()
        if (seconds != null && seconds > 0) {
            viewModelScope.launch { settingsRepository.setDefaultRestDuration(seconds) }
        }
        _uiState.update { it.copy(showCustomRestDialog = false) }
    }

    fun onCustomRestSecondsChanged(value: String) {
        _uiState.update { it.copy(customRestSeconds = value) }
    }

    fun onExportCsv(uri: Uri) = runTransfer("Export failed") {
        val count = withContext(Dispatchers.IO) {
            val out = context.contentResolver.openOutputStream(uri, "wt")
                ?: error("Could not open the destination file")
            csvService.export(out)
        }
        "Exported $count workout${if (count == 1) "" else "s"}."
    }

    fun onImportCsv(uri: Uri) = runTransfer("Import failed") {
        val result = withContext(Dispatchers.IO) {
            val input = context.contentResolver.openInputStream(uri)
                ?: error("Could not open the selected file")
            csvService.import(input)
        }
        describe(result)
    }

    // --- Full backup & restore ---

    fun onExportBackup(uri: Uri) = runTransfer("Backup failed") {
        val summary = withContext(Dispatchers.IO) {
            val out = context.contentResolver.openOutputStream(uri, "wt")
                ?: error("Could not open the destination file")
            out.use { backupService.export(it) }
        }
        "Backed up ${describe(summary)}."
    }

    fun onRestoreFilePicked(uri: Uri) {
        _uiState.update { it.copy(pendingRestoreUri = uri) }
    }

    /** The user answered "Replace all data?"; restore only on [confirmed]. */
    fun onRestoreDecision(confirmed: Boolean) {
        val uri = _uiState.value.pendingRestoreUri ?: return
        _uiState.update { it.copy(pendingRestoreUri = null) }
        if (!confirmed) return
        runTransfer("Restore failed") {
            val summary = withContext(Dispatchers.IO) {
                val input = context.contentResolver.openInputStream(uri)
                    ?: error("Could not open the selected file")
                input.use { backupService.restore(it) }
            }
            "Restored ${describe(summary)}."
        }
    }

    /** Keeps access to the folder across reboots, then turns auto-backup on (weekly by default). */
    fun onAutoBackupFolderPicked(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }.onFailure {
            _uiState.update { it.copy(transferMessage = "Gymora can't keep access to that folder. Pick another.") }
            return
        }
        val interval = _uiState.value.settings.autoBackupIntervalDays.takeIf { it > 0 } ?: DEFAULT_BACKUP_DAYS
        viewModelScope.launch { settingsRepository.setAutoBackup(interval, uri.toString()) }
    }

    fun onAutoBackupIntervalSelected(days: Int) {
        val folder = _uiState.value.settings.autoBackupFolderUri
        viewModelScope.launch { settingsRepository.setAutoBackup(days, folder) }
    }

    fun onBackupNow() {
        AutoBackupScheduler.runNow(context)
        _uiState.update { it.copy(transferMessage = "Backing up in the background…") }
    }

    // --- Health Connect ---

    private fun refreshHealthStatus() {
        viewModelScope.launch {
            val availability = healthConnectSync.availability()
            val granted = runCatching { healthConnectSync.hasAllPermissions() }.getOrDefault(false)
            _uiState.update { it.copy(healthAvailability = availability, healthPermissionsGranted = granted) }
        }
    }

    fun onHealthConnectToggled(enabled: Boolean) {
        if (enabled && !_uiState.value.healthPermissionsGranted) {
            _uiState.update { it.copy(requestHealthPermissions = true) }
            return
        }
        viewModelScope.launch { settingsRepository.setHealthConnectEnabled(enabled) }
        if (enabled) onHealthSyncNow()
    }

    fun onHealthPermissionRequestLaunched() {
        _uiState.update { it.copy(requestHealthPermissions = false) }
    }

    fun onHealthPermissionsResult(granted: Set<String>) {
        val all = granted.containsAll(HealthConnectSync.PERMISSIONS)
        _uiState.update { it.copy(healthPermissionsGranted = all) }
        if (all) {
            viewModelScope.launch { settingsRepository.setHealthConnectEnabled(true) }
            onHealthSyncNow()
        } else {
            _uiState.update {
                it.copy(transferMessage = "Health Connect sync needs all of the requested permissions.")
            }
        }
    }

    fun onHealthSyncNow() = runTransfer("Health Connect sync failed") {
        val r = withContext(Dispatchers.IO) { healthConnectSync.syncAll() }
        "Health Connect: wrote ${r.workoutsWritten} workout(s) and ${r.weightsWritten} weigh-in(s), " +
            "imported ${r.weightsImported} weigh-in(s)."
    }

    fun onTransferMessageShown() {
        _uiState.update { it.copy(transferMessage = null) }
    }

    private fun runTransfer(failurePrefix: String, block: suspend () -> String) {
        if (_uiState.value.transferInProgress) return
        _uiState.update { it.copy(transferInProgress = true) }
        viewModelScope.launch {
            val message = try {
                block()
            } catch (e: CsvImportException) {
                "$failurePrefix: ${e.message}"
            } catch (e: BackupException) {
                "$failurePrefix: ${e.message}"
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                "$failurePrefix: ${e.message ?: "unexpected error"}"
            }
            _uiState.update { it.copy(transferInProgress = false, transferMessage = message) }
        }
    }

    private fun describe(s: BackupSummary): String =
        "${s.workouts} workout(s), ${s.routines} routine(s), ${s.exercises} exercise(s), " +
            "${s.measurements} measurement(s) and ${s.mediaFiles} image(s)"

    private fun describe(r: CsvImportResult): String = buildString {
        append("Imported ${r.workoutsImported} workout${if (r.workoutsImported == 1) "" else "s"}")
        if (r.exercisesCreated > 0) append(", created ${r.exercisesCreated} new exercise(s)")
        append(".")
        if (r.duplicatesSkipped > 0) append(" ${r.duplicatesSkipped} already existed.")
        if (r.rowsSkipped > 0) {
            append(" ${r.rowsSkipped} row(s) skipped (")
            append(r.errors.joinToString("; ")).append(").")
        }
    }

    private companion object {
        const val DEFAULT_BACKUP_DAYS = 7
    }
}
