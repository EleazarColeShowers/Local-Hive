package com.eleazar.localhive.ui.utilities

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun UtilitiesRoute(onBack: () -> Unit, viewModel: UtilitiesViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    UtilitiesScreen(
        uiState = uiState,
        onReport = viewModel::report,
        onBack = onBack,
        onLightAlertsChange = viewModel::setLightAlerts
    )
}
