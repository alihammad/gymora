package com.gymora.data.sync

import android.content.Context
import android.util.Log
import androidx.room.InvalidationTracker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.gymora.account.AccountSession
import com.gymora.account.AuthService
import com.gymora.data.local.db.GymoraDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * When sync runs, all only while signed in and online:
 * - shortly after a local change, if it left something to upload (no network otherwise);
 * - when the app opens, and on "Sync now";
 * - every few hours in the background, to bring in changes made on other phones.
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GymoraDatabase,
    private val session: AccountSession,
) {

    private val workManager get() = WorkManager.getInstance(context)

    private val enabled get() = !session.activeAccount.isGuest

    /** Starts the periodic sync and watches for local changes; call once at app start. */
    fun start() {
        if (!enabled) {
            // A guest never syncs; stop whatever an account scheduled before switching.
            workManager.cancelUniqueWork(PERIODIC)
            workManager.cancelUniqueWork(NOW)
            workManager.cancelUniqueWork(AFTER_CHANGE)
            return
        }
        val periodic = PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_HOURS, TimeUnit.HOURS)
            .setConstraints(ONLINE)
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, periodic)
        database.invalidationTracker.addObserver(
            object : InvalidationTracker.Observer(WATCHED_TABLES) {
                override fun onInvalidated(tables: Set<String>) = afterChange()
            },
        )
    }

    /** A full sync as soon as there is a connection. */
    fun syncNow() {
        if (!enabled) return
        workManager.enqueueUniqueWork(NOW, ExistingWorkPolicy.KEEP, request(onlyIfPending = false))
    }

    /** Whether a sync is running or waiting for a connection. */
    val isSyncing: Flow<Boolean>
        get() = workManager.getWorkInfosForUniqueWorkFlow(NOW).map { infos ->
            infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
        }

    /** Debounced: every change restarts the wait, so a burst of edits uploads once. */
    private fun afterChange() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(ONLINE)
            .setInitialDelay(CHANGE_DELAY_SECONDS, TimeUnit.SECONDS)
            .setInputData(workDataOf(SyncWorker.ONLY_IF_PENDING to true))
            .build()
        workManager.enqueueUniqueWork(AFTER_CHANGE, ExistingWorkPolicy.REPLACE, request)
    }

    private fun request(onlyIfPending: Boolean) = OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(ONLINE)
        .setInputData(workDataOf(SyncWorker.ONLY_IF_PENDING to onlyIfPending))
        .build()

    private companion object {
        const val PERIODIC = "sync-periodic"
        const val NOW = "sync-now"
        const val AFTER_CHANGE = "sync-after-change"
        const val PERIODIC_HOURS = 6L
        const val CHANGE_DELAY_SECONDS = 20L
        val ONLINE = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val WATCHED_TABLES = arrayOf(
            "exercises", "routines", "routine_exercises", "set_templates", "workout_sessions",
            "workout_exercises", "workout_sets", "body_measurements", "settings", "sync_tombstones",
        )
    }
}

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun accountSession(): AccountSession
        fun authService(): AuthService
        fun syncEngine(): SyncEngine
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
        val account = deps.accountSession().activeAccount
        // Only the signed-in owner's data, and only with their login (the cloud rules need it).
        if (account.isGuest || deps.authService().currentUserId != account.id) return Result.success()
        return try {
            val onlyIfPending = inputData.getBoolean(ONLY_IF_PENDING, false)
            val result = deps.syncEngine().sync(account.id, onlyIfPending)
            Log.i(TAG, "Synced: ${result.downloaded} down, ${result.uploaded} up")
            Result.success()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Log.w(TAG, "Sync failed", e)
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val ONLY_IF_PENDING = "only_if_pending"
        private const val TAG = "Sync"
        private const val MAX_ATTEMPTS = 3
    }
}
