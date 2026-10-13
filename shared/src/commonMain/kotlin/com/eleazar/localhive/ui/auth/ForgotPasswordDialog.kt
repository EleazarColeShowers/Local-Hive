package com.eleazar.localhive.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eleazar.localhive.ui.components.LocalHiveTextField
import com.eleazar.localhive.ui.util.isValidEmail

sealed interface PasswordResetState {
    data object Idle : PasswordResetState
    data object Sending : PasswordResetState
    data class Sent(val email: String) : PasswordResetState
    data class Failed(val message: String) : PasswordResetState
}

/** Asks for an email and sends Firebase's reset link to it; shown from the login screen. */
@Composable
fun ForgotPasswordDialog(
    initialEmail: String,
    state: PasswordResetState,
    onSend: (email: String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (state is PasswordResetState.Sent) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Check your email", fontWeight = FontWeight.Bold) },
            text = {
                // Firebase doesn't reveal whether an address has an account, so neither do we.
                Text(
                    "If ${state.email} has a LocalHive account, a link to reset your password is on its way. " +
                        "It can take a few minutes, and may land in spam."
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Back to login", fontWeight = FontWeight.Bold) }
            }
        )
        return
    }

    var email by rememberSaveable { mutableStateOf(initialEmail) }
    val sending = state is PasswordResetState.Sending
    val canSend = isValidEmail(email.trim()) && !sending
    val send = { if (canSend) onSend(email.trim()) }

    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Reset your password", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter the email you signed up with and we'll send you a link to choose a new password.")
                Spacer(Modifier.height(16.dp))
                LocalHiveTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    placeholder = "you@example.com",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    isError = state is PasswordResetState.Failed,
                    supportingText = (state as? PasswordResetState.Failed)?.message
                )
            }
        },
        confirmButton = {
            TextButton(onClick = send, enabled = canSend) {
                if (sending) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Send link", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !sending) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    )
}
