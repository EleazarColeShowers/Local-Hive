package com.eleazar.localhive.ui.directory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DirectoryRoute(
    onUserClick: (String) -> Unit = {},
    onMessageUser: (String) -> Unit = {},
    viewModel: DirectoryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DirectoryScreen(
        uiState = uiState,
        onSearch = viewModel::search,
        onRefresh = viewModel::refresh,
        onUserClick = onUserClick,
        onMessageUser = onMessageUser
    )
}
