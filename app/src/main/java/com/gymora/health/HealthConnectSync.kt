package com.gymora.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.entity.BodyMeasurementEntity
import com.gymora.data.local.entity.WorkoutSessionEntity
import com.gymora.domain.model.SessionStatus
import com.gymora.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Whether Health Connect can be used on this device. */
enum class HealthConnectAvailability {
    AVAILABLE,

    /** Installed but needs an update from the Play Store. */
    UPDATE_REQUIRED,
    NOT_SUPPORTED,
}

/** Counts from a full sync. */
data class HealthSyncResult(val workoutsWritten: Int, val weightsWritten: Int, val weightsImported: Int)

/**
 * Two-way sync with Health Connect. Writes completed workouts as strength-training
 * sessions and Gymora's own weigh-ins as weight records; reads weigh-ins other apps
 * (e.g. a smart scale) recorded into body measurements.
 *
 * Writes carry a stable client record id, so re-syncing updates records instead of
 * duplicating them. Imported weigh-ins remember their Health Connect id and are never
 * written back. Nothing here throws to callers that fire and forget.
 */
@Singleton
class HealthConnectSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GymoraDatabase,
    private val settingsRepository: SettingsRepository,
) {

    /** Outlives screens: a finished workout is written even if the user navigates away. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun availability(): HealthConnectAvailability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
        else -> HealthConnectAvailability.NOT_SUPPORTED
    }

    private val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    suspend fun hasAllPermissions(): Boolean =
        availability() == HealthConnectAvailability.AVAILABLE &&
            client.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)

    /** Whether Gymora may read steps; asked for separately so workout/weight sync is unaffected. */
    suspend fun hasStepsPermission(): Boolean =
        availability() == HealthConnectAvailability.AVAILABLE &&
            client.permissionController.getGrantedPermissions().contains(STEPS_PERMISSION)

    /**
     * Steps recorded today by all apps and devices that write to Health Connect, which it
     * de-duplicates across sources. Null when the user hasn't turned this on, permission is
     * missing, or Health Connect can't be read.
     */
    suspend fun todaySteps(): Int? = runCatching {
        if (!settingsRepository.observeSettings().first().healthStepsEnabled || !hasStepsPermission()) {
            return@runCatching null
        }
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val result = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, Instant.now()),
            ),
        )
        (result[StepsRecord.COUNT_TOTAL] ?: 0L).toInt()
    }.getOrNull()

    /** Daily step totals for the last [days] days (today included); null when [todaySteps] would be. */
    suspend fun dailySteps(days: Int): Map<LocalDate, Int>? = runCatching {
        if (!settingsRepository.observeSettings().first().healthStepsEnabled || !hasStepsPermission()) {
            return@runCatching null
        }
        val start = LocalDate.now().minusDays(days - 1L).atStartOfDay()
        val buckets = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, java.time.LocalDateTime.now()),
                timeRangeSlicer = java.time.Period.ofDays(1),
            ),
        )
        buckets.associate { it.startTime.toLocalDate() to (it.result[StepsRecord.COUNT_TOTAL] ?: 0L).toInt() }
    }.getOrNull()

    /** After a workout is finished. No-op unless sync is on and permitted. */
    fun onWorkoutFinished(sessionId: Long) = launchIfEnabled {
        database.workoutSessionDao().getById(sessionId)?.let { client.insertRecords(listOf(sessionRecord(it))) }
    }

    /** After a body measurement is saved. No-op unless sync is on and permitted. */
    fun onBodyMeasurementSaved() = launchIfEnabled { writeOwnWeights() }

    /** Writes every completed workout and own weigh-in, then imports other apps' weigh-ins. */
    suspend fun syncAll(): HealthSyncResult {
        val sessions = database.workoutSessionDao().listCompleted(Int.MAX_VALUE, 0)
            .filter { it.status == SessionStatus.COMPLETED.name && it.endedAt != null }
        sessions.chunked(BATCH_SIZE).forEach { batch -> client.insertRecords(batch.map(::sessionRecord)) }
        val written = writeOwnWeights()
        val imported = importWeights()
        return HealthSyncResult(sessions.size, written, imported)
    }

    private fun launchIfEnabled(block: suspend () -> Unit) {
        scope.launch {
            runCatching {
                if (settingsRepository.observeSettings().first().healthConnectEnabled && hasAllPermissions()) block()
            }
        }
    }

    private fun sessionRecord(session: WorkoutSessionEntity): ExerciseSessionRecord {
        val start = Instant.ofEpochMilli(session.startedAt)
        // Health Connect rejects sessions that end before they start.
        val end = Instant.ofEpochMilli(maxOf(session.endedAt ?: session.startedAt, session.startedAt + 1_000))
        val zone = ZoneId.systemDefault().rules
        return ExerciseSessionRecord(
            startTime = start,
            startZoneOffset = zone.getOffset(start),
            endTime = end,
            endZoneOffset = zone.getOffset(end),
            metadata = Metadata.manualEntry(
                clientRecordId = "gymora-session-${session.id}",
                clientRecordVersion = session.endedAt ?: session.startedAt,
            ),
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            title = session.routineNameSnapshot,
            notes = session.notes,
        )
    }

    private suspend fun writeOwnWeights(): Int {
        val own = database.bodyMeasurementDao().getAllOnce()
            .filter { it.externalId == null && it.weightKg != null && it.weightKg > 0 }
        val zone = ZoneId.systemDefault().rules
        own.chunked(BATCH_SIZE).forEach { batch ->
            client.insertRecords(
                batch.map { entry ->
                    val time = Instant.ofEpochMilli(entry.measuredAt)
                    WeightRecord(
                        time = time,
                        zoneOffset = zone.getOffset(time),
                        weight = Mass.kilograms(entry.weightKg!!),
                        metadata = Metadata.manualEntry(
                            clientRecordId = "gymora-weight-${entry.id}",
                            clientRecordVersion = entry.measuredAt,
                        ),
                    )
                },
            )
        }
        return own.size
    }

    /** Weigh-ins from other apps in the last [IMPORT_DAYS] days that are not yet stored. */
    private suspend fun importWeights(): Int {
        val dao = database.bodyMeasurementDao()
        val known = dao.getAllOnce().mapNotNull { it.externalId }.toSet()
        val since = Instant.now().minus(IMPORT_DAYS, ChronoUnit.DAYS)
        var imported = 0
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(since),
                    pageToken = pageToken,
                ),
            )
            response.records
                .filter { it.metadata.dataOrigin.packageName != context.packageName && it.metadata.id !in known }
                .forEach { record ->
                    dao.insert(
                        BodyMeasurementEntity(
                            measuredAt = record.time.toEpochMilli(),
                            weightKg = record.weight.inKilograms,
                            shouldersCm = null,
                            chestCm = null,
                            aboveNavelCm = null,
                            navelCm = null,
                            belowNavelCm = null,
                            thighCm = null,
                            externalId = record.metadata.id,
                        ),
                    )
                    imported++
                }
            pageToken = response.pageToken
        } while (pageToken != null)
        return imported
    }

    companion object {
        private const val BATCH_SIZE = 100
        private const val IMPORT_DAYS = 365L * 2

        val PERMISSIONS = setOf(
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(WeightRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
        )

        val STEPS_PERMISSION = HealthPermission.getReadPermission(StepsRecord::class)

        /** Launches Health Connect's permission screen. */
        fun permissionContract() = PermissionController.createRequestPermissionResultContract()
    }
}
