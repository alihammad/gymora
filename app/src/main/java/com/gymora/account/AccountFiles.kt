package com.gymora.account

import android.content.Context
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.media.ExerciseMediaStore
import java.io.File
import java.security.MessageDigest

/**
 * Where each account's data lives. Every account has its own database and media folder,
 * so one person's workouts can never show up in another's queries.
 *
 * The guest keeps the original locations, so data from before accounts existed is untouched
 * and still covered by Android Auto Backup. Signed-in accounts live under noBackupFilesDir:
 * their copy is the cloud, and a device backup must not carry one person's data to another's phone.
 */
object AccountFiles {

    private const val ACCOUNTS_DIR = "accounts"
    private const val DIR_NAME_CHARS = 32

    /** SQLite's companion files that must move together with the database. */
    private val DATABASE_SUFFIXES = listOf("", "-wal", "-shm", "-journal")

    fun databaseFile(context: Context, accountId: String): File =
        if (accountId == Account.GUEST_ID) {
            context.getDatabasePath(GymoraDatabase.NAME)
        } else {
            File(accountDir(context, accountId), GymoraDatabase.NAME)
        }

    fun mediaDir(context: Context, accountId: String): File =
        if (accountId == Account.GUEST_ID) {
            File(context.filesDir, ExerciseMediaStore.DIRECTORY)
        } else {
            File(accountDir(context, accountId), ExerciseMediaStore.DIRECTORY)
        }

    /** Hashed so the folder name works for any id and does not expose it. */
    fun accountDir(context: Context, accountId: String): File {
        require(accountId != Account.GUEST_ID) { "The guest uses the original locations" }
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(accountId.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(DIR_NAME_CHARS)
        return File(File(context.noBackupFilesDir, ACCOUNTS_DIR), hash)
    }

    /**
     * Moves the guest's database and media to [accountId], for someone signing in for the
     * first time who wants to keep what they logged as a guest. Refuses if the account
     * already has a database on this device. Must run while no database is open.
     */
    fun adoptGuestData(context: Context, accountId: String): Boolean {
        val target = databaseFile(context, accountId)
        val source = databaseFile(context, Account.GUEST_ID)
        if (target.exists() || !source.exists()) return false
        target.parentFile?.mkdirs()
        DATABASE_SUFFIXES.forEach { suffix ->
            val from = File(source.path + suffix)
            if (from.exists() && !from.renameTo(File(target.path + suffix))) return false
        }
        val guestMedia = mediaDir(context, Account.GUEST_ID)
        if (guestMedia.exists()) guestMedia.renameTo(mediaDir(context, accountId))
        return true
    }

    /** Removes everything [accountId] stored on this device. Must run while its database is closed. */
    fun deleteAccountData(context: Context, accountId: String) {
        accountDir(context, accountId).deleteRecursively()
    }
}
