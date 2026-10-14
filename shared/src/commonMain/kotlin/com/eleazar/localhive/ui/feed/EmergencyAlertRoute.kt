package com.eleazar.localhive.ui.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EmergencyAlertRoute(
    onNavigateBack: () -> Unit,
    viewModel: FeedViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.postCreated) {
        if (uiState.postCreated) {
            viewModel.consumePostCreated()
            onNavigateBack()
        }
    }

    EmergencyAlertScreen(
        uiState = uiState,
        onSend = viewModel::sendEmergency,
        onNavigateBack = onNavigateBack
    )
}
