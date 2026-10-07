package com.gymora.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.gymora.data.local.dao.StepDao
import com.gymora.domain.repository.StepRepository
import com.gymora.health.HealthConnectSync
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class StepRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stepDao: StepDao,
    private val healthConnectSync: HealthConnectSync,
) : StepRepository {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    override val isSupported: Boolean = stepSensor != null

    override fun observeTodaySteps(useSensor: Boolean): Flow<Int?> {
        val sensor: Flow<Int?> = if (useSensor && isSupported) sensorSteps() else flowOf(null)
        return combine(sensor, healthSteps()) { fromSensor, fromHealth ->
            if (fromSensor == null && fromHealth == null) null else maxOf(fromSensor ?: 0, fromHealth ?: 0)
        }.distinctUntilChanged()
    }

    override suspend fun recordSnapshot() {
        if (!isSupported || !hasActivityPermission()) return
        val counter = withTimeoutOrNull(SNAPSHOT_TIMEOUT_MS) { sensorReadings().first() } ?: return
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        // The job runs just after midnight, so the steps since the last reading belong to
        // yesterday, unless today already has a row (the app was opened since midnight and
        // claimed them).
        val date = if (now.hour < LATE_NIGHT_HOUR && stepDao.get(today.toString()) == null) {
            today.minusDays(1)
        } else {
            today
        }
        stepDao.recordCounter(date.toString(), date.minusDays(1).toString(), counter)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun sensorSteps(): Flow<Int?> = channelFlow {
        launch {
            sensorReadings().collect { counter ->
                val date = LocalDate.now()
                stepDao.recordCounter(date.toString(), date.minusDays(1).toString(), counter)
            }
        }
        today()
            .flatMapLatest { date -> stepDao.observeSteps(date.toString()).map { it ?: 0 } }
            .collect { send(it) }
    }

    /** Health Connect has no change feed for steps, so poll while someone is looking. */
    private fun healthSteps(): Flow<Int?> = flow {
        while (true) {
            emit(healthConnectSync.todaySteps())
            delay(HEALTH_POLL_MS)
        }
    }

    /** Emits today's date now and again at every local midnight. */
    private fun today(): Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now())
            val now = LocalDateTime.now()
            delay(Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis() + 1)
        }
    }

    /** Raw step-counter readings (steps since boot); the sensor reports the current value on registration. */
    private fun sensorReadings(): Flow<Long> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(event.values[0].toLong())
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager?.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager?.unregisterListener(listener) }
    }

    private fun hasActivityPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val SNAPSHOT_TIMEOUT_MS = 10_000L
        const val HEALTH_POLL_MS = 60_000L

        /** A midnight job that ran before this hour still counts as "just after midnight". */
        const val LATE_NIGHT_HOUR = 3
    }
}
