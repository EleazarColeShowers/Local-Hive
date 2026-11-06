package com.eleazar.localhive.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import com.eleazar.localhive.data.remote.LocalHiveApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SignupRoute(
    onNavigateToLogin: () -> Unit,
    onSignupSuccess: () -> Unit,
    viewModel: AuthViewModel = koinViewModel(),
    api: LocalHiveApi = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.user) {
        if (uiState.user != null) onSignupSuccess()
    }

    SignupScreen(
        isLoading = uiState.isLoading,
        error = uiState.error,
        onSignup = viewModel::signup,
        onNavigateToLogin = onNavigateToLogin,
        termsUrl = api.pageLink("terms")
    )
}
