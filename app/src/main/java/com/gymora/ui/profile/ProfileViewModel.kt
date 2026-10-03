package com.gymora.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.model.WorkoutStat
import com.gymora.domain.repository.HistoryRepository
import com.gymora.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val period: ProfilePeriod = ProfilePeriod.MONTH_1,
    /** Every completed workout, oldest first (calendar). */
    val allWorkouts: List<WorkoutStat> = emptyList(),
    /** Workouts inside [period], oldest first (charts). */
    val chartWorkouts: List<WorkoutStat> = emptyList(),
    val weightUnit: WeightUnit = WeightUnit.KG,
    val isLoading: Boolean = true,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.update { it.copy(weightUnit = settings.weightUnit) }
            }
        }
        refresh()
    }

    fun onPeriodSelected(period: ProfilePeriod) {
        _uiState.update { it.copy(period = period, chartWorkouts = filter(it.allWorkouts, period)) }
    }

    /** Reload workouts from the database (call when the screen is shown). */
    fun refresh() {
        viewModelScope.launch {
            val all = runCatching { historyRepository.getWorkoutStats() }.getOrDefault(emptyList())
            _uiState.update {
                it.copy(allWorkouts = all, chartWorkouts = filter(all, it.period), isLoading = false)
            }
        }
    }

    private fun filter(all: List<WorkoutStat>, period: ProfilePeriod) =
        filterByPeriod(all, period, LocalDate.now(), ZoneId.systemDefault())
}
