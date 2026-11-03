package com.gymora.ui.account

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymora.account.Account
import com.gymora.account.AccountSession
import com.gymora.account.AuthException
import com.gymora.account.AuthService
import com.gymora.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val active: Account,
    /** Signed-in accounts on this phone other than the active one. */
    val otherAccounts: List<Account> = emptyList(),
    val creating: Boolean = false,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    /** Offered when signing in from the guest profile. */
    val keepGuestData: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val confirmSignOut: Boolean = false,
    val confirmDelete: Boolean = false,
    val confirmRemove: Account? = null,
) {
    val canSubmit: Boolean
        get() = !busy && email.isNotBlank() && password.isNotEmpty() && (!creating || name.isNotBlank())
}

/**
 * Sign-in, sign-up, sign-out and account deletion. A successful sign-in hands the
 * account to [AccountSession], which restarts the app into that account's own data.
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val auth: AuthService,
    private val session: AccountSession,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AccountUiState(
            active = session.activeAccount,
            otherAccounts = otherAccounts(),
            email = savedStateHandle.get<String>(Destinations.SignIn.EMAIL_ARG).orEmpty(),
        ),
    )
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    fun onNameChanged(value: String) = _uiState.update { it.copy(name = value, error = null) }

    fun onEmailChanged(value: String) = _uiState.update { it.copy(email = value, error = null) }

    fun onPasswordChanged(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun onKeepGuestDataChanged(value: Boolean) = _uiState.update { it.copy(keepGuestData = value) }

    fun onModeToggled() = _uiState.update { it.copy(creating = !it.creating, error = null) }

    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        signIn {
            if (state.creating) {
                auth.createAccount(state.name, state.email, state.password)
            } else {
                auth.signInWithEmail(state.email, state.password)
            }
        }
    }

    /** [idToken] from the Google sheet; null when the user closed it. */
    fun onGoogleToken(idToken: String?) {
        idToken ?: return
        signIn { auth.signInWithGoogle(idToken) }
    }

    fun onError(error: AuthException) = _uiState.update { it.copy(busy = false, error = error.message) }

    fun onForgotPassword() {
        val email = _uiState.value.email
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Enter your email first, then tap Forgot password.") }
            return
        }
        launchAuth {
            auth.sendPasswordReset(email)
            _uiState.update { it.copy(busy = false, message = "Check $email for a link to reset your password.") }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun onSignOutClicked() = _uiState.update { it.copy(confirmSignOut = true) }

    fun onSignOutConfirmed() {
        _uiState.update { it.copy(confirmSignOut = false) }
        auth.signOut()
        // Kept on the phone: the cloud copy does not exist until sync ships.
        session.signOut(removeLocalData = false)
    }

    fun onDeleteClicked() = _uiState.update { it.copy(confirmDelete = true) }

    fun onDeleteConfirmed() {
        _uiState.update { it.copy(confirmDelete = false) }
        launchAuth {
            auth.deleteCurrentUser()
            session.signOut(removeLocalData = true)
        }
    }

    fun onRemoveClicked(account: Account) = _uiState.update { it.copy(confirmRemove = account) }

    fun onRemoveConfirmed() {
        val account = _uiState.value.confirmRemove ?: return
        session.removeFromDevice(account.id)
        _uiState.update { it.copy(confirmRemove = null, otherAccounts = otherAccounts()) }
    }

    fun onDialogDismissed() =
        _uiState.update { it.copy(confirmSignOut = false, confirmDelete = false, confirmRemove = null) }

    /** Continue as guest from a signed-in account; same as signing out, keeping the data. */
    fun onUseAsGuest() = onSignOutConfirmed()

    private fun signIn(block: suspend () -> Account) = launchAuth {
        val account = block()
        val fromGuest = session.activeAccount.isGuest
        session.signIn(account, keepGuestData = fromGuest && _uiState.value.keepGuestData)
        // Only reached when the account was already active; otherwise the app has restarted.
        _uiState.update { it.copy(busy = false, password = "", message = "Signed in as ${account.displayName}.") }
    }

    private fun launchAuth(block: suspend () -> Unit) {
        _uiState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: AuthException) {
                onError(e)
            }
        }
    }

    private fun otherAccounts() = session.accounts().filterNot { it.isGuest || it.id == session.activeAccount.id }
}
