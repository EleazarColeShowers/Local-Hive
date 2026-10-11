package com.eleazar.localhive.ui.estate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.colorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.R

@Composable
fun EstateSelectionRoute(
    onEstateJoined: () -> Unit,
    viewModel: EstateViewModel = hiltViewModel()
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

    val colors = EstateColors(
        hiveGreen = colorResource(id = R.color.hivegreen),
        backgroundGold = colorResource(id = R.color.backgroundgold),
        darkGray = colorResource(id = R.color.darkgray)
    )

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
    viewModel: EstateViewModel = hiltViewModel()
) = EstateSelectionRoute(
    onEstateJoined = onEstateJoined,
    viewModel = viewModel
)
