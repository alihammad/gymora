package com.gymora.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.model.EquipmentUsageType
import com.gymora.domain.model.ProgressSeries
import com.gymora.domain.repository.ExerciseRepository
import com.gymora.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProgressPeriod(val label: String, val days: Long?) {
    DAYS_30("30 days", 30),
    DAYS_90("90 days", 90),
    ALL("All time", null),
}

/** A row in the overview: a series plus a subtitle such as the equipment. */
data class ProgressRow(val series: ProgressSeries, val subtitle: String?)

data class ProgressUiState(
    val period: ProgressPeriod = ProgressPeriod.DAYS_30,
    val exercises: List<ProgressRow> = emptyList(),
    val routines: List<ProgressRow> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        load(ProgressPeriod.DAYS_30)
    }

    fun onPeriodSelected(period: ProgressPeriod) = load(period)

    private fun load(period: ProgressPeriod) {
        _uiState.update { it.copy(period = period, isLoading = true) }
        viewModelScope.launch {
            val since = period.days?.let { Instant.now().minus(it, ChronoUnit.DAYS).toEpochMilli() } ?: 0L
            val library = runCatching { exerciseRepository.observeLibrary().first() }
                .getOrDefault(emptyList()).associateBy { it.id }
            val exercises = runCatching { historyRepository.getExerciseProgress(since) }
                .getOrDefault(emptyList())
                .sortedByDescending { it.values.size }
                .map { series ->
                    val equipment = library[series.id]?.equipment?.firstOrNull()?.let {
                        if (it.usageType == EquipmentUsageType.DOUBLE) "${it.name} (Double)" else it.name
                    }
                    ProgressRow(series, equipment)
                }
            val routines = runCatching { historyRepository.getRoutineProgressSince(since) }
                .getOrDefault(emptyList())
                .sortedByDescending { it.values.size }
                .map { ProgressRow(it, null) }
            // Ignore a stale result if the user already picked another period.
            _uiState.update {
                if (it.period == period) {
                    it.copy(exercises = exercises, routines = routines, isLoading = false)
                } else {
                    it
                }
            }
        }
    }
}
