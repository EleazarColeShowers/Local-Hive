package com.eleazar.localhive.ui.estate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.eleazar.localhive.ui.components.Avatar
import com.eleazar.localhive.ui.components.RoleBadge
import com.eleazar.localhive.domain.model.User
import com.eleazar.localhive.ui.util.nowMillis
import com.eleazar.localhive.domain.model.SosStanding

@Composable
internal fun MembersTab(
    members: List<User>,
    adminIds: List<String>,
    ownerId: String,
    treasurerId: String,
    currentUserId: String,
    onPromote: (String) -> Unit,
    onRemoveAdmin: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onSetTreasurer: (String?) -> Unit,
    onTransferOwnership: (String) -> Unit,
    sosStandings: Map<String, SosStanding>,
    onSetSosDisabled: (String, Boolean) -> Unit,
    onRestoreSiren: (String) -> Unit,
    dark: Color,
    green: Color,
    cardBg: Color
) {
    val now = remember(sosStandings) { nowMillis() }
    if (members.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No members yet", color = dark.copy(alpha = 0.4f), fontSize = 15.sp)
        }
        return
    }

    val viewerIsAdmin = currentUserId in adminIds
    val viewerIsOwner = currentUserId == ownerId
    // Owner first, then admins, then everyone else, each alphabetical.
    val sorted = members.sortedWith(
        compareBy<User>({ it.id != ownerId }, { it.id !in adminIds }, { (it.displayName ?: it.username ?: "").lowercase() })
    )

    val treasurerName = members.firstOrNull { it.id == treasurerId }?.let { it.displayName ?: it.username }

    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Text(
                "Admins can approve requests, post announcements, make others admins and remove members. " +
                    "The owner always stays an admin, and an admin has to lose admin before they can be removed. " +
                    "The owner can hand ownership to any member from that member's menu. " +
                    "The treasurer, who can be anyone, sets dues, confirms payments, logs expenses and can post announcements. " +
                    "Two false alarms in 30 days and a member's SOS alerts go out without the siren; " +
                    "admins can restore it, or turn someone's SOS alerts off altogether.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
        items(sorted, key = { it.id }) { member ->
            val isAdmin = member.id in adminIds
            val isOwner = member.id == ownerId
            val isTreasurer = treasurerId.isNotBlank() && member.id == treasurerId
            val sos = sosStandings[member.id] ?: SosStanding()
            MemberCard(
                member = member,
                role = when {
                    isOwner -> MemberRole.OWNER
                    isAdmin -> MemberRole.ADMIN
                    else -> MemberRole.MEMBER
                },
                isYou = member.id == currentUserId,
                isTreasurer = isTreasurer,
                treasurerName = treasurerName,
                sosNote = when {
                    sos.disabled -> "SOS alerts off"
                    sos.sirenRemoved(now) -> "${sos.recentFalseAlarms(now)} false alarms · alerts without siren"
                    sos.recentFalseAlarms(now) == 1 -> "1 false alarm in the last 30 days"
                    else -> null
                },
                actions = buildList {
                    if (!viewerIsAdmin) return@buildList
                    // Anyone can hold the treasurer role, the admin making the change included.
                    add(if (isTreasurer) MemberAction.REMOVE_TREASURER else MemberAction.MAKE_TREASURER)
                    if (member.id == currentUserId || isOwner) return@buildList
                    if (viewerIsOwner) add(MemberAction.MAKE_OWNER)
                    if (isAdmin) add(MemberAction.REMOVE_ADMIN)
                    else addAll(listOf(MemberAction.MAKE_ADMIN, MemberAction.REMOVE_MEMBER))
                    if (!sos.disabled && sos.sirenRemoved(now)) add(MemberAction.RESTORE_SIREN)
                    add(if (sos.disabled) MemberAction.TURN_ON_SOS else MemberAction.TURN_OFF_SOS)
                },
                onAction = { action ->
                    when (action) {
                        MemberAction.MAKE_ADMIN -> onPromote(member.id)
                        MemberAction.REMOVE_ADMIN -> onRemoveAdmin(member.id)
                        MemberAction.REMOVE_MEMBER -> onRemoveMember(member.id)
                        MemberAction.MAKE_TREASURER -> onSetTreasurer(member.id)
                        MemberAction.REMOVE_TREASURER -> onSetTreasurer(null)
                        MemberAction.MAKE_OWNER -> onTransferOwnership(member.id)
                        MemberAction.RESTORE_SIREN -> onRestoreSiren(member.id)
                        MemberAction.TURN_OFF_SOS -> onSetSosDisabled(member.id, true)
                        MemberAction.TURN_ON_SOS -> onSetSosDisabled(member.id, false)
                    }
                },
                dark = dark,
                green = green,
                cardBg = cardBg
            )
        }
    }
}

private enum class MemberRole { OWNER, ADMIN, MEMBER }
private enum class MemberAction(val label: String, val destructive: Boolean) {
    MAKE_ADMIN("Make admin", false),
    REMOVE_ADMIN("Remove admin", true),
    MAKE_TREASURER("Make treasurer", false),
    REMOVE_TREASURER("Remove as treasurer", true),
    MAKE_OWNER("Make owner", false),
    REMOVE_MEMBER("Remove from estate", true),
    RESTORE_SIREN("Restore SOS siren", false),
    TURN_OFF_SOS("Turn off SOS alerts", true),
    TURN_ON_SOS("Turn on SOS alerts", false)
}

@Composable
private fun MemberCard(
    member: User,
    role: MemberRole,
    isYou: Boolean,
    isTreasurer: Boolean,
    /** Who holds the role now, so appointing someone else can say who they replace. */
    treasurerName: String?,
    /** A line about their SOS alerts for the admins, or null when there's nothing to say. */
    sosNote: String?,
    actions: List<MemberAction>,
    onAction: (MemberAction) -> Unit,
    dark: Color,
    green: Color,
    cardBg: Color
) {
    var confirming by remember { mutableStateOf<MemberAction?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val name = member.displayName ?: member.username ?: "Member"

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(imageUrl = member.profileImageUrl, name = name, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isYou) "$name (you)" else name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = dark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (role != MemberRole.MEMBER || isTreasurer) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (role != MemberRole.MEMBER) RoleBadge(owner = role == MemberRole.OWNER)
                        if (isTreasurer) RoleBadge(treasurer = true)
                    }
                }
                sosNote?.let {
                    Text(
                        it,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (actions.isNotEmpty()) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Manage $name", tint = dark.copy(alpha = 0.6f))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        actions.forEach { action ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        action.label,
                                        color = if (action.destructive) MaterialTheme.colorScheme.error else dark,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    confirming = action
                                }
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 72.dp))
    }

    confirming?.let { pending ->
        val (title, body) = when (pending) {
            MemberAction.MAKE_ADMIN -> "Make $name an admin?" to
                "They'll be able to approve join requests, post announcements, make other members admins and remove members."
            MemberAction.REMOVE_ADMIN -> "Remove $name as admin?" to
                "They'll stay in the estate as a regular member."
            MemberAction.MAKE_TREASURER -> "Make $name the treasurer?" to (
                (treasurerName?.let { "They'll take over from $it. " } ?: "") +
                    "They'll be able to set dues, confirm residents' payments, send reminders and log the estate's expenses."
            )
            MemberAction.REMOVE_TREASURER -> "Remove $name as treasurer?" to
                "The estate will have no treasurer until you appoint another. Existing dues and expenses stay."
            MemberAction.MAKE_OWNER -> "Make $name the owner?" to (
                (if (role == MemberRole.MEMBER) "They'll be made an admin too. " else "") +
                    "They'll own the estate: nobody can remove their admin, and only they can hand ownership on. " +
                    "You'll stay an admin, but another admin could then remove your admin role."
            )
            MemberAction.REMOVE_MEMBER -> "Remove $name from the estate?" to
                "They'll lose access to the feed, the directory and estate announcements, and get a notification. " +
                "To come back they'll have to ask to join and be approved. The invite code won't work for them until you let them back in."
            MemberAction.RESTORE_SIREN -> "Restore $name's SOS siren?" to
                "Their alerts will sound the siren again. Their false alarms so far stop counting; " +
                "two more within 30 days and it's removed again."
            MemberAction.TURN_OFF_SOS -> "Turn off SOS alerts for $name?" to
                "They won't be able to send emergency alerts to the estate until an admin turns them back on. " +
                "They still get everyone else's."
            MemberAction.TURN_ON_SOS -> "Turn SOS alerts back on for $name?" to
                "They'll be able to send emergency alerts to the estate again."
        }
        AlertDialog(
            onDismissRequest = { confirming = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = { Text(body) },
            confirmButton = {
                TextButton(onClick = { onAction(pending); confirming = null }) {
                    Text(
                        if (pending == MemberAction.REMOVE_MEMBER) "Remove" else pending.label,
                        color = if (pending.destructive) MaterialTheme.colorScheme.error else green,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = null }) { Text("Cancel") }
            }
        )
    }
}
