package com.eleazar.localhive.ui.estate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.navigationBarsPadding
import com.eleazar.localhive.domain.model.Estate
import com.eleazar.localhive.domain.model.JoinMode
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import com.eleazar.localhive.domain.model.JoinRequest
import com.eleazar.localhive.domain.model.RemovedMember
import com.eleazar.localhive.domain.model.User
import com.eleazar.localhive.domain.model.SosStanding

/**
 * Bare UI for estate admin management. No ViewModel or Android Context side effects.
 */
@Composable
fun EstateAdminScreen(
    estate: Estate?,
    pendingRequests: List<JoinRequest>,
    members: List<User>,
    removed: List<RemovedMember> = emptyList(),
    currentUserId: String,
    error: String?,
    colors: EstateColors,
    inviteCode: String = "",
    isSaving: Boolean = false,
    message: String? = null,
    onBack: () -> Unit,
    onApproveRequest: (userId: String) -> Unit,
    onDenyRequest: (userId: String) -> Unit,
    onLetBackIn: (RemovedMember) -> Unit = {},
    onPromoteToAdmin: (userId: String) -> Unit,
    onRemoveAdmin: (userId: String) -> Unit = {},
    onRemoveMember: (userId: String) -> Unit = {},
    /** Null leaves the estate without a treasurer. */
    onSetTreasurer: (userId: String?) -> Unit = {},
    onTransferOwnership: (userId: String) -> Unit = {},
    /** Each member's SOS standing, by user id; those with nothing on record are left out. */
    sosStandings: Map<String, SosStanding> = emptyMap(),
    onSetSosDisabled: (userId: String, disabled: Boolean) -> Unit = { _, _ -> },
    onRestoreSiren: (userId: String) -> Unit = {},
    onSaveDetails: (name: String, address: String) -> Unit = { _, _ -> },
    onSetJoinMode: (JoinMode) -> Unit = {},
    onRegenerateCode: () -> Unit = {},
    onCopyCode: (code: String) -> Unit = {},
    onShareCode: (code: String) -> Unit = {},
    onMessageShown: () -> Unit = {},
    onClearError: () -> Unit = {},
    /** The Guards tab's content, which brings its own state. */
    guardsTab: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val adminIds = estate?.adminIds ?: emptyList()

    val green = colors.hiveGreen
    val gold = colors.backgroundGold
    val dark = colors.darkGray
    val cardBg = colors.cardBackground
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            onMessageShown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
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
                    Text("Estate admin", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = dark)
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
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; onClearError() },
                    text = {
                        Text(
                            "Guards",
                            fontWeight = if (selectedTab == 2) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTab == 2) green else dark.copy(alpha = 0.5f)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3; onClearError() },
                    text = {
                        Text(
                            "Settings",
                            fontWeight = if (selectedTab == 3) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTab == 3) green else dark.copy(alpha = 0.5f)
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
                    removed = removed,
                    onApprove = onApproveRequest,
                    onDeny = onDenyRequest,
                    onLetBackIn = onLetBackIn,
                    dark = dark,
                    green = green,
                    cardBg = cardBg
                )
                1 -> MembersTab(
                    members = members,
                    adminIds = adminIds,
                    ownerId = estate?.ownerId.orEmpty(),
                    treasurerId = estate?.treasurerId.orEmpty(),
                    currentUserId = currentUserId,
                    onPromote = onPromoteToAdmin,
                    onRemoveAdmin = onRemoveAdmin,
                    onRemoveMember = onRemoveMember,
                    onSetTreasurer = onSetTreasurer,
                    onTransferOwnership = onTransferOwnership,
                    sosStandings = sosStandings,
                    onSetSosDisabled = onSetSosDisabled,
                    onRestoreSiren = onRestoreSiren,
                    dark = dark,
                    green = green,
                    cardBg = cardBg
                )
                2 -> guardsTab()
                3 -> EstateSettingsTab(
                    estate = estate,
                    inviteCode = inviteCode,
                    isSaving = isSaving,
                    onSaveDetails = onSaveDetails,
                    onSetJoinMode = onSetJoinMode,
                    onRegenerateCode = onRegenerateCode,
                    onCopyCode = onCopyCode,
                    onShareCode = onShareCode,
                    dark = dark,
                    green = green,
                    cardBg = cardBg
                )
            }
        }
        SnackbarHost(
            snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
        )
    }
}
