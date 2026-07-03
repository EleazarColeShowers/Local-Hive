package com.eleazar.localhive.ui.estate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.eleazar.localhive.R
import com.eleazar.localhive.domain.model.JoinRequest
import com.eleazar.localhive.domain.model.User

@Composable
fun EstateAdminScreen(
    estateId: String,
    onBack: () -> Unit,
    viewModel: EstateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val adminIds = uiState.estate?.adminIds ?: emptyList()
    val currentUserId = remember { viewModel.getCurrentUserId() ?: "" }

    val green = colorResource(id = R.color.hivegreen)
    val gold = colorResource(id = R.color.backgroundgold)
    val dark = colorResource(id = R.color.darkgray)

    LaunchedEffect(Unit) {
        viewModel.loadEstate(estateId)
        viewModel.loadPendingRequests(estateId)
        viewModel.loadMembers(estateId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(gold, Color(0xFFFFF8E7))))
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
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Requests",
                            fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTab == 0) green else dark.copy(alpha = 0.5f)
                        )
                        if (uiState.pendingRequests.isNotEmpty()) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(green),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${uiState.pendingRequests.size}",
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
                onClick = { selectedTab = 1 },
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

        when (selectedTab) {
            0 -> RequestsTab(
                requests = uiState.pendingRequests,
                onApprove = { userId -> viewModel.approveRequest(estateId, userId) },
                onDeny = { userId -> viewModel.denyRequest(estateId, userId) },
                dark = dark,
                green = green
            )
            1 -> MembersTab(
                members = uiState.members,
                adminIds = adminIds,
                currentUserId = currentUserId,
                onPromote = { userId -> viewModel.promoteToAdmin(estateId, userId) },
                dark = dark,
                green = green
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
    green: Color
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
                green = green
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
    green: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        .background(Color(0xFFFFEEEE))
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
    currentUserId: String,
    onPromote: (String) -> Unit,
    dark: Color,
    green: Color
) {
    if (members.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No members yet", color = dark.copy(alpha = 0.4f), fontSize = 15.sp)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(members, key = { it.id }) { member ->
            MemberCard(
                member = member,
                isAdmin = member.id in adminIds,
                canPromote = currentUserId in adminIds && member.id !in adminIds && member.id != currentUserId,
                onPromote = { onPromote(member.id) },
                dark = dark,
                green = green
            )
        }
    }
}

@Composable
private fun MemberCard(
    member: User,
    isAdmin: Boolean,
    canPromote: Boolean,
    onPromote: () -> Unit,
    dark: Color,
    green: Color
) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!member.profileImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = member.profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(green.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = green, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    member.displayName ?: member.username ?: "Member",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = dark
                )
                if (isAdmin) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Admin", fontSize = 11.sp, color = Color(0xFFFFC107), fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (canPromote) {
                TextButton(
                    onClick = { showConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = green)
                ) {
                    Text("Make Admin", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Make Admin?") },
            text = {
                Text(
                    "Give ${member.displayName ?: "this member"} admin privileges? They'll be able to approve join requests and manage members.",
                    textAlign = TextAlign.Start
                )
            },
            confirmButton = {
                TextButton(onClick = { onPromote(); showConfirm = false }) {
                    Text("Confirm", color = green)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
