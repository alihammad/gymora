package com.gymora.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.calculator.WorkoutCalculators
import com.gymora.domain.model.ActiveSet
import com.gymora.domain.model.ActiveWorkout
import com.gymora.domain.model.ActiveWorkoutConflictException
import com.gymora.domain.model.EntityNotFoundException
import com.gymora.domain.model.ValidationException
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.repository.ExerciseRepository
import com.gymora.domain.repository.SettingsRepository
import com.gymora.domain.usecase.DiscardWorkoutUseCase
import com.gymora.domain.usecase.FinishWorkoutUseCase
import com.gymora.domain.usecase.LogSetUseCase
import com.gymora.domain.usecase.ModifySessionStructureUseCase
import com.gymora.domain.usecase.PreviousPerformanceUseCase
import com.gymora.domain.usecase.StartWorkoutUseCase
import com.gymora.ui.components.SetEntry
import com.gymora.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val startWorkoutUseCase: StartWorkoutUseCase,
    private val logSetUseCase: LogSetUseCase,
    private val modifySessionStructureUseCase: ModifySessionStructureUseCase,
    private val finishWorkoutUseCase: FinishWorkoutUseCase,
    private val discardWorkoutUseCase: DiscardWorkoutUseCase,
    private val exerciseRepository: ExerciseRepository,
    private val workoutSessionRepository: com.gymora.domain.repository.WorkoutSessionRepository,
    private val previousPerformanceUseCase: PreviousPerformanceUseCase,
    private val settingsRepository: SettingsRepository,
    private val healthConnectSync: com.gymora.health.HealthConnectSync,
) : ViewModel() {

    private var sessionId: Long = savedStateHandle.get<String>(Destinations.ActiveWorkout.ARG)
        ?.toLongOrNull() ?: NEW_SESSION_ID

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        loadRestDefault()
        if (sessionId == NEW_SESSION_ID) {
            // Started via START on a routine card: routineId is passed via a
            // separate saved-state key set by the navigation caller.
            val routineId = savedStateHandle.get<String>(ARG_ROUTINE_ID)?.toLongOrNull()
            val adHocExerciseId = savedStateHandle.get<String>(ARG_EXERCISE_ID)?.toLongOrNull()
            if (routineId != null) {
                startWorkout { startWorkoutUseCase(routineId) }
            } else if (adHocExerciseId != null) {
                startWorkout { startWorkoutUseCase.startAdHoc(adHocExerciseId) }
            }
        } else {
            loadSession()
        }
    }

    private fun startWorkout(start: suspend () -> Long) {
        viewModelScope.launch {
            runCatching { start() }
                .onSuccess { id ->
                    sessionId = id
                    loadSession()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = friendlyMessage(error))
                    }
                }
        }
    }

    private fun loadSession() {
        viewModelScope.launch {
            runCatching { workoutSessionRepository.getActiveWorkout(sessionId) }
                .onSuccess { workout ->
                    _uiState.update { it.copy(activeWorkout = workout, isLoading = false) }
                    startTicker(workout.startedAt)
                    loadPreviousPerformance(workout)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = friendlyMessage(error))
                    }
                }
        }
    }

    /**
     * FR-045 / FR-046: fetch the most recent completed performance for each
     * exercise in the active workout so the UI can show previous values and
     * pre-fill today's fields.
     */
    private fun loadPreviousPerformance(workout: ActiveWorkout) {
        viewModelScope.launch {
            val map = mutableMapOf<Long, com.gymora.domain.model.PreviousPerformance>()
            for (exercise in workout.exercises) {
                val id = exercise.exerciseId ?: continue
                previousPerformanceUseCase(id)?.let { perf ->
                    map[id] = perf
                }
            }
            _uiState.update { it.copy(previousPerformanceMap = map) }
        }
    }

    // --- Rest timer (FR-031, FR-032, R-06) ---

    private var restTimerJob: Job? = null

    private fun loadRestDefault() {
        viewModelScope.launch {
            settingsRepository.observeSettings().first().let { settings ->
                _uiState.update {
                    it.copy(
                        restTimer = it.restTimer.copy(defaultSeconds = settings.defaultRestSeconds),
                        weightUnit = settings.weightUnit,
                    )
                }
            }
        }
    }

    /** Start rest countdown after a set is completed (FR-031). */
    fun onSetCompleted() {
        val defaultSeconds = _uiState.value.restTimer.defaultSeconds
        val endInstant = System.currentTimeMillis() + defaultSeconds * 1000L
        _uiState.update {
            it.copy(
                restTimer = RestTimerState(
                    isRunning = true,
                    endInstantMs = endInstant,
                    defaultSeconds = defaultSeconds,
                ),
            )
        }
        startRestTicker()
    }

    /** Skip the rest timer (FR-032). */
    fun onRestTimerSkip() {
        stopRestTicker()
        _uiState.update {
            it.copy(restTimer = RestTimerState(defaultSeconds = it.restTimer.defaultSeconds))
        }
    }

    /** Add 30 seconds to the current rest timer (FR-032). */
    fun onRestTimerAdd30s() {
        val current = _uiState.value.restTimer
        val newEnd = (current.endInstantMs ?: System.currentTimeMillis()) + 30_000L
        _uiState.update { it.copy(restTimer = current.copy(endInstantMs = newEnd)) }
    }

    /** Restart the rest timer from the default duration (FR-032). */
    fun onRestTimerRestart() {
        val defaultSeconds = _uiState.value.restTimer.defaultSeconds
        val endInstant = System.currentTimeMillis() + defaultSeconds * 1000L
        _uiState.update {
            it.copy(
                restTimer = RestTimerState(
                    isRunning = true,
                    endInstantMs = endInstant,
                    defaultSeconds = defaultSeconds,
                ),
            )
        }
        startRestTicker()
    }

    private fun startRestTicker() {
        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch {
            while (isActive) {
                val timer = _uiState.value.restTimer
                val endMs = timer.endInstantMs
                if (endMs == null || !timer.isRunning) {
                    stopRestTicker()
                    return@launch
                }
                val remaining = (endMs - System.currentTimeMillis()) / 1000
                if (remaining <= 0) {
                    _uiState.update {
                        it.copy(restTimer = RestTimerState(defaultSeconds = timer.defaultSeconds))
                    }
                    stopRestTicker()
                    return@launch
                }
                _uiState.update { it.copy(restTimer = timer.copy(remainingSeconds = remaining)) }
                delay(1_000)
            }
        }
    }

    private fun stopRestTicker() {
        restTimerJob?.cancel()
        restTimerJob = null
    }

    /**
     * BR-07 / FR-021: the ticker recomputes elapsed time from timestamps every
     * second; nothing is counted in memory.
     */
    private fun startTicker(startedAt: Instant) {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = WorkoutCalculators.elapsed(startedAt, Instant.now())
                _uiState.update { it.copy(elapsedSeconds = elapsed.seconds) }
                delay(1_000)
            }
        }
    }

    /** Keeps edits in order: a later keystroke's write never lands before an earlier one. */
    private val editMutex = Mutex()

    /** Any value field of a set changed; saved immediately (FR-025, FR-027). */
    fun onSetValuesChanged(setId: Long, entry: SetEntry) {
        val set = findSet(setId) ?: return
        val unit = set.weightUnit ?: _uiState.value.weightUnit
        val updated = set.copy(
            weight = entry.weight,
            weightUnit = if (entry.weight != null) unit else set.weightUnit,
            reps = entry.reps,
            durationSeconds = entry.durationSeconds,
            distanceMeters = entry.distanceMeters,
        )
        applyLocally(setId) { updated }
        persistSet(updated)
    }

    /**
     * Updates the in-memory set without reloading from the database, so the
     * text fields keep the cursor and the other field's latest value is not lost.
     */
    private fun applyLocally(setId: Long, change: (ActiveSet) -> ActiveSet) {
        _uiState.update { state ->
            val workout = state.activeWorkout ?: return@update state
            state.copy(
                activeWorkout = workout.copy(
                    exercises = workout.exercises.map { exercise ->
                        exercise.copy(
                            sets = exercise.sets.map { if (it.id == setId) change(it) else it },
                        )
                    },
                ),
            )
        }
    }

    private fun persistSet(set: ActiveSet) {
        viewModelScope.launch {
            editMutex.withLock {
                runCatching {
                    logSetUseCase.updateValues(
                        set.id, set.weight, set.weightUnit, set.reps, set.durationSeconds, set.distanceMeters,
                    )
                }
                    .onFailure { error ->
                        _uiState.update { it.copy(errorMessage = friendlyMessage(error)) }
                    }
            }
        }
    }

    fun onToggleComplete(setId: Long, completed: Boolean) {
        viewModelScope.launch {
            if (completed) logSetUseCase.complete(setId) else logSetUseCase.uncomplete(setId)
            loadSession()
            if (completed) onSetCompleted()
        }
    }

    fun onAddSet(workoutExerciseId: Long) {
        viewModelScope.launch {
            modifySessionStructureUseCase.addSet(workoutExerciseId)
            loadSession()
        }
    }

    fun onAddExerciseClicked() {
        viewModelScope.launch {
            val exercises = exerciseRepository.observeLibrary().first()
            _uiState.update { it.copy(showExercisePicker = true, libraryExercises = exercises) }
        }
    }

    fun onExercisePicked(exerciseId: Long) {
        // FR-028: ask "Add to routine" vs "This workout only".
        _uiState.update { it.copy(showExercisePicker = false, pendingAddExerciseId = exerciseId) }
    }

    fun onAddToRoutineDecision(addToRoutine: Boolean) {
        val exerciseId = _uiState.value.pendingAddExerciseId ?: return
        viewModelScope.launch {
            modifySessionStructureUseCase.addExercise(sessionId, exerciseId, addToRoutine)
            _uiState.update { it.copy(pendingAddExerciseId = null) }
            loadSession()
        }
    }

    fun onRemoveExercise(workoutExerciseId: Long) {
        viewModelScope.launch {
            modifySessionStructureUseCase.removeExercise(workoutExerciseId)
            loadSession()
        }
    }

    fun onMoveExercise(fromIndex: Int, toIndex: Int) {
        val exercises = _uiState.value.activeWorkout?.exercises ?: return
        if (fromIndex == toIndex || fromIndex !in exercises.indices || toIndex !in exercises.indices) return
        val ids = exercises.map { it.workoutExerciseId }.toMutableList()
        ids.add(toIndex, ids.removeAt(fromIndex))
        viewModelScope.launch {
            modifySessionStructureUseCase.reorderExercises(sessionId, ids)
            loadSession()
        }
    }

    fun onFinishRequested() {
        _uiState.update { it.copy(showFinishConfirm = true) }
    }

    fun onFinishConfirmed() {
        viewModelScope.launch {
            runCatching { finishWorkoutUseCase(sessionId) }
                .onSuccess {
                    healthConnectSync.onWorkoutFinished(sessionId)
                    tickerJob?.cancel()
                    _uiState.update {
                        it.copy(showFinishConfirm = false, finishedSessionId = sessionId)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(showFinishConfirm = false, errorMessage = friendlyMessage(error))
                    }
                }
        }
    }

    fun onFinishDismissed() {
        _uiState.update { it.copy(showFinishConfirm = false) }
    }

    fun onCancelRequested() {
        _uiState.update { it.copy(showCancelConfirm = true) }
    }

    fun onDiscardConfirmed() {
        viewModelScope.launch {
            runCatching { discardWorkoutUseCase(sessionId) }
                .onSuccess {
                    tickerJob?.cancel()
                    _uiState.update { it.copy(showCancelConfirm = false, finishedSessionId = -1L) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(showCancelConfirm = false, errorMessage = friendlyMessage(error))
                    }
                }
        }
    }

    fun onKeepWorkingOut() {
        _uiState.update { it.copy(showCancelConfirm = false) }
    }

    fun onExercisePickerDismissed() {
        _uiState.update { it.copy(showExercisePicker = false) }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun findSet(setId: Long) = _uiState.value.activeWorkout
        ?.exercises
        ?.flatMap { it.sets }
        ?.firstOrNull { it.id == setId }

    private fun friendlyMessage(error: Throwable): String = when (error) {
        is ValidationException -> error.message ?: "Please check your input."
        is EntityNotFoundException -> "This workout no longer exists."
        is ActiveWorkoutConflictException -> "A workout is already in progress."
        else -> "Something went wrong. Please try again." // FR-060: no stack traces
    }

    companion object {
        const val NEW_SESSION_ID = 0L
        const val ARG_ROUTINE_ID = "routineId"
        const val ARG_EXERCISE_ID = "exerciseId"
    }
}
