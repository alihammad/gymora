package com.gymora.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.repository.HistoryRepository
import com.gymora.domain.repository.RoutineRepository
import com.gymora.domain.repository.SettingsRepository
import com.gymora.domain.repository.StepRepository
import com.gymora.domain.usecase.GetEngagementUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val historyRepository: HistoryRepository,
    private val getEngagement: GetEngagementUseCase,
    private val workoutSessionRepository: com.gymora.domain.repository.WorkoutSessionRepository,
    private val stepRepository: StepRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(stepsSupported = stepRepository.isSupported))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var stepJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.update { it.copy(stepGoal = settings.stepGoal) }
            }
        }
        viewModelScope.launch {
            routineRepository.observeAll().collect { routines ->
                _uiState.update { it.copy(routines = routines, isLoading = false) }
            }
        }
        viewModelScope.launch {
            workoutSessionRepository.observeActiveSession().collect { active ->
                _uiState.update { it.copy(activeWorkout = active) }
            }
        }
        loadRecentWorkouts()
    }

    /**
     * (Re)starts step tracking. [sensorAllowed] is whether the activity-recognition
     * permission is held; Health Connect steps, if the user turned them on, count either way.
     * The count stays null (the card asks for permission) when no source is available.
     */
    fun startStepTracking(sensorAllowed: Boolean) {
        stepJob?.cancel()
        stepJob = viewModelScope.launch {
            stepRepository.observeTodaySteps(sensorAllowed).collect { steps ->
                _uiState.update { it.copy(stepsToday = steps) }
            }
        }
    }

    /** The 3 most recent completed workouts, plus the weekly goal card (spec Assumption, FR-002, T050a). */
    fun loadRecentWorkouts() {
        viewModelScope.launch {
            val recent = historyRepository.listCompleted(limit = RECENT_COUNT, offset = 0)
            _uiState.update { it.copy(recentWorkouts = recent) }
            loadWorkoutDays()
            val progress = getEngagement().progress
            _uiState.update { it.copy(weeklyProgress = progress) }
        }
    }

    /** Move the calendar strip by [weeks] (negative = earlier). */
    fun onShiftWeek(weeks: Long) {
        _uiState.update { it.copy(weekStart = it.weekStart.plusWeeks(weeks), workoutDays = emptySet()) }
        viewModelScope.launch { loadWorkoutDays() }
    }

    /** Show the exercises performed on [day] (tapped in the calendar strip). */
    fun onDaySelected(day: java.time.LocalDate) {
        _uiState.update { it.copy(selectedDay = day, selectedDayWorkouts = emptyList()) }
        viewModelScope.launch {
            val workouts = historyRepository.getWorkoutsOn(day)
            _uiState.update { if (it.selectedDay == day) it.copy(selectedDayWorkouts = workouts) else it }
        }
    }

    fun onDayDismissed() {
        _uiState.update { it.copy(selectedDay = null, selectedDayWorkouts = emptyList()) }
    }

    private suspend fun loadWorkoutDays() {
        val start = _uiState.value.weekStart
        val days = historyRepository.completedDays(start, start.plusDays(7))
        // Ignore the result if the user already moved to another week.
        _uiState.update { if (it.weekStart == start) it.copy(workoutDays = days) else it }
    }

    /** Persist the new home-screen order (FR-015). */
    fun onReorder(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value.routines.toMutableList()
        val moved = current.removeAt(fromIndex)
        current.add(toIndex, moved)
        _uiState.update { it.copy(routines = current) }
        viewModelScope.launch {
            routineRepository.reorder(current.map { it.id })
        }
    }

    companion object {
        const val RECENT_COUNT = 3
    }
}
