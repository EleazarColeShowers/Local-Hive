package com.eleazar.localhive.ui.visitors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.ui.platform.rememberPlatformActions
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VisitorsRoute(onBack: () -> Unit, viewModel: VisitorsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val platform = rememberPlatformActions()
    VisitorsScreen(
        uiState = uiState,
        onBack = onBack,
        onCreate = viewModel::createPass,
        onDismissCreated = viewModel::dismissCreated,
        onShare = { pass -> platform.shareText(passShareText(pass), "Share gate pass") },
        onCopy = { pass -> platform.copyToClipboard("Gate pass", pass.code, "Code copied!") },
        onCancel = viewModel::cancelPass,
        onDelete = viewModel::deletePass
    )
}
