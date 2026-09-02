package com.gymora.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.model.ActiveWorkout
import com.gymora.domain.usecase.DiscardWorkoutUseCase
import com.gymora.domain.usecase.ResumeWorkoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Recovery state on launch (FR-038, FR-039, T045): detects an unfinished
 * workout and exposes resume/discard actions.
 */
@HiltViewModel
class RecoveryViewModel @Inject constructor(
    private val resumeWorkoutUseCase: ResumeWorkoutUseCase,
    private val discardWorkoutUseCase: DiscardWorkoutUseCase,
) : ViewModel() {

    private val _pendingWorkout = MutableStateFlow<ActiveWorkout?>(null)
    val pendingWorkout: StateFlow<ActiveWorkout?> = _pendingWorkout.asStateFlow()

    private val _checked = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            if (!_checked.value) {
                _pendingWorkout.value = resumeWorkoutUseCase.detect()
                _checked.value = true
            }
        }
    }

    fun onDismissed() {
        _pendingWorkout.update { null }
    }

    fun onDiscarded() {
        val workout = _pendingWorkout.value ?: return
        viewModelScope.launch {
            discardWorkoutUseCase(workout.session.id)
            _pendingWorkout.update { null }
        }
    }
}
