package com.eleazar.localhive.ui.directory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.ui.blocking.BlockingViewModel
import com.eleazar.localhive.domain.model.Report
import com.eleazar.localhive.domain.model.ReportSubject
import com.eleazar.localhive.ui.report.rememberReporter

@Composable
fun UserProfileRoute(
    userId: String,
    onNavigateBack: () -> Unit,
    onMessageUser: (chatId: String) -> Unit,
    viewModel: DirectoryViewModel = koinViewModel(),
    blockingViewModel: BlockingViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val blocking by blockingViewModel.uiState.collectAsStateWithLifecycle()
    val report = rememberReporter()

    LaunchedEffect(userId) {
        viewModel.loadUserProfile(userId)
    }

    UserProfileScreen(
        userId = userId,
        onNavigateBack = onNavigateBack,
        onMessageUser = onMessageUser,
        directoryState = uiState,
        isBlocked = blocking.isBlocked(userId),
        onBlock = {
            val user = uiState.selectedUser?.takeIf { it.id == userId }
            blockingViewModel.block(
                targetId = userId,
                name = user?.displayName?.takeIf { it.isNotBlank() } ?: user?.username.orEmpty(),
                photoUrl = user?.profileImageUrl
            )
        },
        onUnblock = { blockingViewModel.unblock(userId) },
        onReport = { reason, details ->
            val user = uiState.selectedUser?.takeIf { it.id == userId }
            report(
                Report(
                    subject = ReportSubject.USER, reason = reason, details = details,
                    targetId = userId, targetUserId = userId,
                    excerpt = listOfNotNull(user?.displayName, user?.username, user?.profileImageUrl)
                        .filter { it.isNotBlank() }.joinToString("\n")
                )
            )
        }
    )
}
