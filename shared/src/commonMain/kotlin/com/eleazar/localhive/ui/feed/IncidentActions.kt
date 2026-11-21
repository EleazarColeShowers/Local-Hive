package com.eleazar.localhive.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.IncidentStatus
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.CheckCircle
import com.eleazar.localhive.ui.components.readable
import com.eleazar.localhive.ui.theme.AlertRed
import com.eleazar.localhive.ui.theme.HiveGreen
import com.eleazar.localhive.ui.components.ScreenGutter

@Composable
internal fun IncidentStatusPicker(current: IncidentStatus?, onPick: (IncidentStatus) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = ScreenGutter, vertical = 12.dp)) {
        Text(
            "Update status",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IncidentStatus.entries.forEach { status ->
                FilterChip(
                    selected = status == current,
                    onClick = { if (status != current) onPick(status) },
                    label = { Text(status.label) }
                )
            }
        }
    }
}

/** Marks an emergency a false alarm, after saying what that means, or (for admins) takes it back. */
@Composable
internal fun FalseAlarmAction(
    marked: Boolean,
    isMine: Boolean,
    authorName: String,
    onMark: () -> Unit,
    onUndo: () -> Unit
) {
    var confirming by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (marked) "Marked as a false alarm" else "Wasn't a real emergency?",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = { confirming = true }) {
            Text(
                if (marked) "Undo" else "Mark as false alarm",
                color = if (marked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
    if (confirming) {
        val (title, body) = when {
            marked -> "Not a false alarm?" to
                "The false-alarm mark comes off, and it stops counting against $authorName."
            isMine -> "Mark your alert as a false alarm?" to
                "It closes, everyone is told no help is needed, and it shows as a false alarm. " +
                "Two false alarms in 30 days and your alerts go out without the siren."
            else -> "Mark this as a false alarm?" to
                "It closes, everyone is told no help is needed, and it shows as a false alarm. " +
                "Two false alarms in 30 days and $authorName's alerts go out without the siren."
        }
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = {
                TextButton(onClick = { confirming = false; if (marked) onUndo() else onMark() }) {
                    Text(
                        if (marked) "Undo" else "Mark false alarm",
                        color = if (marked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancel") } }
        )
    }
}

/**
 * The two answers an SOS needs: neighbours saying they're on their way, and the person who
 * raised it saying they're safe.
 */
@Composable
internal fun SosFollowUp(
    isMine: Boolean,
    authorName: String,
    responding: Boolean,
    onRespond: () -> Unit,
    onMarkSafe: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = ScreenGutter, vertical = 12.dp)) {
        when {
            isMine -> {
                Button(
                    onClick = onMarkSafe,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = HiveGreen, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("I'm safe now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text(
                    "Closes the alert and lets everyone know you're OK.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            responding -> {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = HiveGreen.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = readable(HiveGreen))
                        Spacer(Modifier.width(8.dp))
                        Text("You're on your way", fontWeight = FontWeight.Bold, color = readable(HiveGreen))
                    }
                }
                Text(
                    "$authorName knows you're coming.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            else -> {
                Button(
                    onClick = onRespond,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("I'm coming to help", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text(
                    "$authorName will be told you're on your way.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
