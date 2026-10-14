package com.eleazar.localhive.ui.estate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel
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
    }

    val colors = rememberEstateColors()

    EstateAdminScreen(
        estate = uiState.estate,
        pendingRequests = uiState.pendingRequests,
        members = uiState.members,
        currentUserId = currentUserId,
        error = uiState.error,
        colors = colors,
        onBack = onBack,
        onApproveRequest = { userId -> viewModel.approveRequest(estateId, userId) },
        onDenyRequest = { userId -> viewModel.denyRequest(estateId, userId) },
        onPromoteToAdmin = { userId -> viewModel.promoteToAdmin(estateId, userId) },
        onRemoveAdmin = { userId -> viewModel.removeAdmin(estateId, userId) },
        onRemoveMember = { userId -> viewModel.removeMember(estateId, userId) },
        onSetTreasurer = { userId -> viewModel.setTreasurer(estateId, userId) },
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
