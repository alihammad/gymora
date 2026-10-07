package com.gymora.data.repository

import com.gymora.data.local.dao.SettingsDao
import com.gymora.data.local.entity.SettingsEntity
import com.gymora.domain.calculator.EngagementCalculators
import com.gymora.domain.model.Settings
import com.gymora.domain.model.Theme
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.repository.SettingsRepository
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
) : SettingsRepository {

    override fun observeSettings(): Flow<Settings> =
        settingsDao.observeSettings().map { entity -> entity?.toDomain() ?: Settings.DEFAULTS }

    override suspend fun setWeightUnit(unit: WeightUnit) {
        val current = current()
        settingsDao.upsert(current.copy(weightUnit = unit.name, updatedAt = now()))
    }

    override suspend fun setDefaultRestDuration(seconds: Int) {
        val current = current()
        settingsDao.upsert(current.copy(defaultRestSeconds = seconds, updatedAt = now()))
    }

    override suspend fun setTheme(theme: Theme) {
        val current = current()
        settingsDao.upsert(current.copy(theme = theme.name, updatedAt = now()))
    }

    override suspend fun setWeeklyGoal(goal: Int) {
        val current = current()
        val clamped = goal.coerceIn(Settings.MIN_WEEKLY_GOAL, Settings.MAX_WEEKLY_GOAL)
        settingsDao.upsert(current.copy(weeklyGoal = clamped, updatedAt = now()))
    }

    override suspend fun setStepGoal(goal: Int) {
        val current = current()
        val clamped = goal.coerceIn(Settings.MIN_STEP_GOAL, Settings.MAX_STEP_GOAL)
        settingsDao.upsert(current.copy(stepGoal = clamped, updatedAt = now()))
    }

    override suspend fun setHealthStepsEnabled(enabled: Boolean) {
        val current = current()
        settingsDao.upsert(current.copy(healthStepsEnabled = enabled, updatedAt = now()))
    }

    override suspend fun setReminder(days: Set<DayOfWeek>, time: LocalTime) {
        val current = current()
        settingsDao.upsert(
            current.copy(
                reminderDays = EngagementCalculators.daysToMask(days),
                reminderMinuteOfDay = time.hour * MINUTES_PER_HOUR + time.minute,
                updatedAt = now(),
            ),
        )
    }

    override suspend fun setAutoBackup(intervalDays: Int, folderUri: String?) {
        val current = current()
        settingsDao.upsert(
            current.copy(
                autoBackupIntervalDays = intervalDays.coerceAtLeast(0),
                autoBackupFolderUri = folderUri,
                updatedAt = now(),
            ),
        )
    }

    override suspend fun setLastAutoBackupAt(epochMillis: Long) {
        val current = current()
        settingsDao.upsert(current.copy(lastAutoBackupAt = epochMillis))
    }

    override suspend fun setHealthConnectEnabled(enabled: Boolean) {
        val current = current()
        settingsDao.upsert(current.copy(healthConnectEnabled = enabled, updatedAt = now()))
    }

    private suspend fun current(): SettingsEntity =
        settingsDao.getOnce() ?: SettingsEntity.DEFAULTS

    private fun now(): Long = System.currentTimeMillis()

    private fun SettingsEntity.toDomain(): Settings = Settings(
        weightUnit = WeightUnit.valueOf(weightUnit),
        defaultRestSeconds = defaultRestSeconds,
        theme = Theme.valueOf(theme),
        weeklyGoal = weeklyGoal,
        reminderDays = EngagementCalculators.maskToDays(reminderDays),
        reminderTime = LocalTime.of(reminderMinuteOfDay / MINUTES_PER_HOUR, reminderMinuteOfDay % MINUTES_PER_HOUR),
        autoBackupIntervalDays = autoBackupIntervalDays,
        autoBackupFolderUri = autoBackupFolderUri,
        lastAutoBackupAt = lastAutoBackupAt,
        healthConnectEnabled = healthConnectEnabled,
        stepGoal = stepGoal,
        healthStepsEnabled = healthStepsEnabled,
    )

    private companion object {
        const val MINUTES_PER_HOUR = 60
    }
}
