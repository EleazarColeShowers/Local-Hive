package com.eleazar.localhive.ui.blocking

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

/** Confirms blocking or unblocking [name]; [blocking] picks which. */
@Composable
fun BlockConfirmDialog(
    name: String,
    blocking: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(if (blocking) "Block $name?" else "Unblock $name?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                if (blocking) {
                    "You won't see their posts or comments, and neither of you can message the other. " +
                        "They won't be told. You can unblock them any time in Settings."
                } else {
                    "Their posts and comments will show again, and you'll be able to message each other."
                }
            )
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm() }) {
                Text(
                    if (blocking) "Block" else "Unblock",
                    color = if (blocking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
