package com.gymora.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.model.Exercise
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.model.WorkoutDetail
import com.gymora.domain.repository.ExerciseRepository
import com.gymora.domain.repository.HistoryRepository
import com.gymora.domain.usecase.CorrectHistoricalWorkoutUseCase
import com.gymora.ui.components.SetEntry
import com.gymora.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A set's editable fields during historical correction (FR-042). */
data class EditableSet(
    val id: Long,
    val values: SetEntry,
    val weightUnit: WeightUnit?,
    val isCompleted: Boolean,
    val notes: String,
)

/** UI state for the workout detail (read-only by default; edit via explicit opt-in). */
data class WorkoutDetailUiState(
    val detail: WorkoutDetail? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isEditing: Boolean = false,
    val editableSets: Map<Long, EditableSet> = emptyMap(),
    val editableNotes: String = "",
    val editableStart: java.time.Instant? = null,
    /** Duration in whole minutes, as typed. */
    val editableDurationMinutes: String = "",
    val showAddExercise: Boolean = false,
    val libraryExercises: List<Exercise> = emptyList(),
    val pendingRemoveExerciseId: Long? = null,
    /** Unit for weights entered on sets that had none. */
    val weightUnit: WeightUnit = WeightUnit.KG,
)

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val historyRepository: HistoryRepository,
    private val correctWorkoutUseCase: CorrectHistoricalWorkoutUseCase,
    private val exerciseRepository: ExerciseRepository,
    settingsRepository: com.gymora.domain.repository.SettingsRepository,
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<String>(Destinations.WorkoutDetail.ARG)
        ?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(WorkoutDetailUiState())
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
        viewModelScope.launch {
            val unit = settingsRepository.observeSettings().first().weightUnit
            _uiState.update { it.copy(weightUnit = unit) }
        }
    }

    private fun loadDetail() {
        viewModelScope.launch {
            runCatching { historyRepository.getWorkoutDetail(sessionId) }
                .onSuccess { detail ->
                    _uiState.update { it.copy(detail = detail, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = friendlyMessage(error))
                    }
                }
        }
    }

    /** FR-060: no stack traces surface; map domain failures to friendly copy. */
    private fun friendlyMessage(error: Throwable): String = when (error) {
        is com.gymora.domain.model.EntityNotFoundException -> "This workout no longer exists."
        else -> "Something went wrong. Please try again."
    }

    /** FR-042: history is read-only unless the user explicitly opts into editing. */
    fun enterEditMode() {
        val detail = _uiState.value.detail ?: return
        val editable = detail.exercises.flatMap { ex -> ex.sets }.associate { set ->
            set.id to EditableSet(
                id = set.id,
                values = SetEntry(set.weight, set.reps, set.durationSeconds, set.distanceMeters),
                weightUnit = set.weightUnit,
                isCompleted = set.isCompleted,
                notes = set.notes.orEmpty(),
            )
        }
        _uiState.update {
            it.copy(
                isEditing = true,
                editableSets = editable,
                editableNotes = detail.session.notes.orEmpty(),
                editableStart = detail.session.startedAt,
                editableDurationMinutes = detail.session.endedAt?.let { ended ->
                    (java.time.Duration.between(detail.session.startedAt, ended).seconds / 60)
                        .toString()
                }.orEmpty(),
            )
        }
    }

    fun onStartChanged(value: java.time.Instant) {
        _uiState.update { it.copy(editableStart = value) }
    }

    fun onDurationMinutesChanged(value: String) {
        _uiState.update { it.copy(editableDurationMinutes = value.filter(Char::isDigit).take(4)) }
    }

    fun cancelEdit() {
        _uiState.update {
            it.copy(
                isEditing = false,
                editableSets = emptyMap(),
                editableNotes = "",
                showAddExercise = false,
            )
        }
    }

    fun onSetValuesChanged(setId: Long, values: SetEntry) {
        updateEditableSet(setId) { it.copy(values = values) }
    }

    fun onSetNotesChanged(setId: Long, value: String) {
        updateEditableSet(setId) { it.copy(notes = value) }
    }

    fun onToggleComplete(setId: Long) {
        updateEditableSet(setId) { it.copy(isCompleted = !it.isCompleted) }
    }

    fun onWorkoutNotesChanged(value: String) {
        _uiState.update { it.copy(editableNotes = value) }
    }

    private fun updateEditableSet(setId: Long, transform: (EditableSet) -> EditableSet) {
        val current = _uiState.value.editableSets[setId] ?: return
        _uiState.update { state ->
            state.copy(editableSets = state.editableSets + (setId to transform(current)))
        }
    }

    /** Persist all corrections via the correction APIs, then reload (FR-042). */
    fun save() {
        val state = _uiState.value
        val detail = state.detail ?: return
        viewModelScope.launch {
            runCatching {
                state.editableSets.forEach { (setId, editable) ->
                    val original = detail.exercises.flatMap { it.sets }
                        .firstOrNull { it.id == setId }
                    correctWorkoutUseCase.correctSet(
                        setId = setId,
                        weight = editable.values.weight,
                        weightUnit = editable.weightUnit ?: original?.weightUnit ?: state.weightUnit,
                        reps = editable.values.reps,
                        isCompleted = editable.isCompleted,
                        notes = editable.notes.ifBlank { null },
                        durationSeconds = editable.values.durationSeconds,
                        distanceMeters = editable.values.distanceMeters,
                    )
                }
                correctWorkoutUseCase.updateNotes(
                    sessionId,
                    state.editableNotes.ifBlank { null },
                )
                val start = state.editableStart
                val minutes = state.editableDurationMinutes.toLongOrNull()
                if (start != null && minutes != null && detail.session.endedAt != null) {
                    correctWorkoutUseCase.updateTimes(
                        sessionId,
                        start,
                        start.plusSeconds(minutes * 60),
                    )
                }
            }.onSuccess {
                _uiState.update { it.copy(isEditing = false, editableSets = emptyMap()) }
                loadDetail()
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = friendlyMessage(error)) }
            }
        }
    }

    fun onAddExerciseClicked() {
        viewModelScope.launch {
            val library = exerciseRepository.observeLibrary().first()
            _uiState.update { it.copy(showAddExercise = true, libraryExercises = library) }
        }
    }

    fun onAddExerciseDismissed() {
        _uiState.update { it.copy(showAddExercise = false) }
    }

    fun onExerciseSelected(exerciseId: Long) {
        viewModelScope.launch {
            runCatching { correctWorkoutUseCase.addExercise(sessionId, exerciseId) }
                .onSuccess {
                    _uiState.update { it.copy(showAddExercise = false) }
                    loadDetail()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(showAddExercise = false, errorMessage = friendlyMessage(error))
                    }
                }
        }
    }

    fun onRemoveExerciseClicked(workoutExerciseId: Long) {
        _uiState.update { it.copy(pendingRemoveExerciseId = workoutExerciseId) }
    }

    fun onRemoveExerciseDismissed() {
        _uiState.update { it.copy(pendingRemoveExerciseId = null) }
    }

    fun onRemoveExerciseConfirmed() {
        val id = _uiState.value.pendingRemoveExerciseId ?: return
        viewModelScope.launch {
            runCatching { correctWorkoutUseCase.removeExercise(id) }
                .onSuccess {
                    _uiState.update { it.copy(pendingRemoveExerciseId = null) }
                    loadDetail()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(pendingRemoveExerciseId = null, errorMessage = friendlyMessage(error))
                    }
                }
        }
    }
}
