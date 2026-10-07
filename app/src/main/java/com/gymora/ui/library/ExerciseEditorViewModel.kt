package com.gymora.ui.library

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.model.CreateExerciseInput
import com.gymora.data.media.ExerciseMediaStore
import com.gymora.domain.model.EntityNotFoundException
import com.gymora.domain.model.MeasurementType
import com.gymora.domain.model.MuscleGroup
import com.gymora.domain.model.UpdateExerciseInput
import com.gymora.domain.model.ValidationException
import com.gymora.domain.repository.ExerciseRepository
import com.gymora.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for the exercise editor (create/edit, FR-006/FR-007). */
data class ExerciseEditorUiState(
    val exerciseId: Long? = null,
    val name: String = "",
    val muscleGroup: MuscleGroup? = null,
    val description: String = "",
    val notes: String = "",
    val measurementType: MeasurementType = MeasurementType.WEIGHT_AND_REPS,
    val isUnilateral: Boolean = false,
    /** One cue per line while editing. */
    val formCues: String = "",
    val mediaFile: String? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ExerciseEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    private val mediaStore: ExerciseMediaStore,
) : ViewModel() {

    /** Media as last saved; replaced files are deleted only once the edit is saved. */
    private var savedMediaFile: String? = null

    private val exerciseId: Long? = savedStateHandle.get<String>(Destinations.ExerciseEditor.ARG)
        ?.toLongOrNull()
        ?.takeIf { it > 0 } // 0 is the sentinel for "new exercise"

    private val _uiState = MutableStateFlow(ExerciseEditorUiState())
    val uiState: StateFlow<ExerciseEditorUiState> = _uiState.asStateFlow()

    init {
        if (exerciseId != null) {
            loadExercise(exerciseId)
        }
    }

    private fun loadExercise(id: Long) {
        _uiState.update { it.copy(isLoading = true, exerciseId = id) }
        viewModelScope.launch {
            runCatching { exerciseRepository.getById(id) }
                .onSuccess { exercise ->
                    savedMediaFile = exercise.mediaFile
                    _uiState.update {
                        it.copy(
                            name = exercise.name,
                            muscleGroup = exercise.muscleGroup,
                            description = exercise.description.orEmpty(),
                            notes = exercise.notes.orEmpty(),
                            measurementType = exercise.measurementType,
                            isUnilateral = exercise.isUnilateral,
                            formCues = exercise.formCues.joinToString("\n"),
                            mediaFile = exercise.mediaFile,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = friendlyMessage(error),
                        )
                    }
                }
        }
    }

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    fun onMuscleGroupChanged(value: MuscleGroup?) {
        _uiState.update { it.copy(muscleGroup = value) }
    }

    fun onDescriptionChanged(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onNotesChanged(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun onMeasurementTypeChanged(value: MeasurementType) {
        _uiState.update { it.copy(measurementType = value) }
    }

    fun onUnilateralChanged(value: Boolean) {
        _uiState.update { it.copy(isUnilateral = value) }
    }

    fun onFormCuesChanged(value: String) {
        _uiState.update { it.copy(formCues = value) }
    }

    fun onMediaPicked(uri: Uri) {
        viewModelScope.launch {
            runCatching { mediaStore.import(uri) }
                .onSuccess { name ->
                    discardUnsavedMedia()
                    _uiState.update { it.copy(mediaFile = name) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "Could not add that image.") }
                }
        }
    }

    fun onMediaRemoved() {
        viewModelScope.launch {
            discardUnsavedMedia()
            _uiState.update { it.copy(mediaFile = null) }
        }
    }

    /** Deletes a picked-but-unsaved image; the saved one stays until the edit is saved. */
    private suspend fun discardUnsavedMedia() {
        val current = _uiState.value.mediaFile
        if (current != null && current != savedMediaFile) mediaStore.delete(current)
    }

    fun onSave() {
        val state = _uiState.value
        val cues = state.formCues.lines().map(String::trim).filter(String::isNotEmpty)
        viewModelScope.launch {
            runCatching {
                if (state.exerciseId == null) {
                    exerciseRepository.createCustom(
                        CreateExerciseInput(
                            name = state.name,
                            muscleGroup = state.muscleGroup,
                            description = state.description.ifBlank { null },
                            notes = state.notes.ifBlank { null },
                            measurementType = state.measurementType,
                            isUnilateral = state.isUnilateral,
                            formCues = cues,
                            mediaFile = state.mediaFile,
                        ),
                    )
                } else {
                    exerciseRepository.update(
                        state.exerciseId,
                        UpdateExerciseInput(
                            name = state.name,
                            muscleGroup = state.muscleGroup,
                            description = state.description.ifBlank { null },
                            notes = state.notes.ifBlank { null },
                            measurementType = state.measurementType,
                            isUnilateral = state.isUnilateral,
                            formCues = cues,
                            mediaFile = state.mediaFile,
                        ),
                    )
                }
            }.onSuccess {
                savedMediaFile?.takeIf { it != state.mediaFile }?.let { mediaStore.delete(it) }
                savedMediaFile = state.mediaFile
                _uiState.update { it.copy(isSaved = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = friendlyMessage(error)) }
            }
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun friendlyMessage(error: Throwable): String = when (error) {
        is ValidationException -> error.message ?: "Please check your input."
        is EntityNotFoundException -> "This exercise no longer exists."
        else -> "Something went wrong. Please try again." // FR-060: no stack traces
    }
}
