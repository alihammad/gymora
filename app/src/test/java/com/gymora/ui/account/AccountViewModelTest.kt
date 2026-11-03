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
        Dispatchers.setMain(UnconfinedTestDispatcher())
        store.clear()
        ActiveAccount.reset()
        File(context.noBackupFilesDir, "accounts").deleteRecursively()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        store.clear()
        ActiveAccount.reset()
    }

    private fun viewModel(email: String? = null): AccountViewModel {
        ActiveAccount.reset()
        val handle = SavedStateHandle(mapOf(Destinations.SignIn.EMAIL_ARG to email))
        return AccountViewModel(auth, AccountSession(context, store, restarter), handle)
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

        viewModel().onDeleteConfirmed()

        assertTrue(auth.deleted)
        assertEquals(Account.GUEST_ID, store.activeId())
        viewModel() // next start applies the deletion
        assertFalse(AccountFiles.databaseFile(context, alice.id).exists())
        assertEquals(listOf(Account.GUEST), store.accounts())
    }

    @Test
    fun deletionThatNeedsAFreshLoginKeepsEverything() {
        signedInAsAlice()
        auth.failWith = AuthException.RecentLoginRequired()
        val vm = viewModel()

        vm.onDeleteConfirmed()

        assertEquals(AuthException.RecentLoginRequired().message, vm.uiState.value.error)
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
}
