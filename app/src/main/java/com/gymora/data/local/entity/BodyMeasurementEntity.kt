package com.gymora.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One body-measurement entry. Every value is optional so an entry can record
 * just a weigh-in or just one circumference. Stored in kg / cm; the UI
 * converts for display.
 */
@Entity(tableName = "body_measurements")
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "measured_at") val measuredAt: Long,
    @ColumnInfo(name = "weight_kg") val weightKg: Double?,
    @ColumnInfo(name = "shoulders_cm") val shouldersCm: Double?,
    @ColumnInfo(name = "chest_cm") val chestCm: Double?,
    @ColumnInfo(name = "above_navel_cm") val aboveNavelCm: Double?,
    @ColumnInfo(name = "navel_cm") val navelCm: Double?,
    @ColumnInfo(name = "below_navel_cm") val belowNavelCm: Double?,
    @ColumnInfo(name = "thigh_cm") val thighCm: Double?,
)
