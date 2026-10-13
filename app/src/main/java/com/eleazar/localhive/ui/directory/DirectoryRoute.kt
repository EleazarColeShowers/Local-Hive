package com.eleazar.localhive.ui.directory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DirectoryRoute(
    onUserClick: (String) -> Unit = {},
    onMessageUser: (String) -> Unit = {},
    viewModel: DirectoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DirectoryScreen(
        uiState = uiState,
        onSearch = viewModel::search,
        onUserClick = onUserClick,
        onMessageUser = onMessageUser
    )
}
