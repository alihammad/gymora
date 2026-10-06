package com.gymora.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.model.WorkoutSummary
import com.gymora.domain.model.SessionRecord
import com.gymora.domain.repository.RecordsRepository
import com.gymora.domain.repository.SettingsRepository
import com.gymora.domain.usecase.GetEngagementUseCase
import com.gymora.domain.repository.WorkoutSessionRepository
import com.gymora.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for the workout summary screen (FR-035). */
data class WorkoutSummaryUiState(
    val summary: WorkoutSummary? = null,
    val isLoading: Boolean = true,
    val displayUnit: WeightUnit = WeightUnit.KG,
    /** Personal records set in this workout (share card). */
    val records: List<SessionRecord> = emptyList(),
    val streakWeeks: Int = 0,
)

@HiltViewModel
class WorkoutSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val settingsRepository: SettingsRepository,
    private val recordsRepository: RecordsRepository,
    private val getEngagement: GetEngagementUseCase,
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<String>(Destinations.WorkoutSummary.ARG)
        ?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(WorkoutSummaryUiState())
    val uiState: StateFlow<WorkoutSummaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.observeSettings().first()
            _uiState.update { it.copy(displayUnit = settings.weightUnit) }
            runCatching { workoutSessionRepository.getSummary(sessionId) }
                .onSuccess { summary ->
                    _uiState.update { it.copy(summary = summary, isLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
        // Extras for the share card; the summary shows without them if they fail.
        viewModelScope.launch {
            val records = runCatching { recordsRepository.getSessionRecords(sessionId) }.getOrDefault(emptyList())
            val streak = runCatching { getEngagement().progress.streakWeeks }.getOrDefault(0)
            _uiState.update { it.copy(records = records, streakWeeks = streak) }
        }
    }
}
