package com.eleazar.localhive.ui.utilities

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.UtilityKind
import com.eleazar.localhive.domain.model.UtilityStatus
import com.eleazar.localhive.ui.components.ScreenGutter
import com.eleazar.localhive.ui.components.readable
import com.eleazar.localhive.ui.theme.Clay
import com.eleazar.localhive.ui.theme.HiveGreen
import com.eleazar.localhive.ui.util.nowMillis
import com.eleazar.localhive.ui.util.relativeTime

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

fun UtilityKind.icon(isOn: Boolean?): ImageVector = when (this) {
    UtilityKind.POWER -> if (isOn == false) Icons.Filled.FlashOff else Icons.Filled.Bolt
}

/** Green for on, clay for off, grey when nobody has said. */
@Composable
fun utilityColor(isOn: Boolean?): Color = when (isOn) {
    true -> readable(HiveGreen)
    false -> readable(Clay)
    null -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** "On for 2h", "Off for 3d"; the length is how long since it last changed. */
fun UtilityStatus.headline(now: Long = nowMillis()): String {
    val state = if (isOn) "On" else "Off"
    val age = relativeAge(since, now)
    return if (age.isEmpty()) state else "$state for $age"
}

private fun relativeAge(since: Long, now: Long): String {
    if (since <= 0L) return ""
    val diff = (now - since).coerceAtLeast(0L)
    val minutes = diff / 60_000L
    return when {
        minutes < 1 -> "a moment"
        minutes < 60 -> "${minutes}m"
        minutes < 48 * 60 -> "${minutes / 60}h"
        else -> "${minutes / (24 * 60)}d"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilitiesScreen(
    uiState: UtilitiesUiState,
    onReport: (UtilityKind, isOn: Boolean) -> Unit,
    onBack: () -> Unit,
    onLightAlertsChange: (Boolean) -> Unit = {}
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Light", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "intro") {
                Text(
                    "Say what you can see right now. Everyone in the estate sees it straight away.",
                    fontSize = 14.sp,
                    color = muted
                )
            }
            uiState.error?.let { error ->
                item(key = "error") { Text(error, fontSize = 14.sp, color = MaterialTheme.colorScheme.error) }
            }
            item(key = "alerts") {
                // Right where people wait for the light, so they find it without digging in Settings.
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = readable(HiveGreen).copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = readable(HiveGreen))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tell me when the light is back", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("A notification as soon as a neighbour reports it", fontSize = 12.sp, color = muted)
                        }
                        Switch(checked = uiState.lightAlerts, onCheckedChange = onLightAlertsChange)
                    }
                }
            }
            items(UtilityKind.entries, key = { it.key }) { kind ->
                UtilityCard(
                    kind = kind,
                    status = uiState.statuses[kind],
                    lastDay = uiState.lastDay[kind],
                    reporting = kind in uiState.reporting,
                    onReport = { isOn -> onReport(kind, isOn) }
                )
            }
            if (uiState.reports.isNotEmpty()) {
                item(key = "history") {
                    Text(
                        "Recent reports",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                items(uiState.reports.take(30), key = { it.id }) { report ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            report.kind.icon(report.isOn),
                            contentDescription = null,
                            tint = utilityColor(report.isOn),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${report.kind.label} ${if (report.isOn) "on" else "off"}",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            " · ${report.byName.ifBlank { "A neighbour" }}",
                            fontSize = 14.sp,
                            color = muted,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Text(relativeTime(report.at), fontSize = 13.sp, color = muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun UtilityCard(
    kind: UtilityKind,
    status: UtilityStatus?,
    lastDay: Uptime?,
    reporting: Boolean,
    onReport: (isOn: Boolean) -> Unit
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val tint = utilityColor(status?.isOn)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = tint.copy(alpha = 0.14f), modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(kind.icon(status?.isOn), contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(kind.label, fontSize = 14.sp, color = muted)
                    Text(
                        status?.headline() ?: "No reports yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = if (status == null) muted else tint
                    )
                    if (status != null) {
                        Text(
                            "Last confirmed by ${status.updatedByName.ifBlank { "a neighbour" }} · ${relativeTime(status.updatedAt)}",
                            fontSize = 12.sp,
                            color = muted
                        )
                    }
                }
            }
            lastDay?.let { uptime ->
                Spacer(Modifier.height(12.dp))
                val span = if (uptime.knownMillis >= DAY_MILLIS - 60_000L) "the last 24 hours"
                else "the last ${shortDuration(uptime.knownMillis)} reported"
                Text(
                    "On for ${shortDuration(uptime.onMillis)} of $span",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (uptime.knownMillis > 0) uptime.onMillis.toFloat() / uptime.knownMillis else 0f },
                    color = readable(HiveGreen),
                    trackColor = readable(Clay).copy(alpha = 0.25f),
                    drawStopIndicator = {},
                    modifier = Modifier.fillMaxWidth().height(6.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReportButton(
                    label = "${kind.label} is on",
                    color = readable(HiveGreen),
                    enabled = !reporting,
                    onClick = { onReport(true) },
                    modifier = Modifier.weight(1f)
                )
                ReportButton(
                    label = "${kind.label} is off",
                    color = readable(Clay),
                    enabled = !reporting,
                    onClick = { onReport(false) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ReportButton(label: String, color: Color, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = if (enabled) 0.6f else 0.2f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        modifier = modifier.height(44.dp)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
