package com.gymora.data.repository

import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.entity.BodyMeasurementEntity
import com.gymora.domain.model.BodyMeasurement
import com.gymora.domain.repository.BodyMeasurementRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class BodyMeasurementRepositoryImpl @Inject constructor(
    private val database: GymoraDatabase,
) : BodyMeasurementRepository {

    private val dao get() = database.bodyMeasurementDao()

    override fun observeAll(): Flow<List<BodyMeasurement>> =
        dao.observeAll().map { rows ->
            rows.map {
                BodyMeasurement(
                    id = it.id,
                    measuredAt = Instant.ofEpochMilli(it.measuredAt),
                    weightKg = it.weightKg,
                    shouldersCm = it.shouldersCm,
                    chestCm = it.chestCm,
                    aboveNavelCm = it.aboveNavelCm,
                    navelCm = it.navelCm,
                    belowNavelCm = it.belowNavelCm,
                    thighCm = it.thighCm,
                )
            }
        }

    override suspend fun save(entry: BodyMeasurement) {
        dao.insert(
            BodyMeasurementEntity(
                measuredAt = System.currentTimeMillis(),
                weightKg = entry.weightKg,
                shouldersCm = entry.shouldersCm,
                chestCm = entry.chestCm,
                aboveNavelCm = entry.aboveNavelCm,
                navelCm = entry.navelCm,
                belowNavelCm = entry.belowNavelCm,
                thighCm = entry.thighCm,
            ),
        )
    }
}
