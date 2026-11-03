package com.gymora.ui.account

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.account.Account
import com.gymora.ui.components.Button
import com.gymora.ui.components.ConfirmDialog
import com.gymora.ui.components.OutlinedButton
import com.gymora.ui.components.TextButton

/**
 * Who is signed in, plus sign-in, sign-out, switching and account deletion.
 * [onSignIn] opens the sign-in screen, with an email to prefill when switching.
 */
@Composable
fun AccountSection(
    onSignIn: (email: String?) -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val active = state.active

    LaunchedEffect(state.message, state.error) {
        (state.error ?: state.message)?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.onMessageShown()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (active.isGuest) {
            Text(
                text = "You're using the app as a guest. Your data is stored only on this phone.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = { onSignIn(null) }, modifier = Modifier.fillMaxWidth()) {
                Text("Sign in or create account")
            }
        } else {
            Text(active.displayName, style = MaterialTheme.typography.titleMedium)
            active.email?.takeIf { it != active.displayName }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = viewModel::onSignOutClicked, enabled = !state.busy) { Text("Sign out") }
                TextButton(onClick = viewModel::onDeleteClicked, enabled = !state.busy) {
                    Text("Delete account", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (state.otherAccounts.isNotEmpty() || !active.isGuest) {
            Text(
                "Other profiles on this phone",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            state.otherAccounts.forEach { account ->
                OtherAccountRow(
                    account = account,
                    onSwitch = { onSignIn(account.email) },
                    onRemove = { viewModel.onRemoveClicked(account) },
                )
            }
            if (!active.isGuest) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Guest", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = viewModel::onUseAsGuest) { Text("Switch") }
                }
                TextButton(onClick = { onSignIn(null) }) { Text("Add another account") }
            }
        }
    }

    if (state.confirmSignOut) {
        ConfirmDialog(
            title = "Sign out?",
            message = "Your data stays on this phone, and you'll see it again when you sign back in. " +
                "The app switches to the guest profile.",
            confirmLabel = "Sign out",
            dismissLabel = "Cancel",
            onConfirm = viewModel::onSignOutConfirmed,
            onDismiss = viewModel::onDialogDismissed,
        )
    }
    if (state.confirmDelete) {
        ConfirmDialog(
            title = "Delete your account?",
            message = "This permanently deletes your account and every workout, routine, measurement " +
                "and setting stored with it on this phone. This can't be undone. " +
                "Export a backup first if you want to keep anything.",
            confirmLabel = "Delete forever",
            dismissLabel = "Cancel",
            onConfirm = viewModel::onDeleteConfirmed,
            onDismiss = viewModel::onDialogDismissed,
        )
    }
    state.confirmRemove?.let { account ->
        ConfirmDialog(
            title = "Remove ${account.displayName} from this phone?",
            message = "Their workouts, routines and settings on this phone are deleted. " +
                "Data isn't synced to the cloud yet, so this can't be undone.",
            confirmLabel = "Remove",
            dismissLabel = "Cancel",
            onConfirm = viewModel::onRemoveConfirmed,
            onDismiss = viewModel::onDialogDismissed,
        )
    }
}

@Composable
private fun OtherAccountRow(account: Account, onSwitch: () -> Unit, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(account.displayName, style = MaterialTheme.typography.bodyMedium)
            account.email?.takeIf { it != account.displayName }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
        TextButton(onClick = onSwitch) { Text("Switch") }
        TextButton(onClick = onRemove) { Text("Remove", color = MaterialTheme.colorScheme.error) }
    }
}
