package com.eleazar.localhive.ui.report

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.ReportRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.Report
import com.eleazar.localhive.ui.platform.rememberPlatformActions
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/** Sends reports and feedback, for any screen that offers them. */
class ReportViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val reportRepository: ReportRepository
) : ViewModel() {

    fun submit(report: Report, appVersion: String) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val estateId = userRepository.getUser(userId, cacheFirst = true).getOrNull()?.estateId
            // Firestore keeps the write and sends it once there's a connection, so the dialog
            // thanks the user straight away rather than waiting on this.
            reportRepository.submit(report, userId, estateId, appVersion)
        }
    }
}

/** What a screen calls to send a [Report]. */
@Composable
fun rememberReporter(viewModel: ReportViewModel = koinViewModel()): (Report) -> Unit {
    val platform = rememberPlatformActions()
    return remember(viewModel, platform) { { report -> viewModel.submit(report, platform.appVersion) } }
}
