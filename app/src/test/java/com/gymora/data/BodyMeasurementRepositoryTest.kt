package com.gymora.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.entity.BodyMeasurementEntity
import com.gymora.data.repository.BodyMeasurementRepositoryImpl
import com.gymora.domain.model.BodyMetric
import com.gymora.domain.model.MetricPoint
import com.gymora.domain.model.series
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Body measurements are kept one own entry per date, with a history per metric. */
@RunWith(RobolectricTestRunner::class)
class BodyMeasurementRepositoryTest {

    private lateinit var database: GymoraDatabase
    private lateinit var repository: BodyMeasurementRepositoryImpl
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val zone: ZoneId = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 21)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, GymoraDatabase::class.java)
            .allowMainThreadQueries().build()
        repository = BodyMeasurementRepositoryImpl(database).apply {
            clock = Clock.fixed(today.atTime(9, 30).atZone(zone).toInstant(), zone)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun everyMetricRoundTrips() = runTest {
        val values = BodyMetric.entries.mapIndexed { i, m -> m to 10.0 + i }.toMap()
        repository.saveForDate(today, values)

        val saved = repository.observeAll().first().single()
        assertEquals(values, saved.values)
    }

    @Test
    fun savingTheSameDateAgainReplacesThatDate() = runTest {
        val first = repository.saveForDate(today, mapOf(BodyMetric.WEIGHT to 80.0, BodyMetric.NECK to 40.0))
        val second = repository.saveForDate(today, mapOf(BodyMetric.WEIGHT to 79.5, BodyMetric.LEFT_CALF to 38.0))

        assertEquals(first.id, second.id)
        val entry = repository.observeAll().first().single()
        assertEquals(79.5, entry[BodyMetric.WEIGHT]!!, 0.0)
        assertEquals(38.0, entry[BodyMetric.LEFT_CALF]!!, 0.0)
        assertNull("Left out = cleared on that date", entry[BodyMetric.NECK])
    }

    @Test
    fun pastDatesAreSeparateEntriesAtNoon() = runTest {
        val yesterday = today.minusDays(1)
        repository.saveForDate(yesterday, mapOf(BodyMetric.WEIGHT to 81.0))
        repository.saveForDate(today, mapOf(BodyMetric.WEIGHT to 80.0))

        val all = repository.observeAll().first()
        assertEquals(2, all.size)
        assertEquals(yesterday.atTime(LocalTime.NOON).atZone(zone).toInstant(), all.last().measuredAt)
        assertEquals(
            listOf(MetricPoint(yesterday, 81.0), MetricPoint(today, 80.0)),
            all.series(BodyMetric.WEIGHT, zone),
        )
    }

    @Test
    fun duplicateOwnEntriesOnADateAreFoldedIntoOne() = runTest {
        val dao = database.bodyMeasurementDao()
        val morning = today.atTime(7, 0).atZone(zone).toInstant().toEpochMilli()
        val evening = today.atTime(20, 0).atZone(zone).toInstant().toEpochMilli()
        val older = dao.insert(BodyMeasurementEntity(measuredAt = morning, weightKg = 80.0))
        val newer = dao.insert(BodyMeasurementEntity(measuredAt = evening, chestCm = 100.0))

        val result = repository.saveForDate(today, mapOf(BodyMetric.WEIGHT to 80.0, BodyMetric.CHEST to 100.0))

        assertEquals(newer, result.id)
        assertEquals(listOf(older), result.removedIds)
        assertEquals(1, dao.getAllOnce().size)
    }

    @Test
    fun importedWeighInsAreNotOverwritten() = runTest {
        val dao = database.bodyMeasurementDao()
        val at = today.atTime(6, 0).atZone(zone).toInstant().toEpochMilli()
        dao.insert(BodyMeasurementEntity(measuredAt = at, weightKg = 82.0, externalId = "hc-1"))

        repository.saveForDate(today, mapOf(BodyMetric.WAIST to 90.0))

        val all = repository.observeAll().first()
        assertEquals(2, all.size)
        assertTrue(all.any { it.imported && it[BodyMetric.WEIGHT] == 82.0 })
    }

    @Test
    fun deleteRemovesTheEntry() = runTest {
        val saved = repository.saveForDate(today, mapOf(BodyMetric.HIPS to 100.0))
        repository.delete(saved.id)
        assertTrue(repository.observeAll().first().isEmpty())
    }
}
