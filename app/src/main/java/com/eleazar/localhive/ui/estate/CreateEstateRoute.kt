package com.eleazar.localhive.ui.estate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.colorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.R

@Composable
fun CreateEstateRoute(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: EstateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.success) {
        if (uiState.success) onSuccess()
    }

    val colors = EstateColors(
        hiveGreen = colorResource(id = R.color.hivegreen),
        backgroundGold = colorResource(id = R.color.backgroundgold),
        darkGray = colorResource(id = R.color.darkgray)
    )

    CreateEstateScreen(
        isLoading = uiState.isLoading,
        error = uiState.error,
        colors = colors,
        onCreateEstate = { name, address -> viewModel.createEstate(name, address) },
        onBack = onBack
    )
}

/** Backwards-compatibility alias for callers expecting CreateEstateScreen. */
@Composable
fun CreateEstateScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: EstateViewModel = hiltViewModel()
) = CreateEstateRoute(
    onSuccess = onSuccess,
    onBack = onBack,
    viewModel = viewModel
)
