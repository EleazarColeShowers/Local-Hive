package com.eleazar.localhive.ui.estate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun JoinEstateRoute(
    initialCode: String = "",
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: EstateViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.success) {
        if (uiState.success) onSuccess()
    }

    LaunchedEffect(Unit) {
        viewModel.searchEstates("")
    }

    val colors = rememberEstateColors()

    JoinEstateScreen(
        initialCode = initialCode,
        searchResults = uiState.searchResults,
        requestedEstateIds = uiState.requestedEstateIds,
        isLoading = uiState.isLoading,
        error = uiState.error,
        colors = colors,
        onBack = onBack,
        onSearchQueryChange = { query -> viewModel.searchEstates(query) },
        onRequestJoin = { estateId -> viewModel.requestToJoin(estateId) },
        onJoinWithCode = { inviteCode -> viewModel.joinEstate(inviteCode) }
    )
}

/** Backwards-compatibility alias for callers expecting JoinEstateScreen. */
@Composable
fun JoinEstateScreen(
    initialCode: String = "",
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: EstateViewModel = koinViewModel()
) = JoinEstateRoute(
    initialCode = initialCode,
    onSuccess = onSuccess,
    onBack = onBack,
    viewModel = viewModel
)
