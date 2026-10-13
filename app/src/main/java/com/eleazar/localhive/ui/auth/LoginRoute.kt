package com.eleazar.localhive.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginRoute(
    onNavigateToSignup: () -> Unit,
    onLoginSuccess: (hasEstate: Boolean) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.user) {
        uiState.user?.let { user ->
            viewModel.clearUser()
            onLoginSuccess(user.estateId != null)
        }
    }

    LoginScreen(
        isLoading = uiState.isLoading,
        error = uiState.error,
        onLoginClick = { email, password -> viewModel.login(email.trim(), password) },
        onNavigateToSignup = onNavigateToSignup,
        passwordReset = uiState.reset,
        onSendPasswordReset = viewModel::sendPasswordReset,
        onDismissPasswordReset = viewModel::clearPasswordReset,
    ) {
        if (uiState.error != null) viewModel.clearError()
    }
}
