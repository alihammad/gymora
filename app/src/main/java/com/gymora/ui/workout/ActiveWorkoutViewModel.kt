package com.gymora.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.calculator.WorkoutCalculators
import com.gymora.domain.model.ActiveWorkoutConflictException
import com.gymora.domain.model.EntityNotFoundException
import com.gymora.domain.model.ValidationException
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.repository.ExerciseRepository
import com.gymora.domain.usecase.DiscardWorkoutUseCase
import com.gymora.domain.usecase.FinishWorkoutUseCase
import com.gymora.domain.usecase.LogSetUseCase
import com.gymora.domain.usecase.ModifySessionStructureUseCase
import com.gymora.domain.usecase.PreviousPerformanceUseCase
import com.gymora.domain.usecase.StartWorkoutUseCase
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
) : ViewModel() {

    private var sessionId: Long = savedStateHandle.get<String>(Destinations.ActiveWorkout.ARG)
        ?.toLongOrNull() ?: NEW_SESSION_ID

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        if (sessionId == NEW_SESSION_ID) {
            // Started via START on a routine card: routineId is passed via a
            // separate saved-state key set by the navigation caller.
            val routineId = savedStateHandle.get<String>(ARG_ROUTINE_ID)?.toLongOrNull()
            if (routineId != null) {
                startWorkout(routineId)
            }
        } else {
            loadSession()
        }
    }

    private fun startWorkout(routineId: Long) {
        viewModelScope.launch {
            runCatching { startWorkoutUseCase(routineId) }
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
            workout.exercises.forEach { exercise ->
                exercise.exerciseId?.let { id ->
                    previousPerformanceUseCase(id)?.let { perf ->
                        map[id] = perf
                    }
                }
            }
            _uiState.update { it.copy(previousPerformanceMap = map) }
        }
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

    fun onWeightChanged(setId: Long, text: String) {
        val set = findSet(setId) ?: return
        val weight = text.toDoubleOrNull()
        viewModelScope.launch {
            runCatching {
                logSetUseCase.updateValues(setId, weight, set.weightUnit ?: WeightUnit.KG, set.reps)
            }.onSuccess { loadSession() }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = friendlyMessage(error)) }
                }
        }
    }

    fun onRepsChanged(setId: Long, text: String) {
        val set = findSet(setId) ?: return
        val reps = text.toIntOrNull()
        viewModelScope.launch {
            runCatching {
                logSetUseCase.updateValues(setId, set.weight, set.weightUnit, reps)
            }.onSuccess { loadSession() }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = friendlyMessage(error)) }
                }
        }
    }

    fun onToggleComplete(setId: Long, completed: Boolean) {
        viewModelScope.launch {
            if (completed) logSetUseCase.complete(setId) else logSetUseCase.uncomplete(setId)
            loadSession()
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

    fun onFinishRequested() {
        _uiState.update { it.copy(showFinishConfirm = true) }
    }

    fun onFinishConfirmed() {
        viewModelScope.launch {
            runCatching { finishWorkoutUseCase(sessionId) }
                .onSuccess {
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
    }
}
