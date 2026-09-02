package com.gymora.data.repository

import com.gymora.data.local.dao.SettingsDao
import com.gymora.data.local.entity.SettingsEntity
import com.gymora.domain.model.Settings
import com.gymora.domain.model.Theme
import com.gymora.domain.model.WeightUnit
import com.gymora.domain.repository.SettingsRepository
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

    private suspend fun current(): SettingsEntity =
        settingsDao.getOnce() ?: SettingsEntity.DEFAULTS

    private fun now(): Long = System.currentTimeMillis()

    private fun SettingsEntity.toDomain(): Settings = Settings(
        weightUnit = WeightUnit.valueOf(weightUnit),
        defaultRestSeconds = defaultRestSeconds,
        theme = Theme.valueOf(theme),
    )
}
