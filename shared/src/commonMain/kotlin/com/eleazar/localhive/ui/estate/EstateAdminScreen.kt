package com.eleazar.localhive.ui.estate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextOverflow
import com.eleazar.localhive.ui.components.Avatar
import com.eleazar.localhive.ui.components.RoleBadge
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.navigationBarsPadding
import com.eleazar.localhive.domain.model.Estate
import com.eleazar.localhive.domain.model.JoinRequest
import com.eleazar.localhive.domain.model.User

/**
 * Bare UI for estate admin management. No ViewModel or Android Context side effects.
 */
@Composable
fun EstateAdminScreen(
    estate: Estate?,
    pendingRequests: List<JoinRequest>,
    members: List<User>,
    currentUserId: String,
    error: String?,
    colors: EstateColors,
    onBack: () -> Unit,
    onApproveRequest: (userId: String) -> Unit,
    onDenyRequest: (userId: String) -> Unit,
    onPromoteToAdmin: (userId: String) -> Unit,
    onRemoveAdmin: (userId: String) -> Unit = {},
    onRemoveMember: (userId: String) -> Unit = {},
    onClearError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val adminIds = estate?.adminIds ?: emptyList()

    val green = colors.hiveGreen
    val gold = colors.backgroundGold
    val dark = colors.darkGray
    val cardBg = colors.cardBackground

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gold)
            // Drawn edge to edge, so keep the header clear of the status bar and the list of the nav bar.
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = dark)
            }
            Column {
                Text("Estate Admin", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = dark)
                Text("Manage your community", fontSize = 12.sp, color = dark.copy(alpha = 0.5f))
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = green,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = green
                )
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0; onClearError() },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Requests",
                            fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTab == 0) green else dark.copy(alpha = 0.5f)
                        )
                        if (pendingRequests.isNotEmpty()) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(green),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${pendingRequests.size}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1; onClearError() },
                text = {
                    Text(
                        "Members",
                        fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selectedTab == 1) green else dark.copy(alpha = 0.5f)
                    )
                }
            )
        }

        HorizontalDivider(color = dark.copy(alpha = 0.08f))

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        when (selectedTab) {
            0 -> RequestsTab(
                requests = pendingRequests,
                onApprove = onApproveRequest,
                onDeny = onDenyRequest,
                dark = dark,
                green = green,
                cardBg = cardBg
            )
            1 -> MembersTab(
                members = members,
                adminIds = adminIds,
                ownerId = estate?.ownerId.orEmpty(),
                currentUserId = currentUserId,
                onPromote = onPromoteToAdmin,
                onRemoveAdmin = onRemoveAdmin,
                onRemoveMember = onRemoveMember,
                dark = dark,
                green = green,
                cardBg = cardBg
            )
        }
    }
}

@Composable
private fun RequestsTab(
    requests: List<JoinRequest>,
    onApprove: (String) -> Unit,
    onDeny: (String) -> Unit,
    dark: Color,
    green: Color,
    cardBg: Color
) {
    if (requests.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = dark.copy(alpha = 0.2f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("No pending requests", color = dark.copy(alpha = 0.4f), fontSize = 15.sp)
                Text(
                    "Join requests will appear here",
                    color = dark.copy(alpha = 0.3f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests, key = { it.userId }) { request ->
            JoinRequestCard(
                request = request,
                onApprove = { onApprove(request.userId) },
                onDeny = { onDeny(request.userId) },
                dark = dark,
                green = green,
                cardBg = cardBg
            )
        }
    }
}

@Composable
private fun JoinRequestCard(
    request: JoinRequest,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    dark: Color,
    green: Color,
    cardBg: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (request.userPhotoUrl.isNotBlank()) {
                AsyncImage(
                    model = request.userPhotoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(green.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = green, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    request.userName.ifBlank { "Unknown User" },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = dark
                )
                Text("Wants to join", fontSize = 12.sp, color = dark.copy(alpha = 0.5f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onDeny,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935).copy(alpha = 0.15f))
                ) {
                    Icon(Icons.Default.Close, "Deny", tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onApprove,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(green.copy(alpha = 0.12f))
                ) {
                    Icon(Icons.Default.Check, "Approve", tint = green, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun MembersTab(
    members: List<User>,
    adminIds: List<String>,
    ownerId: String,
    currentUserId: String,
    onPromote: (String) -> Unit,
    onRemoveAdmin: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    dark: Color,
    green: Color,
    cardBg: Color
) {
    if (members.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No members yet", color = dark.copy(alpha = 0.4f), fontSize = 15.sp)
        }
        return
    }

    val viewerIsAdmin = currentUserId in adminIds
    // Owner first, then admins, then everyone else, each alphabetical.
    val sorted = members.sortedWith(
        compareBy<User>({ it.id != ownerId }, { it.id !in adminIds }, { (it.displayName ?: it.username ?: "").lowercase() })
    )

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Admins can approve requests, post announcements, make others admins and remove members. " +
                    "The owner always stays an admin, and an admin has to lose admin before they can be removed.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = dark.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(sorted, key = { it.id }) { member ->
            val isAdmin = member.id in adminIds
            val isOwner = member.id == ownerId
            MemberCard(
                member = member,
                role = when {
                    isOwner -> MemberRole.OWNER
                    isAdmin -> MemberRole.ADMIN
                    else -> MemberRole.MEMBER
                },
                isYou = member.id == currentUserId,
                actions = when {
                    !viewerIsAdmin || member.id == currentUserId || isOwner -> emptyList()
                    isAdmin -> listOf(MemberAction.REMOVE_ADMIN)
                    else -> listOf(MemberAction.MAKE_ADMIN, MemberAction.REMOVE_MEMBER)
                },
                onAction = { action ->
                    when (action) {
                        MemberAction.MAKE_ADMIN -> onPromote(member.id)
                        MemberAction.REMOVE_ADMIN -> onRemoveAdmin(member.id)
                        MemberAction.REMOVE_MEMBER -> onRemoveMember(member.id)
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
    REMOVE_MEMBER("Remove from estate", true)
}

@Composable
private fun MemberCard(
    member: User,
    role: MemberRole,
    isYou: Boolean,
    actions: List<MemberAction>,
    onAction: (MemberAction) -> Unit,
    dark: Color,
    green: Color,
    cardBg: Color
) {
    var confirming by remember { mutableStateOf<MemberAction?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val name = member.displayName ?: member.username ?: "Member"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, dark.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(imageUrl = member.profileImageUrl, name = name, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isYou) "$name (you)" else name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = dark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (role != MemberRole.MEMBER) {
                    Spacer(Modifier.height(4.dp))
                    RoleBadge(owner = role == MemberRole.OWNER)
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
    }

    confirming?.let { pending ->
        val (title, body) = when (pending) {
            MemberAction.MAKE_ADMIN -> "Make $name an admin?" to
                "They'll be able to approve join requests, post announcements, make other members admins and remove members."
            MemberAction.REMOVE_ADMIN -> "Remove $name as admin?" to
                "They'll stay in the estate as a regular member."
            MemberAction.REMOVE_MEMBER -> "Remove $name from the estate?" to
                "They'll lose access to the feed, the directory and estate announcements, and get a notification. " +
                "To come back they'll have to ask to join again or use an invite code."
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
