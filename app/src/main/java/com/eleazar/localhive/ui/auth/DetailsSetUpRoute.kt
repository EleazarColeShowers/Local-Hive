package com.eleazar.localhive.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DetailsSetUpRoute(
    onDetailsComplete: () -> Unit,
    viewModel: DetailsSetUpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onDetailsComplete()
    }

    DetailsSetUpScreen(
        isLoading = uiState.isLoading,
        error = uiState.error,
        onSave = viewModel::saveDetails,
        onSkip = viewModel::skip
    )
}
