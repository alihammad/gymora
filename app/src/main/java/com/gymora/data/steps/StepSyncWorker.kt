package com.gymora.data.steps

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gymora.domain.repository.StepRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Runs shortly after every local midnight and records the step sensor's reading, so steps
 * taken while the app was closed are credited to the day they happened on instead of to
 * whichever day the app is next opened.
 */
object StepSyncScheduler {

    private const val WORK = "step-midnight-sync"

    /** Minutes after midnight the job is due; leaves room for clock and time-zone jitter. */
    private const val MINUTES_PAST_MIDNIGHT = 5L

    /** Queues the next run if none is waiting; safe to call on every app start. */
    fun ensureScheduled(context: Context) = enqueue(context, ExistingWorkPolicy.KEEP)

    internal fun scheduleNext(context: Context) = enqueue(context, ExistingWorkPolicy.REPLACE)

    private fun enqueue(context: Context, policy: ExistingWorkPolicy) {
        val now = LocalDateTime.now()
        val due = now.toLocalDate().plusDays(1).atStartOfDay().plusMinutes(MINUTES_PAST_MIDNIGHT)
        val request = OneTimeWorkRequestBuilder<StepSyncWorker>()
            .setInitialDelay(Duration.between(now, due).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK, policy, request)
    }
}

class StepSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun stepRepository(): StepRepository
    }

    override suspend fun doWork(): Result {
        try {
            EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
                .stepRepository()
                .recordSnapshot()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // Missing one night only costs some precision; never retry in a loop for it.
            Log.w(TAG, "Midnight step snapshot failed", e)
        } finally {
            StepSyncScheduler.scheduleNext(applicationContext)
        }
        return Result.success()
    }

    private companion object {
        const val TAG = "StepSync"
    }
}
