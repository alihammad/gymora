package com.gymora.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.model.PersonalRecords
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.repository.RecordsRepository
import com.gymora.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecordsUiState(
    val records: PersonalRecords? = null,
    val isLoading: Boolean = true,
    val displayUnit: WeightUnit = WeightUnit.KG,
)

@HiltViewModel
class RecordsViewModel @Inject constructor(
    private val recordsRepository: RecordsRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordsUiState())
    val uiState: StateFlow<RecordsUiState> = _uiState.asStateFlow()

    init {
        // Records are computed in a canonical unit (KG); the screen converts to the
        // user's selected display unit (FR-049, R-04).
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.update { it.copy(displayUnit = settings.weightUnit) }
            }
        }
        viewModelScope.launch {
            runCatching { recordsRepository.getPersonalRecords() }
                .onSuccess { records ->
                    _uiState.update { it.copy(records = records, isLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }
}