package com.gymora.ui.home

import com.gymora.domain.model.HistoryEntry
import com.gymora.domain.model.RoutineSummary

/** UI state for the home screen (FR-001, FR-002, FR-015). */
data class HomeUiState(
    val routines: List<RoutineSummary> = emptyList(),
    val recentWorkouts: List<HistoryEntry> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
