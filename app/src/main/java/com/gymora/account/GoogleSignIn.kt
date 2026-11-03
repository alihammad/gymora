package com.gymora.account

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.gymora.R

/**
 * Shows the system "Sign in with Google" sheet and returns a Google ID token for
 * [AuthService.signInWithGoogle]. Needs an Activity context so the sheet can attach to it.
 */
object GoogleSignIn {

    private const val TAG = "GoogleSignIn"

    /** The token, or null if the user closed the sheet. Throws [AuthException] on failure. */
    suspend fun requestIdToken(activityContext: Context): String? {
        val option = GetGoogleIdOption.Builder()
            // Offer every Google account on the phone, not only ones used here before.
            .setFilterByAuthorizedAccounts(false)
            // Generated from google-services.json by the Google Services plugin.
            .setServerClientId(activityContext.getString(R.string.default_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = try {
            CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        } catch (_: GetCredentialCancellationException) {
            return null
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Google sign-in failed", e)
            throw e.toAuthException()
        }
        val isGoogleToken = credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        if (!isGoogleToken) throw AuthException.Failed()
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    private fun GetCredentialException.toAuthException(): AuthException = when (this) {
        is NoCredentialException -> AuthException.NoGoogleAccount(this)
        else -> AuthException.Failed(this)
    }
}
