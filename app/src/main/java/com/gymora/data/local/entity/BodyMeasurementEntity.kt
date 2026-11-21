package com.gymora.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One body-measurement entry. Every value is optional so an entry can record
 * just a weigh-in or just one circumference. Stored in kg / cm / kcal; the UI
 * converts for display. Columns are named after the stored unit and synced and
 * backed up by name, so renaming one is a breaking change.
 */
@Entity(
    tableName = "body_measurements",
    indices = [
        Index(value = ["external_id"], unique = true),
        Index(value = ["sync_id"], unique = true),
    ],
)
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "measured_at") val measuredAt: Long,
    @ColumnInfo(name = "weight_kg") val weightKg: Double? = null,
    @ColumnInfo(name = "shoulders_cm") val shouldersCm: Double? = null,
    @ColumnInfo(name = "chest_cm") val chestCm: Double? = null,
    /** Waist 2 in / 5 cm above the navel. */
    @ColumnInfo(name = "above_navel_cm") val aboveNavelCm: Double? = null,
    /** Waist at the navel. */
    @ColumnInfo(name = "navel_cm") val navelCm: Double? = null,
    /** Waist 2 in / 5 cm below the navel. */
    @ColumnInfo(name = "below_navel_cm") val belowNavelCm: Double? = null,
    /**
     * Single thigh value from before left/right thighs (v2–v7). No longer written;
     * MIGRATION_7_8 copied it to both sides. Kept so older app versions syncing
     * the same account still find their column.
     */
    @ColumnInfo(name = "thigh_cm") val thighCm: Double? = null,
    /** Health Connect record id for weigh-ins imported from other apps (v5); null for own entries. */
    @ColumnInfo(name = "external_id") val externalId: String? = null,
    /** Cross-device id (v7); assigned by SyncTriggers on insert. */
    @ColumnInfo(name = "sync_id") val syncId: String? = null,
    /** Last change (v7); maintained by SyncTriggers. */
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = 0,
    // --- v8 ---
    @ColumnInfo(name = "calories_kcal") val caloriesKcal: Double? = null,
    @ColumnInfo(name = "neck_cm") val neckCm: Double? = null,
    @ColumnInfo(name = "left_bicep_cm") val leftBicepCm: Double? = null,
    @ColumnInfo(name = "right_bicep_cm") val rightBicepCm: Double? = null,
    @ColumnInfo(name = "left_arm_cm") val leftArmCm: Double? = null,
    @ColumnInfo(name = "right_arm_cm") val rightArmCm: Double? = null,
    @ColumnInfo(name = "hips_cm") val hipsCm: Double? = null,
    @ColumnInfo(name = "left_thigh_cm") val leftThighCm: Double? = null,
    @ColumnInfo(name = "right_thigh_cm") val rightThighCm: Double? = null,
    @ColumnInfo(name = "left_calf_cm") val leftCalfCm: Double? = null,
    @ColumnInfo(name = "right_calf_cm") val rightCalfCm: Double? = null,
)
