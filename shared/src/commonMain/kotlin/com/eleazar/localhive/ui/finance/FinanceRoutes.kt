package com.eleazar.localhive.ui.finance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.ui.platform.MediaKind
import com.eleazar.localhive.ui.platform.rememberMediaPicker

@Composable
fun FinancesRoute(
    onBack: () -> Unit,
    onLevyClick: (String) -> Unit,
    onCreateLevy: () -> Unit,
    onAddExpense: () -> Unit,
    viewModel: FinanceViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.loadLevies()
        viewModel.loadExpenses()
    }
    FinancesScreen(
        uiState = uiState,
        onBack = onBack,
        onLevyClick = onLevyClick,
        onCreateLevy = onCreateLevy,
        onAddExpense = onAddExpense,
        onDeleteExpense = viewModel::deleteExpense,
        onMessageShown = viewModel::consumeMessage,
        onErrorShown = viewModel::consumeError
    )
}

@Composable
fun LevyDetailRoute(
    levyId: String,
    onBack: () -> Unit,
    viewModel: FinanceViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pickProof = rememberMediaPicker(MediaKind.IMAGE) { viewModel.sendProof(it.uri) }
    LaunchedEffect(levyId) { viewModel.openLevy(levyId) }
    // Deleting the levy closes it.
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            viewModel.consumeSaved()
            onBack()
        }
    }
    LevyDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onSendProof = pickProof,
        onConfirm = viewModel::confirmPayment,
        onReject = viewModel::rejectPayment,
        onMarkPaid = viewModel::markPaid,
        onRemind = viewModel::remindUnpaid,
        onDelete = viewModel::deleteLevy,
        onMessageShown = viewModel::consumeMessage,
        onErrorShown = viewModel::consumeError
    )
}

@Composable
fun CreateLevyRoute(
    onBack: () -> Unit,
    viewModel: FinanceViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            viewModel.consumeSaved()
            onBack()
        }
    }
    CreateLevyScreen(uiState = uiState, onBack = onBack, onCreate = viewModel::createLevy)
}

@Composable
fun AddExpenseRoute(
    onBack: () -> Unit,
    viewModel: FinanceViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var receipt by rememberSaveable { mutableStateOf<String?>(null) }
    val pickReceipt = rememberMediaPicker(MediaKind.IMAGE) { receipt = it.uri }
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            viewModel.consumeSaved()
            onBack()
        }
    }
    AddExpenseScreen(
        uiState = uiState,
        pickedReceipt = receipt,
        onPickReceipt = pickReceipt,
        onRemoveReceipt = { receipt = null },
        onBack = onBack,
        onSave = { title, amount, spentAt, note -> viewModel.addExpense(title, amount, spentAt, note, receipt) }
    )
}
