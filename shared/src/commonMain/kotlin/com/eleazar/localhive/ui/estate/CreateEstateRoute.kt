package com.eleazar.localhive.ui.estate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CreateEstateRoute(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: EstateViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.success) {
        if (uiState.success) onSuccess()
    }

    val colors = rememberEstateColors()

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
    viewModel: EstateViewModel = koinViewModel()
) = CreateEstateRoute(
    onSuccess = onSuccess,
    onBack = onBack,
    viewModel = viewModel
)
