package com.gymora.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.model.Settings
import com.gymora.domain.model.Theme
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: Settings = Settings.DEFAULTS,
    val showCustomRestDialog: Boolean = false,
    val customRestSeconds: String = "",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun onWeightUnitSelected(unit: WeightUnit) {
        viewModelScope.launch { settingsRepository.setWeightUnit(unit) }
    }

    fun onRestDurationSelected(seconds: Int) {
        viewModelScope.launch { settingsRepository.setDefaultRestDuration(seconds) }
    }

    fun onThemeSelected(theme: Theme) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun onCustomRestClicked() {
        _uiState.update { it.copy(showCustomRestDialog = true, customRestSeconds = "") }
    }

    fun onCustomRestDismissed() {
        _uiState.update { it.copy(showCustomRestDialog = false) }
    }

    fun onCustomRestConfirmed() {
        val seconds = _uiState.value.customRestSeconds.toIntOrNull()
        if (seconds != null && seconds > 0) {
            viewModelScope.launch { settingsRepository.setDefaultRestDuration(seconds) }
        }
        _uiState.update { it.copy(showCustomRestDialog = false) }
    }

    fun onCustomRestSecondsChanged(value: String) {
        _uiState.update { it.copy(customRestSeconds = value) }
    }
}
