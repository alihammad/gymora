package com.gymora.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.gymora.data.local.entity.StepDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class StepDao {

    @Query("SELECT steps FROM step_days WHERE date = :date")
    abstract fun observeSteps(date: String): Flow<Int?>

    @Query("SELECT * FROM step_days WHERE date >= :from AND date <= :to ORDER BY date")
    abstract fun observeRange(from: String, to: String): Flow<List<StepDayEntity>>

    @Query("SELECT * FROM step_days WHERE date = :date")
    abstract suspend fun get(date: String): StepDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(day: StepDayEntity)

    /**
     * Folds a raw sensor reading ([counter], steps since boot) into [date]'s total.
     *
     * The sensor counter only grows until a reboot resets it, so each reading adds the
     * difference to the previous one. A reading lower than the previous one means a
     * reboot: everything it reports happened since. On a new day the previous day's
     * last reading carries over (steps taken overnight count for today); with no row
     * for yesterday there is no trustworthy baseline, so today starts at zero.
     */
    @Transaction
    open suspend fun recordCounter(date: String, previousDate: String, counter: Long) {
        val today = get(date)
        val baseline = today ?: get(previousDate)
        val delta = when {
            baseline == null -> 0L
            counter >= baseline.lastCounter -> counter - baseline.lastCounter
            else -> counter
        }
        upsert(
            StepDayEntity(
                date = date,
                steps = (today?.steps ?: 0) + delta.toInt(),
                lastCounter = counter,
            ),
        )
    }
}
