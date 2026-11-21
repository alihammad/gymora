package com.gymora.ui.account

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.gymora.account.Account
import com.gymora.account.AccountFiles
import com.gymora.account.AccountSession
import com.gymora.account.AccountStore
import com.gymora.account.ActiveAccount
import com.gymora.account.AuthException
import com.gymora.account.AuthService
import com.gymora.account.ProcessRestarter
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.testing.WorkManagerTestInitHelper
import com.gymora.data.local.db.GymoraDatabase
import com.gymora.data.local.sync.SyncTriggers
import com.gymora.data.sync.FakeSyncRemote
import com.gymora.data.sync.SyncEngine
import com.gymora.data.sync.SyncScheduler
import com.gymora.ui.navigation.Destinations
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Signing in, out and deleting: the cloud login and the local profile always move together. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AccountViewModelTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = AccountStore(context)
    private val restarter = CountingRestarter()
    private val auth = FakeAuth()
    private val alice = Account(id = "uid-alice", displayName = "Alice", email = "alice@example.com")

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        store.clear()
        ActiveAccount.reset()
        File(context.noBackupFilesDir, "accounts").deleteRecursively()
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
        store.clear()
        ActiveAccount.reset()
    }

    private val cloud = FakeSyncRemote()
    private val database = Room.inMemoryDatabaseBuilder(context, GymoraDatabase::class.java)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) = SyncTriggers.install(db)
        })
        .allowMainThreadQueries()
        .build()

    private fun viewModel(email: String? = null): AccountViewModel {
        ActiveAccount.reset()
        val handle = SavedStateHandle(mapOf(Destinations.SignIn.EMAIL_ARG to email))
        val session = AccountSession(context, store, restarter)
        return AccountViewModel(
            auth,
            session,
            SyncEngine(database, cloud),
            SyncScheduler(context, database, session),
            handle,
        )
    }

    @Test
    fun emailSignInSwitchesToTheAccountAndKeepsGuestDataByDefault() {
        val vm = viewModel()
        vm.onEmailChanged("alice@example.com")
        vm.onPasswordChanged("secret1")
        vm.onSubmit()

        assertEquals("alice@example.com" to "secret1", auth.lastSignIn)
        assertEquals(1, restarter.restarts)
        assertEquals(alice.id, store.activeId())
        assertEquals(alice.id, store.pendingGuestAdoption)
    }

    @Test
    fun guestDataStaysWithTheGuestWhenUnticked() {
        val vm = viewModel()
        vm.onEmailChanged("alice@example.com")
        vm.onPasswordChanged("secret1")
        vm.onKeepGuestDataChanged(false)
        vm.onSubmit()

        assertEquals(alice.id, store.activeId())
        assertNull(store.pendingGuestAdoption)
    }

    @Test
    fun creatingAnAccountNeedsAName() {
        val vm = viewModel()
        vm.onModeToggled()
        vm.onEmailChanged("alice@example.com")
        vm.onPasswordChanged("secret1")
        assertFalse(vm.uiState.value.canSubmit)

        vm.onNameChanged("Alice")
        vm.onSubmit()
        assertEquals("Alice", auth.createdName)
        assertEquals(alice.id, store.activeId())
    }

    @Test
    fun aFailedSignInShowsTheReasonAndChangesNothing() {
        auth.failWith = AuthException.InvalidCredentials()
        val vm = viewModel()
        vm.onEmailChanged("alice@example.com")
        vm.onPasswordChanged("wrong")
        vm.onSubmit()

        assertEquals(AuthException.InvalidCredentials().message, vm.uiState.value.error)
        assertFalse(vm.uiState.value.busy)
        assertEquals(0, restarter.restarts)
        assertEquals(Account.GUEST_ID, store.activeId())
    }

    @Test
    fun googleSheetClosedDoesNothing() {
        viewModel().onGoogleToken(null)
        assertEquals(0, restarter.restarts)
    }

    @Test
    fun signOutEndsTheLoginAndKeepsTheDataOnThePhone() {
        signedInAsAlice()
        AccountFiles.databaseFile(context, alice.id).apply { parentFile?.mkdirs(); writeText("db") }

        viewModel().onSignOutConfirmed()

        assertNull(auth.currentUserId)
        assertEquals(Account.GUEST_ID, store.activeId())
        viewModel() // next start
        assertTrue(AccountFiles.databaseFile(context, alice.id).exists())
    }

    @Test
    fun deletingTheAccountRemovesTheLoginAndTheLocalData() {
        signedInAsAlice()
        AccountFiles.databaseFile(context, alice.id).apply { parentFile?.mkdirs(); writeText("db") }

        val vm = viewModel()
        vm.onDeleteConfirmed()
        vm.awaitOutcome()

        assertEquals(alice.id, cloud.deletedAllFor)
        assertTrue(auth.deleted)
        assertEquals(Account.GUEST_ID, store.activeId())
        viewModel() // next start applies the deletion
        assertFalse(AccountFiles.databaseFile(context, alice.id).exists())
        assertEquals(listOf(Account.GUEST), store.accounts())
    }

    @Test
    fun deletionThatNeedsAFreshLoginKeepsEverything() {
        signedInAsAlice()
        auth.isLoginRecent = false
        val vm = viewModel()

        vm.onDeleteConfirmed()

        assertEquals(AuthException.RecentLoginRequired().message, vm.uiState.value.error)
        // Checked before anything is deleted, in the cloud or here.
        assertNull(cloud.deletedAllFor)
        assertFalse(auth.deleted)
        assertEquals(alice.id, store.activeId())
    }

    @Test
    fun cloudDeletionFailureKeepsTheAccount() {
        signedInAsAlice()
        cloud.failNext = IllegalStateException("offline")
        val vm = viewModel()

        vm.onDeleteConfirmed()
        vm.awaitOutcome()

        assertEquals(AuthException.CloudDeleteFailed().message, vm.uiState.value.error)
        assertFalse(auth.deleted)
        assertEquals(alice.id, store.activeId())
    }

    @Test
    fun switchingPrefillsTheOtherAccountsEmail() {
        assertEquals("alice@example.com", viewModel(email = "alice@example.com").uiState.value.email)
    }

    @Test
    fun forgotPasswordAsksForTheEmailFirst() {
        val vm = viewModel()
        vm.onForgotPassword()
        assertTrue(vm.uiState.value.error!!.contains("email"))

        vm.onEmailChanged("alice@example.com")
        vm.onForgotPassword()
        assertEquals("alice@example.com", auth.resetSentTo)
    }

    /** Deletion talks to the cloud off the main thread; wait for it to restart the app or show an error. */
    private fun AccountViewModel.awaitOutcome() {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (restarter.restarts == 0 && uiState.value.error == null) {
            check(System.currentTimeMillis() < deadline) { "Deletion did not finish" }
            Thread.sleep(POLL_MS)
        }
    }

    private fun signedInAsAlice() {
        store.upsert(alice)
        store.setActive(alice.id)
        auth.currentUserId = alice.id
    }

    private class CountingRestarter : ProcessRestarter {
        var restarts = 0
        override fun restart() {
            restarts++
        }
    }

    private inner class FakeAuth : AuthService {
        override var currentUserId: String? = null
        override var isLoginRecent = true
        var failWith: AuthException? = null
        var lastSignIn: Pair<String, String>? = null
        var createdName: String? = null
        var resetSentTo: String? = null
        var deleted = false

        private fun succeed(): Account {
            failWith?.let { throw it }
            currentUserId = alice.id
            return alice
        }

        override suspend fun signInWithEmail(email: String, password: String): Account {
            lastSignIn = email to password
            return succeed()
        }

        override suspend fun createAccount(name: String, email: String, password: String): Account {
            createdName = name
            return succeed()
        }

        override suspend fun signInWithGoogle(idToken: String) = succeed()

        override suspend fun sendPasswordReset(email: String) {
            failWith?.let { throw it }
            resetSentTo = email
        }

        override fun signOut() {
            currentUserId = null
        }

        override suspend fun deleteCurrentUser() {
            failWith?.let { throw it }
            deleted = true
            currentUserId = null
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val POLL_MS = 10L
    }
}
