package com.eleazar.localhive.ui.blocking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BlockedUsersRoute(
    onBack: () -> Unit,
    viewModel: BlockingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BlockedUsersScreen(
        blocked = uiState.blocked,
        isLoading = uiState.isLoading,
        error = uiState.error,
        onUnblock = viewModel::unblock,
        onBack = onBack
    )
}
