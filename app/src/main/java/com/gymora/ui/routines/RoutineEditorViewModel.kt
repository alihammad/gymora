package com.gymora.ui.routines

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.model.EntityNotFoundException
import com.gymora.domain.model.MeasurementType
import com.gymora.domain.model.SetTemplateInput
import com.gymora.domain.model.ValidationException
import com.gymora.domain.repository.ExerciseRepository
import com.gymora.domain.repository.HistoryRepository
import com.gymora.domain.repository.RoutineRepository
import com.gymora.domain.usecase.DeleteRoutineUseCase
import com.gymora.domain.usecase.DuplicateRoutineUseCase
import com.gymora.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RoutineEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
    private val historyRepository: HistoryRepository,
    private val deleteRoutineUseCase: DeleteRoutineUseCase,
    private val duplicateRoutineUseCase: DuplicateRoutineUseCase,
) : ViewModel() {

    /** Mutable: a "new routine" navigation creates the row first, then edits it. */
    private var routineId: Long = savedStateHandle.get<String>(Destinations.RoutineEditor.ARG)
        ?.toLongOrNull() ?: NEW_ROUTINE_ID

    private val _uiState = MutableStateFlow(RoutineEditorUiState())
    val uiState: StateFlow<RoutineEditorUiState> = _uiState.asStateFlow()

    init {
        if (routineId == NEW_ROUTINE_ID) {
            createBlankRoutine()
        } else {
            loadRoutine()
        }
    }

    private fun createBlankRoutine() {
        viewModelScope.launch {
            runCatching { routineRepository.create("New Workout", null) }
                .onSuccess { id ->
                    routineId = id
                    loadRoutine()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = friendlyMessage(error))
                    }
                }
        }
    }

    private fun loadRoutine() {
        viewModelScope.launch {
            runCatching { routineRepository.getById(routineId) }
                .onSuccess { detail ->
                    _uiState.update { it.copy(routine = detail, isLoading = false) }
                    loadProgress()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = friendlyMessage(error))
                    }
                }
        }
    }

    private fun loadProgress() {
        viewModelScope.launch {
            runCatching { historyRepository.getRoutineProgress(routineId, PROGRESS_SESSIONS) }
                .onSuccess { points -> _uiState.update { it.copy(progress = points) } }
        }
    }

    fun onRename(name: String) {
        viewModelScope.launch {
            runCatching { routineRepository.rename(routineId, name) }
                .onSuccess { loadRoutine() }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = friendlyMessage(error)) }
                }
        }
    }

    fun onDescriptionChanged(description: String?) {
        viewModelScope.launch {
            routineRepository.updateDescription(routineId, description?.ifBlank { null })
            loadRoutine()
        }
    }

    fun onAddExerciseClicked() {
        viewModelScope.launch {
            val exercises = exerciseRepository.observeLibrary().first()
            _uiState.update { it.copy(showExercisePicker = true, libraryExercises = exercises) }
        }
    }

    fun onExercisePicked(exerciseId: Long) {
        viewModelScope.launch {
            routineRepository.addExercise(routineId, exerciseId, notes = null)
            _uiState.update { it.copy(showExercisePicker = false) }
            loadRoutine()
        }
    }

    fun onExercisePickerDismissed() {
        _uiState.update { it.copy(showExercisePicker = false) }
    }

    fun onRemoveExercise(routineExerciseId: Long) {
        viewModelScope.launch {
            routineRepository.removeExercise(routineId, routineExerciseId)
            loadRoutine()
        }
    }

    fun onMoveExercise(fromIndex: Int, toIndex: Int) {
        val routine = _uiState.value.routine ?: return
        val ids = routine.exercises.map { it.routineExerciseId }.toMutableList()
        val moved = ids.removeAt(fromIndex)
        ids.add(toIndex, moved)
        viewModelScope.launch {
            routineRepository.reorderExercises(routineId, ids)
            loadRoutine()
        }
    }

    fun onAddSetTemplate(routineExerciseId: Long) {
        viewModelScope.launch {
            routineRepository.addSetTemplate(
                routineExerciseId,
                SetTemplateInput(
                    targetReps = 10,
                    targetWeight = null,
                    weightUnit = null,
                    measurementType = MeasurementType.WEIGHT_AND_REPS,
                ),
            )
            loadRoutine()
        }
    }

    fun onUpdateSetTemplate(templateId: Long, input: SetTemplateInput) {
        viewModelScope.launch {
            runCatching { routineRepository.updateSetTemplate(templateId, input) }
                .onSuccess { loadRoutine() }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = friendlyMessage(error)) }
                }
        }
    }

    fun onDeleteSetTemplate(templateId: Long) {
        viewModelScope.launch {
            routineRepository.deleteSetTemplate(templateId)
            loadRoutine()
        }
    }

    fun onDuplicate() {
        viewModelScope.launch {
            runCatching { duplicateRoutineUseCase(routineId) }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = friendlyMessage(error)) }
                }
        }
    }

    fun onDeleteRequested() {
        _uiState.update { it.copy(showDeleteConfirm = true) }
    }

    fun onDeleteConfirmed() {
        viewModelScope.launch {
            runCatching { deleteRoutineUseCase(routineId) }
                .onSuccess {
                    _uiState.update { it.copy(showDeleteConfirm = false, isDeleted = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(showDeleteConfirm = false, errorMessage = friendlyMessage(error))
                    }
                }
        }
    }

    fun onDeleteDismissed() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun friendlyMessage(error: Throwable): String = when (error) {
        is ValidationException -> error.message ?: "Please check your input."
        is EntityNotFoundException -> "This workout no longer exists."
        else -> "Something went wrong. Please try again." // FR-060: no stack traces
    }

    companion object {
        const val NEW_ROUTINE_ID = 0L
        const val PROGRESS_SESSIONS = 20
    }
}
