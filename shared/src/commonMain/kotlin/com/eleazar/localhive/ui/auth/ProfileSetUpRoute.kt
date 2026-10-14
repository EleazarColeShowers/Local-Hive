package com.eleazar.localhive.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.ui.platform.MediaKind
import com.eleazar.localhive.ui.platform.rememberMediaPicker
import org.koin.compose.viewmodel.koinViewModel

/** Entry point: owns the ViewModel and the system photo picker. */
@Composable
fun ProfileSetUpRoute(
    onProfileComplete: () -> Unit,
    viewModel: ProfileSetUpViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var imageUri by rememberSaveable { mutableStateOf<String?>(null) }

    val pickPhoto = rememberMediaPicker(MediaKind.IMAGE) { imageUri = it.uri }

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onProfileComplete()
    }

    ProfileSetUpScreen(
        isLoading = uiState.isLoading,
        error = uiState.error,
        pickedImage = imageUri,
        onPickPhoto = pickPhoto,
        onContinue = { details -> viewModel.saveProfile(details, imageUri) }
    )
}
