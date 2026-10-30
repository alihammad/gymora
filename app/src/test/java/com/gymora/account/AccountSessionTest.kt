package com.gymora.account

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.media.ExerciseMediaStore
import com.gymora.data.repository.SettingsRepositoryImpl
import com.gymora.domain.model.WeightUnit
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Accounts on one device: each has its own data, and switching never mixes or loses it. */
@RunWith(RobolectricTestRunner::class)
class AccountSessionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = AccountStore(context)
    private val restarter = FakeRestarter()
    private val alice = Account(id = "uid-alice", displayName = "Alice", email = "alice@example.com")
    private val bob = Account(id = "uid-bob", displayName = "Bob")

    @Before
    fun setUp() = reset()

    @After
    fun tearDown() = reset()

    private fun reset() {
        store.clear()
        ActiveAccount.reset()
        AccountFiles.databaseFile(context, Account.GUEST_ID).let { db ->
            listOf("", "-wal", "-shm", "-journal").forEach { File(db.path + it).delete() }
        }
        AccountFiles.mediaDir(context, Account.GUEST_ID).deleteRecursively()
        File(context.noBackupFilesDir, "accounts").deleteRecursively()
    }

    /** What the next process start sees. */
    private fun restartedSession(): AccountSession {
        ActiveAccount.reset()
        return AccountSession(context, store, restarter)
    }

    private fun openDatabase(accountId: String): GymoraDatabase {
        val file = AccountFiles.databaseFile(context, accountId).apply { parentFile?.mkdirs() }
        return Room.databaseBuilder(context, GymoraDatabase::class.java, file.absolutePath)
            .allowMainThreadQueries().build()
    }

    @Test
    fun guestIsActiveByDefaultAndKeepsTheOriginalLocations() {
        val session = restartedSession()
        assertTrue(session.activeAccount.isGuest)
        assertEquals(context.getDatabasePath(GymoraDatabase.NAME), AccountFiles.databaseFile(context, Account.GUEST_ID))
        assertEquals(
            File(context.filesDir, ExerciseMediaStore.DIRECTORY),
            AccountFiles.mediaDir(context, Account.GUEST_ID),
        )
    }

    @Test
    fun signedInAccountsGetSeparateFoldersOutsideAutoBackup() {
        val aliceDb = AccountFiles.databaseFile(context, alice.id)
        val bobDb = AccountFiles.databaseFile(context, bob.id)
        assertNotEquals(aliceDb, bobDb)
        assertTrue(aliceDb.path.startsWith(context.noBackupFilesDir.path))
        // The folder name is a hash, not the account id.
        assertFalse(aliceDb.path.contains(alice.id))
    }

    @Test
    fun signInSwitchesAndRestarts() {
        restartedSession().signIn(alice, keepGuestData = false)
        assertEquals(1, restarter.restarts)
        assertEquals(alice, restartedSession().activeAccount)
        assertEquals(listOf(Account.GUEST, alice), store.accounts())
    }

    @Test
    fun switchingToTheActiveAccountDoesNothing() {
        restartedSession().signIn(alice, keepGuestData = false)
        restartedSession().switchTo(alice.id)
        assertEquals(1, restarter.restarts)
    }

    @Test
    fun eachAccountSeesOnlyItsOwnSettings() = runTest {
        openDatabase(alice.id).useSettings { it.setWeightUnit(WeightUnit.LB) }
        openDatabase(bob.id).useSettings { assertEquals(WeightUnit.KG, it.observeSettings().first().weightUnit) }
        openDatabase(alice.id).useSettings { assertEquals(WeightUnit.LB, it.observeSettings().first().weightUnit) }
    }

    @Test
    fun keepingGuestDataMovesItIntoTheAccountOnRestart() = runTest {
        openDatabase(Account.GUEST_ID).useSettings { it.setWeightUnit(WeightUnit.LB) }
        File(AccountFiles.mediaDir(context, Account.GUEST_ID).apply { mkdirs() }, "curl.gif").writeText("gif")

        restartedSession().signIn(alice, keepGuestData = true)
        restartedSession()

        openDatabase(alice.id).useSettings { assertEquals(WeightUnit.LB, it.observeSettings().first().weightUnit) }
        assertTrue(File(AccountFiles.mediaDir(context, alice.id), "curl.gif").exists())
        // The guest starts fresh.
        assertFalse(AccountFiles.databaseFile(context, Account.GUEST_ID).exists())
    }

    @Test
    fun guestDataNeverOverwritesAnAccountsExistingData() = runTest {
        openDatabase(alice.id).useSettings { it.setWeightUnit(WeightUnit.LB) }
        openDatabase(Account.GUEST_ID).useSettings { it.setWeightUnit(WeightUnit.KG) }

        restartedSession().signIn(alice, keepGuestData = true)
        restartedSession()

        openDatabase(alice.id).useSettings { assertEquals(WeightUnit.LB, it.observeSettings().first().weightUnit) }
        assertTrue(AccountFiles.databaseFile(context, Account.GUEST_ID).exists())
    }

    @Test
    fun signOutKeepsLocalDataUnlessAskedToRemoveIt() = runTest {
        restartedSession().signIn(alice, keepGuestData = false)
        openDatabase(alice.id).useSettings { it.setWeightUnit(WeightUnit.LB) }

        restartedSession().signOut(removeLocalData = false)
        assertTrue(restartedSession().activeAccount.isGuest)
        assertTrue(AccountFiles.databaseFile(context, alice.id).exists())

        restartedSession().switchTo(alice.id)
        restartedSession().signOut(removeLocalData = true)
        // Deleted on the restart, once the database is closed.
        assertTrue(AccountFiles.databaseFile(context, alice.id).exists())
        restartedSession()
        assertFalse(AccountFiles.databaseFile(context, alice.id).exists())
        assertEquals(listOf(Account.GUEST), store.accounts())
    }

    @Test
    fun removingAnotherAccountDeletesItsDataRightAway() = runTest {
        restartedSession().signIn(bob, keepGuestData = false)
        openDatabase(bob.id).useSettings { it.setWeightUnit(WeightUnit.LB) }
        restartedSession().signIn(alice, keepGuestData = false)

        restartedSession().removeFromDevice(bob.id)

        assertFalse(AccountFiles.databaseFile(context, bob.id).exists())
        assertEquals(listOf(Account.GUEST, alice), store.accounts())
    }

    @Test
    fun aForgottenActiveAccountFallsBackToGuest() {
        restartedSession().signIn(alice, keepGuestData = false)
        store.remove(alice.id)
        assertTrue(restartedSession().activeAccount.isGuest)
    }

    @Test
    fun signingInAgainRefreshesTheProfileWithoutDuplicating() {
        restartedSession().signIn(alice, keepGuestData = false)
        restartedSession().signIn(alice.copy(displayName = "Alice B"), keepGuestData = false)
        assertEquals(listOf(Account.GUEST, alice.copy(displayName = "Alice B")), store.accounts())
    }

    private suspend fun GymoraDatabase.useSettings(block: suspend (SettingsRepositoryImpl) -> Unit) {
        try {
            block(SettingsRepositoryImpl(settingsDao()))
        } finally {
            close()
        }
    }

    private class FakeRestarter : ProcessRestarter {
        var restarts = 0
        override fun restart() {
            restarts++
        }
    }
}
