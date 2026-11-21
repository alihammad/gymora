package com.gymora.data.repository

import androidx.room.withTransaction
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.entity.BodyMeasurementEntity
import com.gymora.domain.model.BodyMeasurement
import com.gymora.domain.model.BodyMetric
import com.gymora.domain.repository.BodyMeasurementRepository
import com.gymora.domain.repository.BodySaveResult
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class BodyMeasurementRepositoryImpl @Inject constructor(
    private val database: GymoraDatabase,
) : BodyMeasurementRepository {

    /** Decides "today" and the day boundaries; replaced in tests. */
    internal var clock: Clock = Clock.systemDefaultZone()

    private val dao get() = database.bodyMeasurementDao()

    override fun observeAll(): Flow<List<BodyMeasurement>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun saveForDate(date: LocalDate, values: Map<BodyMetric, Double>): BodySaveResult {
        val zone = clock.zone
        val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return database.withTransaction {
            val existing = dao.getOwnBetween(from, to)
            val keep = existing.firstOrNull()
            if (keep == null) {
                val now = Instant.now(clock)
                val measuredAt = if (date == LocalDate.now(clock)) now else date.atTime(LocalTime.NOON).atZone(zone).toInstant()
                val id = dao.insert(BodyMeasurementEntity(measuredAt = measuredAt.toEpochMilli()).withValues(values))
                BodySaveResult(id, emptyList())
            } else {
                dao.update(keep.withValues(values))
                // Older versions could store several entries on one date; the form showed
                // them merged and the user just saved the merged result, so fold them in.
                val removed = existing.drop(1).map { it.id }
                removed.forEach { dao.deleteById(it) }
                BodySaveResult(keep.id, removed)
            }
        }
    }

    override suspend fun delete(id: Long) = dao.deleteById(id)
}

/** The stored column of each metric. */
internal fun BodyMeasurementEntity.valueOf(metric: BodyMetric): Double? = when (metric) {
    BodyMetric.WEIGHT -> weightKg
    BodyMetric.CALORIES -> caloriesKcal
    BodyMetric.NECK -> neckCm
    BodyMetric.SHOULDERS -> shouldersCm
    BodyMetric.CHEST -> chestCm
    BodyMetric.LEFT_BICEP -> leftBicepCm
    BodyMetric.RIGHT_BICEP -> rightBicepCm
    BodyMetric.LEFT_ARM -> leftArmCm
    BodyMetric.RIGHT_ARM -> rightArmCm
    BodyMetric.WAIST_ABOVE_NAVEL -> aboveNavelCm
    BodyMetric.WAIST -> navelCm
    BodyMetric.WAIST_BELOW_NAVEL -> belowNavelCm
    BodyMetric.HIPS -> hipsCm
    BodyMetric.LEFT_THIGH -> leftThighCm
    BodyMetric.RIGHT_THIGH -> rightThighCm
    BodyMetric.LEFT_CALF -> leftCalfCm
    BodyMetric.RIGHT_CALF -> rightCalfCm
}

/** Sets every metric column to [values] (absent = null); other columns are kept. */
internal fun BodyMeasurementEntity.withValues(values: Map<BodyMetric, Double>) = copy(
    weightKg = values[BodyMetric.WEIGHT],
    caloriesKcal = values[BodyMetric.CALORIES],
    neckCm = values[BodyMetric.NECK],
    shouldersCm = values[BodyMetric.SHOULDERS],
    chestCm = values[BodyMetric.CHEST],
    leftBicepCm = values[BodyMetric.LEFT_BICEP],
    rightBicepCm = values[BodyMetric.RIGHT_BICEP],
    leftArmCm = values[BodyMetric.LEFT_ARM],
    rightArmCm = values[BodyMetric.RIGHT_ARM],
    aboveNavelCm = values[BodyMetric.WAIST_ABOVE_NAVEL],
    navelCm = values[BodyMetric.WAIST],
    belowNavelCm = values[BodyMetric.WAIST_BELOW_NAVEL],
    hipsCm = values[BodyMetric.HIPS],
    leftThighCm = values[BodyMetric.LEFT_THIGH],
    rightThighCm = values[BodyMetric.RIGHT_THIGH],
    leftCalfCm = values[BodyMetric.LEFT_CALF],
    rightCalfCm = values[BodyMetric.RIGHT_CALF],
)

internal fun BodyMeasurementEntity.toDomain() = BodyMeasurement(
    id = id,
    measuredAt = Instant.ofEpochMilli(measuredAt),
    values = BodyMetric.entries.mapNotNull { m -> valueOf(m)?.let { m to it } }.toMap(),
    imported = externalId != null,
)
