package com.gymora.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.data.transfer.CsvImportException
import com.gymora.data.transfer.CsvImportResult
import com.gymora.data.transfer.WorkoutCsvService
import com.gymora.domain.model.Settings
import com.gymora.domain.model.Theme
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val csvService: WorkoutCsvService,
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
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                "$failurePrefix: ${e.message ?: "unexpected error"}"
            }
            _uiState.update { it.copy(transferInProgress = false, transferMessage = message) }
        }
    }

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
}
