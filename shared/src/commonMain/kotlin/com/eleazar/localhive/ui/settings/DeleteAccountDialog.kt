package com.eleazar.localhive.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.eleazar.localhive.ui.components.LeaveRole
import com.eleazar.localhive.ui.components.LocalHiveTextField

/**
 * Asks for the password before deleting the account for good. An estate owner who would leave
 * members without an admin is told to hand over first instead, since the server refuses anyway.
 */
@Composable
fun DeleteAccountDialog(
    estateName: String?,
    leaveRole: LeaveRole,
    isDeleting: Boolean,
    error: String?,
    onDelete: (password: String) -> Unit,
    onDismiss: () -> Unit,
    onPasswordChanged: () -> Unit = {}
) {
    if (estateName != null && leaveRole == LeaveRole.OWNER_BLOCKED) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Choose another admin first", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "You own $estateName and you're its only admin. Make another member an admin from the " +
                        "admin panel, so the estate isn't left without anyone to run it. Then you can delete your account."
                )
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK", fontWeight = FontWeight.Bold) } }
        )
        return
    }

    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Delete your account?", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    buildString {
                        append("This can't be undone. Your profile, posts, comments, likes and photos will be deleted")
                        if (estateName != null) append(", and you'll leave $estateName")
                        append(". People you've messaged keep the conversation, shown as from a deleted user.")
                    }
                )
                if (leaveRole == LeaveRole.OWNER_HANDS_OVER && estateName != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("Ownership of $estateName passes to another admin.", fontWeight = FontWeight.SemiBold)
                }
                if (leaveRole == LeaveRole.OWNER_LAST && estateName != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("You're its last member, so $estateName will be deleted too.", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(16.dp))
                LocalHiveTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        onPasswordChanged()
                    },
                    label = "Your password",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (visible) "Hide password" else "Show password"
                            )
                        }
                    },
                    isError = error != null,
                    supportingText = error
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onDelete(password) }, enabled = password.isNotEmpty() && !isDeleting) {
                if (isDeleting) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Delete forever", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) { Text("Cancel") }
        }
    )
}
