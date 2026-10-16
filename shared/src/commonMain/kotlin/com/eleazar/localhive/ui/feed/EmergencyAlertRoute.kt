package com.eleazar.localhive.ui.feed

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EmergencyAlertRoute(
    onNavigateBack: () -> Unit,
    viewModel: FeedViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.postCreated) {
        if (uiState.postCreated) {
            viewModel.consumePostCreated()
            onNavigateBack()
        }
    }

    // The alert is in the feed, but nobody's phone rang; say so rather than close as if it had.
    uiState.emergencyNotPushed?.let { reason ->
        val close = {
            viewModel.consumeEmergencyNotPushed()
            onNavigateBack()
        }
        AlertDialog(
            onDismissRequest = close,
            title = { Text("Posted, but no alert sent") },
            text = { Text("Your emergency is in the estate feed, but neighbours weren't notified. $reason If someone's life is in danger, call 112.") },
            confirmButton = { TextButton(onClick = close) { Text("OK") } }
        )
    }

    EmergencyAlertScreen(
        uiState = uiState,
        onSend = viewModel::sendEmergency,
        onNavigateBack = onNavigateBack
    )
}
