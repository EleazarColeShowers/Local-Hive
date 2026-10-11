package com.eleazar.localhive.ui.estate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.colorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.R

@Composable
fun JoinEstateRoute(
    initialCode: String = "",
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: EstateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.success) {
        if (uiState.success) onSuccess()
    }

    LaunchedEffect(Unit) {
        viewModel.searchEstates("")
    }

    val colors = EstateColors(
        hiveGreen = colorResource(id = R.color.hivegreen),
        backgroundGold = colorResource(id = R.color.backgroundgold),
        darkGray = colorResource(id = R.color.darkgray)
    )

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
    viewModel: EstateViewModel = hiltViewModel()
) = JoinEstateRoute(
    initialCode = initialCode,
    onSuccess = onSuccess,
    onBack = onBack,
    viewModel = viewModel
)
