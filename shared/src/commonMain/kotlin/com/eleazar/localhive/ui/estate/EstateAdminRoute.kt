package com.eleazar.localhive.ui.estate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel
import com.eleazar.localhive.ui.platform.rememberPlatformActions
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EstateAdminRoute(
    estateId: String,
    onBack: () -> Unit,
    viewModel: EstateViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUserId = remember { viewModel.getCurrentUserId() ?: "" }

    LaunchedEffect(estateId) {
        viewModel.loadEstate(estateId)
        viewModel.loadPendingRequests(estateId)
        viewModel.loadMembers(estateId)
        viewModel.loadInviteCode(estateId)
    }
    val platform = rememberPlatformActions()

    val colors = rememberEstateColors()

    EstateAdminScreen(
        estate = uiState.estate,
        pendingRequests = uiState.pendingRequests,
        members = uiState.members,
        currentUserId = currentUserId,
        error = uiState.error,
        inviteCode = uiState.inviteCode,
        isSaving = uiState.isSaving,
        message = uiState.message,
        colors = colors,
        onBack = onBack,
        onApproveRequest = { userId -> viewModel.approveRequest(estateId, userId) },
        onDenyRequest = { userId -> viewModel.denyRequest(estateId, userId) },
        onPromoteToAdmin = { userId -> viewModel.promoteToAdmin(estateId, userId) },
        onRemoveAdmin = { userId -> viewModel.removeAdmin(estateId, userId) },
        onRemoveMember = { userId -> viewModel.removeMember(estateId, userId) },
        onSetTreasurer = { userId -> viewModel.setTreasurer(estateId, userId) },
        onTransferOwnership = { userId -> viewModel.transferOwnership(estateId, userId) },
        onSaveDetails = { name, address -> viewModel.updateEstateDetails(estateId, name, address) },
        onSetJoinMode = { mode -> viewModel.setJoinMode(estateId, mode) },
        onRegenerateCode = { viewModel.regenerateInviteCode(estateId) },
        onCopyCode = { code -> platform.copyToClipboard("Invite Code", code, "Code copied!") },
        onMessageShown = { viewModel.consumeMessage() },
        onClearError = { viewModel.clearError() }
    )
}

/** Backwards-compatibility alias for callers expecting EstateAdminScreen. */
@Composable
fun EstateAdminScreen(
    estateId: String,
    onBack: () -> Unit,
    viewModel: EstateViewModel = koinViewModel()
) = EstateAdminRoute(
    estateId = estateId,
    onBack = onBack,
    viewModel = viewModel
)
