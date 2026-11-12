package com.eleazar.localhive.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsPaused
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.SosSound
import com.eleazar.localhive.ui.util.clockTime
import com.eleazar.localhive.ui.util.nowMillis
import com.eleazar.localhive.ui.util.startOfDay

private const val HOUR_MILLIS = 60 * 60 * 1000L

/** 7 AM today if it's still before then, otherwise 7 AM tomorrow: the end of a night's pause. */
internal fun nextMorning(now: Long): Long {
    val today = startOfDay(now) + 7 * HOUR_MILLIS
    return if (now < today) today else today + 24 * HOUR_MILLIS
}

/** "Paused until 3:45 PM", or "Paused until tomorrow, 7:00 AM". */
internal fun pausedLabel(until: Long, now: Long): String {
    val day = if (startOfDay(until) == startOfDay(now)) "" else "tomorrow, "
    return "Paused until $day${clockTime(until)}"
}

/**
 * How a neighbour's SOS sounds on this phone, and a pause for meetings and nights when it only
 * vibrates. Alerts always still arrive: nobody can turn SOS off for themselves completely.
 */
@Composable
internal fun SosSettingsRows(
    sound: SosSound,
    onSoundChange: (SosSound) -> Unit,
    pausedUntil: Long,
    onPause: (until: Long) -> Unit,
    darkGray: Color,
    hiveGreen: Color
) {
    val now = remember(pausedUntil) { nowMillis() }
    val paused = pausedUntil > now
    var choosingPause by remember { mutableStateOf(false) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Text(
        "A neighbour's emergency always reaches you. Choose how loud it is.",
        fontSize = 12.sp,
        color = muted,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
    )
    SosSound.entries.forEach { option ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSoundChange(option) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(option.label, fontSize = 14.sp, color = darkGray)
                Text(option.description, fontSize = 12.sp, color = muted)
            }
            RadioButton(
                selected = sound == option,
                onClick = { onSoundChange(option) },
                colors = RadioButtonDefaults.colors(selectedColor = hiveGreen)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 16.dp))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { choosingPause = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.NotificationsPaused, null, tint = muted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(if (paused) pausedLabel(pausedUntil, now) else "Pause SOS sound", fontSize = 14.sp, color = darkGray)
            Text(
                if (paused) "Alerts still arrive, but only vibrate" else "For a meeting or the night: alerts only vibrate",
                fontSize = 12.sp,
                color = muted
            )
        }
        if (paused) {
            TextButton(onClick = { onPause(0L) }) {
                Text("Resume", color = hiveGreen, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (choosingPause) {
        val options = listOf(
            "For 1 hour" to now + HOUR_MILLIS,
            "For 3 hours" to now + 3 * HOUR_MILLIS,
            "Until 7 AM" to nextMorning(now)
        )
        AlertDialog(
            onDismissRequest = { choosingPause = false },
            title = { Text("Pause SOS sound") },
            text = {
                Column {
                    Text(
                        "Emergencies still show up and vibrate, so you'll know. The sound comes back by itself.",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    options.forEach { (label, until) ->
                        TextButton(
                            onClick = {
                                choosingPause = false
                                onPause(until)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, color = hiveGreen, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { choosingPause = false }) { Text("Cancel") } }
        )
    }
}
