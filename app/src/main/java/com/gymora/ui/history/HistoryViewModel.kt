package com.gymora.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.model.HistoryEntry
import com.gymora.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for the history list (FR-040, FR-058). */
data class HistoryUiState(
    val entries: List<HistoryEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadInitial()
    }

    private fun loadInitial() {
        viewModelScope.launch {
            val entries = historyRepository.listCompleted(limit = PAGE_SIZE, offset = 0)
            _uiState.update {
                it.copy(
                    entries = entries,
                    isLoading = false,
                    hasMore = entries.size == PAGE_SIZE,
                )
            }
        }
    }

    /** Incremental loading (FR-058, R-07): never the full lifetime history at once. */
    fun onLoadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            val more = historyRepository.listCompleted(
                limit = PAGE_SIZE,
                offset = state.entries.size,
            )
            _uiState.update {
                it.copy(
                    entries = it.entries + more,
                    isLoadingMore = false,
                    hasMore = more.size == PAGE_SIZE,
                )
            }
        }
    }

    companion object {
        const val PAGE_SIZE = 30
    }
}
