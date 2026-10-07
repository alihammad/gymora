package com.gymora.data.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gymora.domain.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/** Schedules the periodic backup to the folder picked in Settings. */
object AutoBackupScheduler {

    private const val PERIODIC_WORK = "auto-backup"
    private const val NOW_WORK = "auto-backup-now"

    /** [intervalDays] 0 cancels. Keeps the current schedule when the interval is unchanged. */
    fun schedule(context: Context, intervalDays: Int, folderUri: String?) {
        val workManager = WorkManager.getInstance(context)
        if (intervalDays <= 0 || folderUri == null) {
            workManager.cancelUniqueWork(PERIODIC_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(intervalDays.toLong(), TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .addTag(PERIODIC_WORK)
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** Runs one backup to the configured folder right away. */
    fun runNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            NOW_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<AutoBackupWorker>().build(),
        )
    }
}

/**
 * Writes a full backup into the user's chosen folder (any document provider: local
 * storage, an SD card, or a cloud app that offers folders) and keeps the newest [KEEP].
 */
class AutoBackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun backupService(): BackupService
        fun settingsRepository(): SettingsRepository
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
        val settings = deps.settingsRepository().observeSettings().first()
        val folder = settings.autoBackupFolderUri?.let(Uri::parse) ?: return Result.success()
        return try {
            val resolver = applicationContext.contentResolver
            val parent = DocumentsContract.buildDocumentUriUsingTree(
                folder,
                DocumentsContract.getTreeDocumentId(folder),
            )
            val name = PREFIX + LocalDateTime.now().format(STAMP) + ".zip"
            val file = DocumentsContract.createDocument(resolver, parent, BackupService.MIME_TYPE, name)
                ?: return Result.retry()
            resolver.openOutputStream(file, "wt")?.use { deps.backupService().export(it) }
                ?: return Result.retry()
            pruneOldBackups(folder)
            deps.settingsRepository().setLastAutoBackupAt(System.currentTimeMillis())
            Result.success()
        } catch (e: SecurityException) {
            // Folder access was revoked or the folder is gone; the user must pick it again.
            Log.w(TAG, "Backup folder is no longer accessible", e)
            Result.failure()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Log.w(TAG, "Automatic backup failed", e)
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private fun pruneOldBackups(folder: Uri) {
        val resolver = applicationContext.contentResolver
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            folder,
            DocumentsContract.getTreeDocumentId(folder),
        )
        val backups = mutableListOf<Pair<String, String>>() // documentId to name
        resolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(1) ?: continue
                if (name.startsWith(PREFIX) && name.endsWith(".zip")) backups += cursor.getString(0) to name
            }
        }
        // Timestamped names sort chronologically.
        backups.sortedByDescending { it.second }.drop(KEEP).forEach { (id, _) ->
            runCatching {
                DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(folder, id))
            }
        }
    }

    private companion object {
        const val TAG = "AutoBackup"
        const val PREFIX = "gymora-auto-"
        const val KEEP = 7
        const val MAX_ATTEMPTS = 3
        val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm")
    }
}
