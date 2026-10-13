package com.eleazar.localhive.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

private const val MIN_SPLASH_MILLIS = 600L

@Composable
fun SplashRoute(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToEstateSelection: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    // Shown for a short minimum counted from launch, rather than a fixed pause added on
    // after the auth check has already finished.
    val shownAt = remember { System.currentTimeMillis() }
    LaunchedEffect(authState) {
        if (authState is SplashViewModel.AuthState.Loading) return@LaunchedEffect
        delay((MIN_SPLASH_MILLIS - (System.currentTimeMillis() - shownAt)).coerceAtLeast(0))
        when (authState) {
            is SplashViewModel.AuthState.NotLoggedIn -> onNavigateToOnboarding()
            is SplashViewModel.AuthState.LoggedInNoEstate -> onNavigateToEstateSelection()
            is SplashViewModel.AuthState.LoggedInWithEstate -> onNavigateToMain()
            is SplashViewModel.AuthState.Loading -> {}
        }
    }

    SplashScreen()
}
