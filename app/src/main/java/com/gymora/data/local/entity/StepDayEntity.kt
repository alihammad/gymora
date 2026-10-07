package com.gymora.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Steps counted by the phone's step sensor for one local day. Device-local:
 * not part of backups, since the sensor counter only makes sense on this phone.
 */
@Entity(tableName = "step_days")
data class StepDayEntity(
    /** Local date, ISO-8601 (yyyy-MM-dd). */
    @PrimaryKey val date: String,
    @ColumnInfo(name = "steps") val steps: Int,
    /** Last raw sensor reading (steps since boot), used to compute the next delta. */
    @ColumnInfo(name = "last_counter") val lastCounter: Long,
)
