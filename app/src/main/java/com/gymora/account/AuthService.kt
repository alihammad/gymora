package com.gymora.account

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/** Signs people in to their cloud account. Knows nothing about local data; see [AccountSession]. */
interface AuthService {

    /** The signed-in cloud user, or null. Survives restarts and works offline. */
    val currentUserId: String?

    suspend fun signInWithEmail(email: String, password: String): Account

    suspend fun createAccount(name: String, email: String, password: String): Account

    suspend fun signInWithGoogle(idToken: String): Account

    suspend fun sendPasswordReset(email: String)

    fun signOut()

    /** Permanently deletes the cloud account. Throws [AuthException.RecentLoginRequired] if the login is old. */
    suspend fun deleteCurrentUser()
}

/** A failure with a message that can be shown to the user as is. */
sealed class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidCredentials(cause: Throwable? = null) :
        AuthException("That email and password don't match.", cause)
    class EmailInUse(cause: Throwable? = null) :
        AuthException("An account with that email already exists. Sign in instead.", cause)
    class WeakPassword(cause: Throwable? = null) :
        AuthException("Choose a password with at least 6 characters.", cause)
    class InvalidEmail(cause: Throwable? = null) :
        AuthException("That doesn't look like an email address.", cause)
    class Network(cause: Throwable? = null) :
        AuthException("No connection. Signing in needs the internet the first time.", cause)
    class TooManyAttempts(cause: Throwable? = null) :
        AuthException("Too many attempts. Wait a few minutes and try again.", cause)
    class RecentLoginRequired(cause: Throwable? = null) :
        AuthException("For your security, sign out and sign in again, then delete the account.", cause)
    class NoGoogleAccount(cause: Throwable? = null) :
        AuthException("No Google account on this phone. Add one in system Settings, or use email.", cause)
    class Failed(cause: Throwable? = null) :
        AuthException("Something went wrong. Please try again.", cause)
}

@Singleton
class FirebaseAuthService @Inject constructor() : AuthService {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    override val currentUserId: String? get() = auth.currentUser?.uid

    override suspend fun signInWithEmail(email: String, password: String): Account = mapErrors {
        auth.signInWithEmailAndPassword(email.trim(), password).await().user.toAccount()
    }

    override suspend fun createAccount(name: String, email: String, password: String): Account = mapErrors {
        val user = requireNotNull(auth.createUserWithEmailAndPassword(email.trim(), password).await().user)
        if (name.isNotBlank()) user.updateProfile(userProfileChangeRequest { displayName = name.trim() }).await()
        // Not required to use the app; it lets people recover the account later.
        runCatching { user.sendEmailVerification().await() }
        user.toAccount()
    }

    override suspend fun signInWithGoogle(idToken: String): Account = mapErrors {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await().user.toAccount()
    }

    override suspend fun sendPasswordReset(email: String) = mapErrors {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    override fun signOut() = auth.signOut()

    override suspend fun deleteCurrentUser() = mapErrors {
        auth.currentUser?.delete()?.await()
        Unit
    }

    private fun FirebaseUser?.toAccount(): Account {
        val user = requireNotNull(this) { "Signed in without a user" }
        return Account(
            id = user.uid,
            displayName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email.orEmpty(),
            email = user.email,
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> mapErrors(block: suspend () -> T): T = try {
        block()
    } catch (e: AuthException) {
        throw e
    } catch (e: Exception) {
        throw when (e) {
            is FirebaseAuthRecentLoginRequiredException -> AuthException.RecentLoginRequired(e)
            is FirebaseAuthWeakPasswordException -> AuthException.WeakPassword(e)
            is FirebaseAuthUserCollisionException -> AuthException.EmailInUse(e)
            is FirebaseAuthInvalidUserException -> AuthException.InvalidCredentials(e)
            is FirebaseAuthInvalidCredentialsException -> if (e.errorCode == "ERROR_INVALID_EMAIL") {
                AuthException.InvalidEmail(e)
            } else {
                AuthException.InvalidCredentials(e)
            }
            is FirebaseNetworkException -> AuthException.Network(e)
            is FirebaseTooManyRequestsException -> AuthException.TooManyAttempts(e)
            is FirebaseAuthException -> AuthException.Failed(e)
            else -> AuthException.Failed(e)
        }
    }
}
