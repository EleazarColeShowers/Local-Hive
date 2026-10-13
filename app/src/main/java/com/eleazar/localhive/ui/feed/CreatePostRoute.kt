package com.eleazar.localhive.ui.feed

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** App-module entry point: owns the ViewModel and the Android photo picker. */
@Composable
fun CreatePostRoute(
    onNavigateBack: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { imageUri = it } }

    LaunchedEffect(uiState.postCreated) {
        if (uiState.postCreated) {
            viewModel.consumePostCreated()
            onNavigateBack()
        }
    }

    CreatePostScreen(
        uiState = uiState,
        pickedImage = imageUri,
        onPickPhoto = {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onRemovePhoto = { imageUri = null },
        onPost = { content, category -> viewModel.createPost(content, imageUri, category) },
        onNavigateBack = onNavigateBack
    )
}
