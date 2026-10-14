package com.eleazar.localhive.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.ui.util.nowMillis
import kotlinx.coroutines.delay

private const val MIN_SPLASH_MILLIS = 600L

@Composable
fun SplashRoute(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToEstateSelection: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: SplashViewModel = koinViewModel()
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    // Shown for a short minimum counted from launch, rather than a fixed pause added on
    // after the auth check has already finished.
    val shownAt = remember { nowMillis() }
    LaunchedEffect(authState) {
        if (authState is SplashViewModel.AuthState.Loading) return@LaunchedEffect
        delay((MIN_SPLASH_MILLIS - (nowMillis() - shownAt)).coerceAtLeast(0))
        when (authState) {
            is SplashViewModel.AuthState.NotLoggedIn -> onNavigateToOnboarding()
            is SplashViewModel.AuthState.LoggedInNoEstate -> onNavigateToEstateSelection()
            is SplashViewModel.AuthState.LoggedInWithEstate -> onNavigateToMain()
            is SplashViewModel.AuthState.Loading -> {}
        }
    }

    SplashScreen()
}
