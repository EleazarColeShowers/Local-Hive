package com.eleazar.localhive.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eleazar.localhive.domain.model.Estate

/** What leaving means for this particular person, which changes what the dialog has to say. */
enum class LeaveRole {
    MEMBER,
    ADMIN,
    /** Owner, and another admin exists to take ownership. */
    OWNER_HANDS_OVER,
    /** Owner and the only person left; the estate stays theirs to come back to. */
    OWNER_LAST,
    /** Owner with other members but no other admin: must promote someone first. */
    OWNER_BLOCKED;

    companion object {
        fun of(estate: Estate, userId: String?): LeaveRole = when {
            !estate.isOwner(userId) -> if (estate.isAdmin(userId)) ADMIN else MEMBER
            estate.adminIds.any { it != userId } -> OWNER_HANDS_OVER
            estate.memberCount <= 1 -> OWNER_LAST
            else -> OWNER_BLOCKED
        }
    }
}

@Composable
fun LeaveEstateDialog(
    estateName: String,
    role: LeaveRole,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onOpenAdminPanel: (() -> Unit)? = null
) {
    if (role == LeaveRole.OWNER_BLOCKED) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Choose another admin first", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "You're the owner of $estateName and its only admin. Make another member an admin " +
                        "so the estate isn't left without anyone to run it, then you can leave."
                )
            },
            confirmButton = {
                if (onOpenAdminPanel != null) {
                    TextButton(onClick = { onDismiss(); onOpenAdminPanel() }) {
                        Text("Open admin panel", fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(onClick = onDismiss) { Text("OK", fontWeight = FontWeight.Bold) }
                }
            },
            dismissButton = if (onOpenAdminPanel != null) {
                { TextButton(onClick = onDismiss) { Text("Not now") } }
            } else {
                null
            }
        )
        return
    }

    val extra = when (role) {
        LeaveRole.ADMIN -> "You'll also stop being an admin."
        LeaveRole.OWNER_HANDS_OVER -> "As the owner, ownership passes to one of the other admins."
        LeaveRole.OWNER_LAST -> "You're the last member, so the estate will be empty until you or someone else rejoins."
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Leave $estateName?", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "You'll lose access to the feed, the directory and your neighbours' posts. " +
                        "To come back you'll have to request to join again and wait for an admin to approve you, " +
                        "or use a new invite code."
                )
                if (extra != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(extra, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm() }) {
                Text("Leave estate", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Stay") } }
    )
}
