package com.eleazar.localhive.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import com.eleazar.localhive.ui.components.RoleBadge
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// ───────────────────────── View mode ─────────────────────────

@Composable
internal fun ProfileViewContent(
    user: ProfileUserUi?,
    estate: ProfileEstateUi?,
    isAdmin: Boolean,
    colors: ProfileColors,
    onEdit: () -> Unit,
    onShowInviteCode: () -> Unit,
    onAdminPanel: (String) -> Unit,
    onFinances: () -> Unit,
    onPolls: () -> Unit,
    onUtilities: () -> Unit,
    onVisitors: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = user?.displayName ?: user?.username ?: "Neighbor",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.darkGray,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        user?.username?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = "@$it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        user?.bio?.takeIf { it.isNotBlank() }?.let { bio ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = bio,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.darkGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(40.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text("Edit profile", fontWeight = FontWeight.SemiBold, color = colors.darkGray)
        }

        Spacer(Modifier.height(24.dp))

        if (estate != null) {
            SectionLabel("Your estate", colors)
            SectionCard(colors) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = estate.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.darkGray
                            )
                            if (estate.address.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = colors.darkGray.copy(alpha = 0.45f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = estate.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.darkGray.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (estate.isTreasurer) RoleBadge(treasurer = true)
                            if (isAdmin) RoleBadge(owner = estate.isOwner)
                        }
                    }
                    Text(
                        text = "${estate.memberCount} member${if (estate.memberCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ActionRow(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = "Dues & expenses",
                    subtitle = if (estate.isTreasurer) "Manage levies and log spending" else "What you owe and where the money goes",
                    colors = colors,
                    onClick = onFinances
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 72.dp))
                ActionRow(
                    icon = Icons.Outlined.HowToVote,
                    title = "Polls",
                    subtitle = if (isAdmin) "Ask the estate a question" else "Vote on estate decisions",
                    colors = colors,
                    onClick = onPolls
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 72.dp))
                ActionRow(
                    icon = Icons.Outlined.Bolt,
                    title = "Light",
                    subtitle = "Is there light? See and report it",
                    colors = colors,
                    onClick = onUtilities
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 72.dp))
                ActionRow(
                    icon = Icons.Outlined.Badge,
                    title = "Visitors",
                    subtitle = "Gate passes for people you're expecting",
                    colors = colors,
                    onClick = onVisitors
                )
                if (isAdmin) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    if (estate.acceptsCodes) {
                        ActionRow(
                            icon = Icons.Default.Share,
                            title = "Share invite code",
                            subtitle = "Let neighbours join ${estate.name}",
                            colors = colors,
                            onClick = onShowInviteCode
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(start = 72.dp)
                        )
                    }
                    ActionRow(
                        icon = Icons.Default.Lock,
                        title = "Admin panel",
                        subtitle = "Manage your estate",
                        colors = colors,
                        onClick = { onAdminPanel(estate.id) }
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        val contactRows = listOfNotNull(
            user?.email?.let { Triple(Icons.Default.Email, "Email", it) },
            user?.phone?.let { Triple(Icons.Default.Phone, "Phone", it) },
            user?.address?.let { Triple(Icons.Default.Home, "Address", it) },
            user?.occupation?.takeIf { it.isNotBlank() }?.let { Triple(Icons.Default.AccountBox, "Occupation", it) }
        )
        if (contactRows.isNotEmpty()) {
            SectionLabel("About", colors)
            SectionCard(colors) {
                contactRows.forEachIndexed { index, (icon, label, value) ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(start = 72.dp)
                        )
                    }
                    InfoRow(icon, label, value, colors)
                }
            }
        }
    }
}
