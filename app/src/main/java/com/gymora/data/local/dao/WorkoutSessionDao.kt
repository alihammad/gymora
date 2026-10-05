package com.gymora.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gymora.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {

    /** The single ACTIVE session, if any (BR-14). */
    @Query("SELECT * FROM workout_sessions WHERE status = 'ACTIVE' LIMIT 1")
    fun observeActive(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveOnce(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getById(id: Long): WorkoutSessionEntity?

    @Insert
    suspend fun insert(entity: WorkoutSessionEntity): Long

    @Update
    suspend fun update(entity: WorkoutSessionEntity)

    /** Finish: record end timestamp + COMPLETED status (FR-034). */
    @Query(
        "UPDATE workout_sessions SET ended_at = :endedAt, status = 'COMPLETED' WHERE id = :id",
    )
    suspend fun finish(id: Long, endedAt: Long)

    /** Permanent deletion (BR-15). */
    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Completed sessions, newest first, paged (FR-040, FR-058, R-07). */
    @Query(
        "SELECT * FROM workout_sessions WHERE status = 'COMPLETED' " +
            "ORDER BY started_at DESC LIMIT :limit OFFSET :offset",
    )
    suspend fun listCompleted(limit: Int, offset: Int): List<WorkoutSessionEntity>

    /** Completed sessions started at or after [since], oldest first. */
    @Query(
        "SELECT * FROM workout_sessions WHERE status = 'COMPLETED' AND started_at >= :since " +
            "ORDER BY started_at ASC",
    )
    suspend fun listCompletedSince(since: Long): List<WorkoutSessionEntity>

    /** Most recent completed sessions started from a routine (progress chart). */
    @Query(
        "SELECT * FROM workout_sessions WHERE status = 'COMPLETED' AND routine_id = :routineId " +
            "ORDER BY started_at DESC LIMIT :limit",
    )
    suspend fun listCompletedForRoutine(routineId: Long, limit: Int): List<WorkoutSessionEntity>

    /** Start timestamps of completed sessions in [fromMillis, toMillis) — calendar markers. */
    @Query(
        "SELECT started_at FROM workout_sessions WHERE status = 'COMPLETED' " +
            "AND started_at >= :fromMillis AND started_at < :toMillis",
    )
    suspend fun completedStartTimesBetween(fromMillis: Long, toMillis: Long): List<Long>

    /** Completed sessions started in [fromMillis, toMillis), oldest first. */
    @Query(
        "SELECT * FROM workout_sessions WHERE status = 'COMPLETED' " +
            "AND started_at >= :fromMillis AND started_at < :toMillis ORDER BY started_at ASC",
    )
    suspend fun listCompletedBetween(fromMillis: Long, toMillis: Long): List<WorkoutSessionEntity>

    /** CSV-import duplicate check: same workout name starting in the same second. */
    @Query(
        "SELECT COUNT(*) FROM workout_sessions WHERE status = 'COMPLETED' " +
            "AND started_at / 1000 = :epochSecond AND routine_name_snapshot = :name COLLATE NOCASE",
    )
    suspend fun countCompletedAt(epochSecond: Long, name: String): Int

    /** Most recent completed session that contains the given exercise (FR-045). */
    @Query(
        """
        SELECT s.* FROM workout_sessions s
        INNER JOIN workout_exercises we ON we.session_id = s.id
        WHERE s.status = 'COMPLETED' AND we.exercise_id = :exerciseId
        ORDER BY s.started_at DESC LIMIT 1
        """,
    )
    suspend fun latestCompletedSessionForExercise(exerciseId: Long): WorkoutSessionEntity?
}
