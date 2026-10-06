package com.gymora.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.domain.model.HistoryEntry
import com.gymora.domain.model.WorkoutStat
import com.gymora.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How the History tab shows completed workouts. */
enum class HistoryView { LIST, CALENDAR }

/** UI state for the history list (FR-040, FR-058) and calendar. */
data class HistoryUiState(
    val view: HistoryView = HistoryView.LIST,
    val entries: List<HistoryEntry> = emptyList(),
    /** Every completed workout, oldest first; loaded when the calendar is opened. */
    val calendarWorkouts: List<WorkoutStat> = emptyList(),
    /** Day tapped in the calendar; its workouts are listed under it. */
    val selectedDay: LocalDate? = null,
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
            val entries = try {
                historyRepository.listCompleted(limit = PAGE_SIZE, offset = 0)
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
            _uiState.update {
                it.copy(
                    entries = entries,
                    isLoading = false,
                    hasMore = entries.size == PAGE_SIZE,
                )
            }
        }
    }

    fun onViewSelected(view: HistoryView) {
        _uiState.update { it.copy(view = view) }
        if (view == HistoryView.CALENDAR) loadCalendar()
    }

    fun onCalendarDaySelected(day: LocalDate) {
        _uiState.update { it.copy(selectedDay = day) }
    }

    /** Reloaded on every open so newly finished workouts appear. */
    private fun loadCalendar() {
        viewModelScope.launch {
            val all = runCatching { historyRepository.getWorkoutStats() }.getOrDefault(emptyList())
            val zone = ZoneId.systemDefault()
            _uiState.update { state ->
                // Default to the most recent day trained so the list below is never empty.
                val selected = state.selectedDay ?: all.lastOrNull()?.startedAt?.atZone(zone)?.toLocalDate()
                state.copy(calendarWorkouts = all, selectedDay = selected)
            }
        }
    }

    /** Incremental loading (FR-058, R-07): never the full lifetime history at once. */
    fun onLoadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            val more = historyRepository.listCompleted(
                limit = PAGE_SIZE,
                offset = state.entries.size,
            )
            _uiState.update {
                it.copy(
                    entries = (it.entries + more).distinctBy { e -> e.id },
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
