package com.eleazar.localhive.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.UtilityKind
import com.eleazar.localhive.domain.model.UtilityStatus
import com.eleazar.localhive.ui.finance.DuesSummary
import com.eleazar.localhive.ui.theme.Clay
import com.eleazar.localhive.ui.utilities.headline
import com.eleazar.localhive.ui.utilities.icon
import com.eleazar.localhive.ui.utilities.utilityColor
import com.eleazar.localhive.ui.util.relativeTime

/** Polls open for voting, and how many of them the user still has to vote in. */
data class PollsGlance(val open: Int, val awaitingVote: Int)

/**
 * The estate at a glance, above the feed: light, water, polls and dues, each a tap from its own
 * screen. Tiles that want something from the user are tinted so they stand out.
 */
@Composable
fun EstateGlance(
    utilities: Map<UtilityKind, UtilityStatus>,
    polls: PollsGlance,
    dues: DuesSummary?,
    onUtilities: () -> Unit,
    onPolls: () -> Unit,
    onDues: () -> Unit,
    modifier: Modifier = Modifier
) {
    val power = utilities[UtilityKind.POWER]
    val water = utilities[UtilityKind.WATER]
    val primary = MaterialTheme.colorScheme.primary
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
            GlanceTile(
                icon = UtilityKind.POWER.icon(power?.isOn),
                tint = utilityColor(power?.isOn),
                label = "Light",
                value = power?.headline() ?: "No reports",
                detail = power?.let { "${it.updatedByName.ifBlank { "A neighbour" }} · ${relativeTime(it.updatedAt)}" } ?: "Tap to report",
                onClick = onUtilities,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GlanceTile(
                icon = UtilityKind.WATER.icon(water?.isOn),
                tint = utilityColor(water?.isOn),
                label = "Water",
                value = water?.headline() ?: "No reports",
                detail = water?.let { "${it.updatedByName.ifBlank { "A neighbour" }} · ${relativeTime(it.updatedAt)}" } ?: "Tap to report",
                onClick = onUtilities,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
            GlanceTile(
                icon = Icons.Outlined.HowToVote,
                tint = primary,
                label = "Polls",
                value = when {
                    polls.awaitingVote == 1 -> "1 to vote on"
                    polls.awaitingVote > 1 -> "${polls.awaitingVote} to vote on"
                    polls.open > 0 -> "You've voted"
                    else -> "None open"
                },
                detail = if (polls.open > 0) "${polls.open} open" else "See past results",
                attention = polls.awaitingVote > 0,
                onClick = onPolls,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            GlanceTile(
                icon = Icons.Outlined.AccountBalanceWallet,
                tint = if (dues?.needsAttention == true) readable(Clay) else primary,
                label = "Dues",
                value = dues?.headline ?: "…",
                detail = dues?.detail ?: "Loading",
                attention = dues?.needsAttention == true,
                onClick = onDues,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun GlanceTile(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String,
    detail: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    attention: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (attention) tint.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (attention) tint.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $value. $detail" }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = tint.copy(alpha = 0.14f), modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                if (attention) Surface(shape = CircleShape, color = tint, modifier = Modifier.size(8.dp)) {}
            }
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (attention) tint else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                detail,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
