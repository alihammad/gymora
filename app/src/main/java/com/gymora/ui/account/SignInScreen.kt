package com.gymora.ui.account

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymora.account.AuthException
import com.gymora.account.GoogleSignIn
import com.gymora.ui.components.Button
import com.gymora.ui.components.OutlinedButton
import com.gymora.ui.components.OutlinedTextField
import com.gymora.ui.components.TextButton
import com.gymora.ui.components.TopAppBar
import kotlinx.coroutines.launch

/** Sign in or create an account, with Google or email and password. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.creating) "Create account" else "Sign in") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Your workouts, routines and settings stay with your account, " +
                    "separate from anyone else who uses this phone.",
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedButton(
                onClick = {
                    scope.launch {
                        try {
                            viewModel.onGoogleToken(GoogleSignIn.requestIdToken(context))
                        } catch (e: AuthException) {
                            viewModel.onError(e)
                        }
                    }
                },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Continue with Google") }

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text("or", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 12.dp))
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            if (state.creating) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChanged,
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChanged,
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChanged,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.onSubmit() }),
                supportingText = if (state.creating) ({ Text("At least 6 characters") }) else null,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.active.isGuest) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onKeepGuestDataChanged(!state.keepGuestData) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = state.keepGuestData, onCheckedChange = viewModel::onKeepGuestDataChanged)
                    Text(
                        text = "Move what I logged as a guest into this account " +
                            "(only if the account has no data on this phone yet)",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = viewModel::onSubmit,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        state.busy -> "Please wait…"
                        state.creating -> "Create account"
                        else -> "Sign in"
                    },
                )
            }

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = viewModel::onModeToggled, enabled = !state.busy) {
                    Text(if (state.creating) "I have an account" else "Create an account")
                }
                if (!state.creating) {
                    TextButton(onClick = viewModel::onForgotPassword, enabled = !state.busy) {
                        Text("Forgot password?")
                    }
                }
            }
        }
    }
}
