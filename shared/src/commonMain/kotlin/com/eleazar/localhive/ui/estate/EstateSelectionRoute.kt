package com.eleazar.localhive.ui.estate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EstateSelectionRoute(
    onEstateJoined: () -> Unit,
    viewModel: EstateViewModel = koinViewModel()
) {
    var showCreate by remember { mutableStateOf(false) }
    var showJoin by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.success) {
        if (uiState.success) onEstateJoined()
    }

    if (showCreate) {
        CreateEstateRoute(onSuccess = onEstateJoined, onBack = { showCreate = false }, viewModel = viewModel)
        return
    }
    if (showJoin) {
        JoinEstateRoute(onSuccess = onEstateJoined, onBack = { showJoin = false }, viewModel = viewModel)
        return
    }

    val colors = rememberEstateColors()

    EstateSelectionScreen(
        pendingRequest = uiState.pendingRequest,
        colors = colors,
        onCreateClick = { showCreate = true },
        onJoinClick = { showJoin = true },
        onCancelRequest = { viewModel.cancelRequest() }
    )
}

/** Backwards-compatibility alias for callers expecting EstateSelectionScreen. */
@Composable
fun EstateSelectionScreen(
    onEstateJoined: () -> Unit,
    viewModel: EstateViewModel = koinViewModel()
) = EstateSelectionRoute(
    onEstateJoined = onEstateJoined,
    viewModel = viewModel
)
