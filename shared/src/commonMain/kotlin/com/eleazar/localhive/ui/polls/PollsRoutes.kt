package com.eleazar.localhive.ui.polls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PollsRoute(
    onBack: () -> Unit,
    onCreatePoll: () -> Unit,
    viewModel: PollsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PollsScreen(
        uiState = uiState,
        onVote = viewModel::vote,
        onClose = viewModel::closePoll,
        onDelete = viewModel::deletePoll,
        onCreatePoll = onCreatePoll,
        onBack = onBack
    )
}

@Composable
fun CreatePollRoute(onBack: () -> Unit, viewModel: PollsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.pollCreated) {
        if (uiState.pollCreated) {
            viewModel.consumePollCreated()
            onBack()
        }
    }

    CreatePollScreen(uiState = uiState, onCreate = viewModel::createPoll, onBack = onBack)
}
