package com.gymora.domain.usecase

import com.gymora.domain.calculator.EngagementCalculators
import com.gymora.domain.calculator.WeeklyProgress
import com.gymora.domain.model.RoutineSummary
import com.gymora.domain.repository.HistoryRepository
import com.gymora.domain.repository.RoutineRepository
import com.gymora.domain.repository.SettingsRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Everything the habit surfaces (Home card, widget, reminders) show. */
data class Engagement(
    val progress: WeeklyProgress,
    /** Suggested routine to do next; null when there are no routines. */
    val nextRoutine: RoutineSummary?,
    val trainedToday: Boolean,
)

/** One snapshot of weekly goal progress, streak and the suggested next routine. */
class GetEngagementUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val routineRepository: RoutineRepository,
    private val settingsRepository: SettingsRepository,
) {

    suspend operator fun invoke(today: LocalDate = LocalDate.now()): Engagement {
        val dates = historyRepository.completedWorkoutDates()
        val goal = settingsRepository.observeSettings().first().weeklyGoal
        return Engagement(
            progress = EngagementCalculators.weeklyProgress(dates, goal, today),
            nextRoutine = EngagementCalculators.nextRoutine(routineRepository.observeAll().first()),
            trainedToday = today in dates,
        )
    }
}
