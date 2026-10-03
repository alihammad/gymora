package com.gymora.ui.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.calculator.WorkoutCalculators
import com.gymora.domain.model.BodyMeasurement
import com.gymora.domain.model.LatestMeasurements
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.repository.BodyMeasurementRepository
import com.gymora.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Raw text of each form field; blank = not provided. */
data class MeasurementForm(
    val weight: String = "",
    val shoulders: String = "",
    val chest: String = "",
    val aboveNavel: String = "",
    val navel: String = "",
    val belowNavel: String = "",
    val thigh: String = "",
)

data class BodyMeasurementsUiState(
    /** Imperial (lb / inches) when the weight unit is LB, metric (kg / cm) otherwise. */
    val metric: Boolean = true,
    val latest: LatestMeasurements = LatestMeasurements.from(emptyList()),
    /** Weight entries, oldest first, in kg. */
    val weightHistory: List<Pair<Instant, Double>> = emptyList(),
    val form: MeasurementForm = MeasurementForm(),
    val message: String? = null,
)

@HiltViewModel
class BodyMeasurementsViewModel @Inject constructor(
    private val repository: BodyMeasurementRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BodyMeasurementsUiState())
    val uiState: StateFlow<BodyMeasurementsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.observeAll(), settingsRepository.observeSettings()) { entries, settings ->
                entries to settings.weightUnit
            }.collect { (entries, unit) ->
                _uiState.update {
                    it.copy(
                        metric = unit == WeightUnit.KG,
                        latest = LatestMeasurements.from(entries),
                        weightHistory = entries.reversed()
                            .mapNotNull { e -> e.weightKg?.let { w -> e.measuredAt to w } },
                    )
                }
            }
        }
    }

    fun onFormChanged(form: MeasurementForm) {
        _uiState.update { it.copy(form = form, message = null) }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    fun onSave() {
        val state = _uiState.value
        val f = state.form
        val lengthToCm: (Double) -> Double = { v -> if (state.metric) v else v * CM_PER_INCH }
        val weightToKg: (Double) -> Double = { v ->
            if (state.metric) v else WorkoutCalculators.convertWeight(v, WeightUnit.LB, WeightUnit.KG)
        }
        fun parse(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

        val entry = BodyMeasurement(
            id = 0,
            measuredAt = Instant.now(),
            weightKg = parse(f.weight)?.let(weightToKg),
            shouldersCm = parse(f.shoulders)?.let(lengthToCm),
            chestCm = parse(f.chest)?.let(lengthToCm),
            aboveNavelCm = parse(f.aboveNavel)?.let(lengthToCm),
            navelCm = parse(f.navel)?.let(lengthToCm),
            belowNavelCm = parse(f.belowNavel)?.let(lengthToCm),
            thighCm = parse(f.thigh)?.let(lengthToCm),
        )
        if (entry.isEmpty) {
            _uiState.update { it.copy(message = "Enter at least one value.") }
            return
        }
        viewModelScope.launch {
            runCatching { repository.save(entry) }
                .onSuccess {
                    _uiState.update { it.copy(form = MeasurementForm(), message = "Measurements saved.") }
                }
                .onFailure {
                    _uiState.update { it.copy(message = "Could not save. Please try again.") }
                }
        }
    }

    companion object {
        const val CM_PER_INCH = 2.54
    }
}
