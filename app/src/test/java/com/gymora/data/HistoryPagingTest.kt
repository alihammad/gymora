package com.gymora.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.repository.HistoryRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * T072 [SC-006]: large-history responsiveness. A fixture generates 1,000+
 * completed workouts and verifies LIMIT/OFFSET paging returns correct,
 * newest-first pages without ever materializing the full history at once.
 */
@RunWith(RobolectricTestRunner::class)
class HistoryPagingTest {

    private lateinit var database: GymoraDatabase
    private lateinit var historyRepository: HistoryRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymoraDatabase::class.java,
        ).allowMainThreadQueries().build()
        historyRepository = HistoryRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** Insert [count] COMPLETED sessions directly for speed (SC-006 fixture). */
    private fun seedCompletedSessions(count: Int) {
        val db = database.openHelper.writableDatabase
        db.beginTransaction()
        try {
            for (i in 0 until count) {
                // started_at increases with i so the newest is the last inserted.
                db.execSQL(
                    "INSERT INTO workout_sessions " +
                        "(routine_id, routine_name_snapshot, started_at, ended_at, status, notes, created_at) " +
                        "VALUES (NULL, ?, ?, ?, 'COMPLETED', NULL, ?)",
                    arrayOf(
                        "Workout $i",
                        (1_000_000L + i * 1_000L),
                        (1_000_000L + i * 1_000L + 3_600_000L),
                        1_000_000L + i * 1_000L,
                    ),
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    @Test
    fun pagingAcrossLargeHistoryReturnsAllNewestFirst() = runTest {
        val total = 1_000
        seedCompletedSessions(total)

        val pageSize = 100
        val seen = mutableListOf<Long>()
        var offset = 0
        while (true) {
            val page = historyRepository.listCompleted(limit = pageSize, offset = offset)
            if (page.isEmpty()) break
            seen += page.map { it.startedAt.toEpochMilli() }
            offset += page.size
        }

        assertEquals(total, seen.size)
        // Newest-first: every element must be >= the next.
        assertTrue(seen.zipWithNext().all { (a, b) -> a >= b })
    }

    @Test
    fun firstPageIsMostRecentAndPagedCorrectly() = runTest {
        seedCompletedSessions(1_000)

        val page = historyRepository.listCompleted(limit = 30, offset = 0)
        assertEquals(30, page.size)
        // The most recent completed workout ("Workout 999") is first.
        assertEquals("Workout 999", page.first().routineNameSnapshot)
    }

    @Test
    fun pageBeyondEndReturnsEmpty() = runTest {
        seedCompletedSessions(50)
        assertTrue(historyRepository.listCompleted(limit = 30, offset = 60).isEmpty())
    }
}
