package com.eleazar.localhive.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DoorFront
import androidx.compose.material.icons.outlined.Emergency
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.EmergencyType
import com.eleazar.localhive.domain.model.IncidentStatus
import com.eleazar.localhive.domain.model.Post
import com.eleazar.localhive.ui.theme.AlertRed
import com.eleazar.localhive.ui.theme.HiveGreen
import com.eleazar.localhive.ui.theme.HoneyInk
import com.eleazar.localhive.ui.util.relativeTime

val EmergencyType.icon: ImageVector
    get() = when (this) {
        EmergencyType.BREAK_IN -> Icons.Outlined.DoorFront
        EmergencyType.FIRE -> Icons.Outlined.LocalFireDepartment
        EmergencyType.MEDICAL -> Icons.Outlined.MedicalServices
        EmergencyType.OTHER -> Icons.Outlined.ReportProblem
    }

/** Each kind its own colour, so the right tile can be found at a glance. */
val EmergencyType.color: Color
    get() = when (this) {
        EmergencyType.BREAK_IN -> Color(0xFF4A3F9F)
        EmergencyType.FIRE -> Color(0xFFD84315)
        EmergencyType.MEDICAL -> AlertRed
        EmergencyType.OTHER -> HoneyInk
    }

/** A few words on what each kind covers, under its name on the SOS screen. */
val EmergencyType.hint: String
    get() = when (this) {
        EmergencyType.BREAK_IN -> "Intruder or theft"
        EmergencyType.FIRE -> "Smoke or flames"
        EmergencyType.MEDICAL -> "Injury or illness"
        EmergencyType.OTHER -> "Something else"
    }

/** "Break-in emergency", or just "Emergency" when the type isn't known. */
val Post.emergencyTitle: String
    get() = EmergencyType.fromKey(emergencyType)?.takeIf { it != EmergencyType.OTHER }
        ?.let { "${it.label} emergency" } ?: "Emergency"

/** "Ada is on the way", "Ada and Bo are on the way", "Ada, Bo and 3 others are on the way". */
fun respondersLine(responders: Map<String, String>): String? {
    val names = responders.values.map { it.ifBlank { "A neighbour" } }
    return when (names.size) {
        0 -> null
        1 -> "${names[0]} is on the way"
        2 -> "${names[0]} and ${names[1]} are on the way"
        3 -> "${names[0]}, ${names[1]} and ${names[2]} are on the way"
        else -> "${names[0]}, ${names[1]} and ${names.size - 2} others are on the way"
    }
}

private val IncidentStatus.color: Color
    get() = when (this) {
        IncidentStatus.OPEN -> AlertRed
        IncidentStatus.HANDLING -> HoneyInk
        IncidentStatus.RESOLVED -> HiveGreen
    }

/** Lightens [color] in dark mode so it still reads on a dark background. */
@Composable
fun readable(color: Color): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (dark) Color(
        red = color.red + (1f - color.red) * 0.35f,
        green = color.green + (1f - color.green) * 0.35f,
        blue = color.blue + (1f - color.blue) * 0.35f
    ) else color
}

/** A small pill saying where a security report or emergency stands. */
@Composable
fun IncidentStatusChip(status: IncidentStatus, modifier: Modifier = Modifier) {
    val tint = readable(status.color)
    Surface(shape = RoundedCornerShape(50), color = tint.copy(alpha = 0.14f), modifier = modifier) {
        Text(
            status.label,
            color = tint,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/** A security report or emergency's location and status, shown under its category. */
@Composable
fun IncidentSummary(post: Post, modifier: Modifier = Modifier) {
    val status = IncidentStatus.fromKey(post.incidentStatus) ?: return
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IncidentStatusChip(status)
            if (post.location.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Outlined.LocationOn, contentDescription = "Location", tint = muted, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(2.dp))
                Text(
                    post.location,
                    fontSize = 13.sp,
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (status != IncidentStatus.RESOLVED) respondersLine(post.responders)?.let { line ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = readable(HiveGreen), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(line, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = readable(HiveGreen))
            }
        }
        if (post.statusUpdatedByName.isNotBlank() && status != IncidentStatus.OPEN) {
            val time = relativeTime(post.statusUpdatedAt)
            Text(
                "Marked ${status.label.lowercase()} by ${post.statusUpdatedByName}" + if (time.isNotEmpty()) " · $time" else "",
                fontSize = 12.sp,
                color = muted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/** A live emergency, pinned above the feed until it's resolved. */
@Composable
fun EmergencyBanner(post: Post, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val type = EmergencyType.fromKey(post.emergencyType)
    val status = IncidentStatus.fromKey(post.incidentStatus)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = AlertRed,
        contentColor = Color.White,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(type?.icon ?: Icons.Outlined.Emergency, contentDescription = null, modifier = Modifier.size(26.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    post.emergencyTitle + if (post.location.isNotBlank()) " · ${post.location}" else "",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val time = relativeTime(post.createdAt)
                Text(
                    buildString {
                        append(post.authorName.ifBlank { "A neighbour" })
                        if (time.isNotEmpty()) append(" · $time")
                        if (status == IncidentStatus.HANDLING) append(" · being handled")
                        if (post.responders.isNotEmpty()) append(" · ${post.responders.size} on the way")
                    },
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open")
        }
    }
}
