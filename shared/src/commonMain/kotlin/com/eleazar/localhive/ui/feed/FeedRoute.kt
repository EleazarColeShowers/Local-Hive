package com.eleazar.localhive.ui.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import com.eleazar.localhive.ui.components.PollsGlance
import com.eleazar.localhive.ui.finance.FinanceViewModel
import com.eleazar.localhive.ui.finance.duesSummary
import com.eleazar.localhive.ui.polls.PollsViewModel
import com.eleazar.localhive.ui.utilities.UtilitiesViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FeedRoute(
    onPostClick: (String) -> Unit = {},
    onCreatePost: () -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onEmergency: () -> Unit = {},
    onEditPost: (String) -> Unit = {},
    onUtilities: () -> Unit = {},
    onPolls: () -> Unit = {},
    onDues: () -> Unit = {},
    viewModel: FeedViewModel = koinViewModel(),
    utilitiesViewModel: UtilitiesViewModel = koinViewModel(),
    pollsViewModel: PollsViewModel = koinViewModel(),
    financeViewModel: FinanceViewModel = koinViewModel()
) {
    val finance by financeViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(financeViewModel) { financeViewModel.loadLevies() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val utilities by utilitiesViewModel.uiState.collectAsStateWithLifecycle()
    val polls by pollsViewModel.uiState.collectAsStateWithLifecycle()

    // Deleting from the feed has no screen to close.
    LaunchedEffect(uiState.postDeleted) {
        if (uiState.postDeleted) viewModel.consumePostDeleted()
    }

    FeedScreen(
        uiState = uiState,
        onPostClick = onPostClick,
        onCreatePost = onCreatePost,
        onLike = viewModel::toggleLike,
        onLoadMore = viewModel::loadMorePosts,
        onUserClick = onUserClick,
        onEmergency = onEmergency,
        onEditPost = onEditPost,
        onDeletePost = viewModel::deletePost,
        utilities = utilities.statuses,
        polls = polls.openPolls().let { open ->
            PollsGlance(open = open.size, awaitingVote = open.count { it.id !in polls.myVotes })
        },
        dues = if (finance.isLoading || finance.estateId == null) null else duesSummary(finance.levies, finance.isTreasurer),
        onUtilities = onUtilities,
        onPolls = onPolls,
        onDues = onDues
    )
}
