package com.eleazar.localhive.ui.gate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/**
 * @param onSignedOut the guard signed out of the app.
 * @param onLeftPost the guard is no longer at any gate, by their choice or an admin's; they go
 *   back to choosing an estate, where they can enter a new guard code.
 */
@Composable
fun GateRoute(
    onSignedOut: () -> Unit,
    onLeftPost: () -> Unit,
    viewModel: GateViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(uiState.signedOut) { if (uiState.signedOut) onSignedOut() }
    LaunchedEffect(uiState.leftPost) { if (uiState.leftPost) onLeftPost() }

    GateScreen(
        uiState = uiState,
        onLookUp = viewModel::lookUp,
        onClearLookup = viewModel::clearLookup,
        onCheckIn = viewModel::checkIn,
        onCheckOut = viewModel::checkOut,
        onMessageShown = viewModel::consumeMessage,
        onRetry = viewModel::retry,
        onLeavePost = viewModel::leavePost,
        onSignOut = viewModel::signOut
    )
}

@Composable
fun GuardSignOnRoute(
    onSignedOn: () -> Unit,
    onBack: () -> Unit,
    viewModel: GateViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.signedOn) { if (uiState.signedOn) onSignedOn() }
    GuardSignOnScreen(
        isRedeeming = uiState.isRedeeming,
        error = uiState.redeemError,
        onRedeem = viewModel::redeem,
        onCodeChanged = viewModel::clearRedeemError,
        onBack = onBack
    )
}
