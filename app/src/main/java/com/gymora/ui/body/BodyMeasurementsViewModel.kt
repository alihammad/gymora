package com.gymora.ui.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.model.BodyMeasurement
import com.gymora.domain.model.BodyMetric
import com.gymora.domain.model.LatestMeasurements
import com.gymora.domain.model.MetricPoint
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.model.series
import com.gymora.domain.repository.BodyMeasurementRepository
import com.gymora.domain.repository.SettingsRepository
import com.gymora.health.HealthConnectSync
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything recorded on one date, the latest value of each metric that day. */
data class BodyHistoryDay(
    val date: LocalDate,
    val values: Map<BodyMetric, Double>,
    /** The user's own entries that day; empty when the day only has imported weigh-ins. */
    val ownIds: List<Long>,
)

data class BodyMeasurementsUiState(
    /** Metric (kg / cm) when the weight unit is KG, imperial (lb / in) otherwise. */
    val metricUnits: Boolean = true,
    val latest: LatestMeasurements = LatestMeasurements.from(emptyList()),
    val weightHistory: List<MetricPoint> = emptyList(),
    /** Newest date first. */
    val history: List<BodyHistoryDay> = emptyList(),
    /** The date the form records. */
    val selectedDate: LocalDate = LocalDate.now(),
    /** Raw text per field; missing or blank = not measured. */
    val form: Map<BodyMetric, String> = emptyMap(),
    /** The selected date already has an own entry, so saving updates it. */
    val dateHasEntry: Boolean = false,
    /** Fields whose text is not a positive number. */
    val invalid: Set<BodyMetric> = emptySet(),
    val isSaving: Boolean = false,
    val message: String? = null,
)

/**
 * Body measurements, kept one entry per date. The form edits the selected date:
 * picking a date loads what was recorded then, and saving replaces it.
 */
@HiltViewModel
class BodyMeasurementsViewModel @Inject constructor(
    private val repository: BodyMeasurementRepository,
    private val healthConnectSync: HealthConnectSync,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BodyMeasurementsUiState())
    val uiState: StateFlow<BodyMeasurementsUiState> = _uiState.asStateFlow()

    private val zone: ZoneId = ZoneId.systemDefault()
    private var entries: List<BodyMeasurement> = emptyList()
    private var formLoaded = false

    /**
     * Stored values behind the form's loaded text. A field still showing its loaded text
     * saves the stored value, so viewing in lb / in and saving never drifts the numbers.
     */
    private var loaded: Map<BodyMetric, Pair<String, Double>> = emptyMap()

    init {
        viewModelScope.launch {
            combine(repository.observeAll(), settingsRepository.observeSettings()) { all, settings ->
                all to (settings.weightUnit == WeightUnit.KG)
            }.collect { (all, metricUnits) ->
                entries = all
                val unitsChanged = metricUnits != _uiState.value.metricUnits
                _uiState.update {
                    it.copy(
                        metricUnits = metricUnits,
                        latest = LatestMeasurements.from(all),
                        weightHistory = all.series(BodyMetric.WEIGHT, zone),
                        history = historyOf(all),
                    )
                }
                // Load once, then only on request, so edits in progress are never replaced.
                if (!formLoaded || unitsChanged) {
                    formLoaded = true
                    loadForm(_uiState.value.selectedDate)
                }
            }
        }
    }

    fun onDateSelected(date: LocalDate) {
        if (date.isAfter(LocalDate.now(zone))) return
        loadForm(date)
    }

    fun onFieldChanged(metric: BodyMetric, text: String) {
        _uiState.update {
            it.copy(form = it.form + (metric to text), invalid = it.invalid - metric, message = null)
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving) return
        val values = mutableMapOf<BodyMetric, Double>()
        val invalid = mutableSetOf<BodyMetric>()
        BodyMetric.entries.forEach { metric ->
            val text = state.form[metric].orEmpty()
            when (val parsed = BodyUnits.parse(text)) {
                ParsedField.Blank -> Unit
                ParsedField.Invalid -> invalid += metric
                is ParsedField.Value -> values[metric] = loaded[metric]
                    ?.takeIf { (loadedText, _) -> loadedText == text.trim() }?.second
                    ?: BodyUnits.toStored(metric, parsed.value, state.metricUnits)
            }
        }
        when {
            invalid.isNotEmpty() -> {
                _uiState.update { it.copy(invalid = invalid, message = "Check the highlighted values.") }
                return
            }
            values.isEmpty() -> {
                val hint = if (state.dateHasEntry) " To remove this date, delete it from the history." else ""
                _uiState.update { it.copy(message = "Enter at least one value.$hint") }
                return
            }
        }
        val date = state.selectedDate
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching { repository.saveForDate(date, values) }
                .onSuccess { result ->
                    // Weigh-ins that no longer exist leave Health Connect too.
                    val removed = result.removedIds + listOfNotNull(result.id.takeIf { BodyMetric.WEIGHT !in values })
                    healthConnectSync.onBodyMeasurementsChanged(removed)
                    setForm(date, values, hasEntry = true)
                    _uiState.update { it.copy(isSaving = false, message = "Saved for ${describe(date)}.") }
                }
                .onFailure {
                    _uiState.update { it.copy(isSaving = false, message = "Could not save. Please try again.") }
                }
        }
    }

    /** Deletes the user's own entries on [day]; imported weigh-ins stay with Health Connect. */
    fun onDeleteDay(day: BodyHistoryDay) {
        if (day.ownIds.isEmpty()) return
        viewModelScope.launch {
            runCatching { day.ownIds.forEach { repository.delete(it) } }
                .onSuccess {
                    healthConnectSync.onBodyMeasurementsChanged(day.ownIds)
                    if (_uiState.value.selectedDate == day.date) setForm(day.date, emptyMap(), hasEntry = false)
                    _uiState.update { it.copy(message = "Deleted ${describe(day.date)}.") }
                }
                .onFailure {
                    _uiState.update { it.copy(message = "Could not delete. Please try again.") }
                }
        }
    }

    private fun loadForm(date: LocalDate) {
        val own = entries.filter { !it.imported && it.date(zone) == date } // newest first
        val values = BodyMetric.entries.mapNotNull { m -> own.firstNotNullOfOrNull { it[m] }?.let { m to it } }.toMap()
        setForm(date, values, hasEntry = own.isNotEmpty())
    }

    private fun setForm(date: LocalDate, values: Map<BodyMetric, Double>, hasEntry: Boolean) {
        val metricUnits = _uiState.value.metricUnits
        loaded = values.mapValues { (m, v) -> BodyUnits.formText(m, v, metricUnits) to v }
        _uiState.update {
            it.copy(
                selectedDate = date,
                form = loaded.mapValues { (_, pair) -> pair.first },
                dateHasEntry = hasEntry,
                invalid = emptySet(),
            )
        }
    }

    private fun historyOf(all: List<BodyMeasurement>): List<BodyHistoryDay> =
        all.groupBy { it.date(zone) } // keeps newest-first order within and across days
            .map { (date, day) ->
                BodyHistoryDay(
                    date = date,
                    values = BodyMetric.entries
                        .mapNotNull { m -> day.firstNotNullOfOrNull { it[m] }?.let { m to it } }
                        .toMap(),
                    ownIds = day.filterNot { it.imported }.map { it.id },
                )
            }

    private fun describe(date: LocalDate): String =
        if (date == LocalDate.now(zone)) "today" else date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
}
