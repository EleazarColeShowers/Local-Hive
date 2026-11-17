package com.eleazar.localhive.ui.finance

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.FinanceLogEntry
import com.eleazar.localhive.ui.components.EmptyState
import com.eleazar.localhive.ui.util.formatDate

/**
 * Everything the treasurer has done to the estate's books, for every member to see: levies set
 * and deleted, payments confirmed or taken back, spending, and bank detail changes. Nobody can
 * edit or delete an entry, the treasurer included.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceLogScreen(uiState: FinanceUiState, onBack: () -> Unit, onErrorShown: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) { uiState.error?.let { snackbar.showSnackbar(it); onErrorShown() } }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Activity log", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        when {
            uiState.isLoading && uiState.financeLog.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.financeLog.isEmpty() -> EmptyState(
                icon = Icons.Outlined.History,
                title = "Nothing yet",
                message = "Every change the treasurer makes to the estate's money will be listed here, for everyone to see.",
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {
                item {
                    Text(
                        "Kept automatically. Nobody can change or remove an entry, the treasurer included.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(uiState.financeLog, key = { it.id }) { entry -> LogRow(entry) }
            }
        }
    }
}

@Composable
private fun LogRow(entry: FinanceLogEntry) {
    val warning = entry.action in setOf("payment_undone", "levy_deleted", "expense_deleted", "bank_changed", "bank_removed")
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Icon(
            logIcon(entry.action),
            contentDescription = null,
            tint = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.summary, fontSize = 15.sp)
            Text(
                listOf(entry.byName.ifBlank { "Treasurer" }, formatDate(entry.at, "d MMM yyyy, h:mm a")).joinToString(" · "),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 48.dp))
}

private fun logIcon(action: String): ImageVector = when (action) {
    "payment_confirmed", "payment_recorded" -> Icons.Outlined.CheckCircle
    "payment_undone", "payment_rejected" -> Icons.Outlined.Undo
    "expense_added" -> Icons.Outlined.ReceiptLong
    "levy_deleted", "expense_deleted" -> Icons.Outlined.Delete
    "bank_changed", "bank_removed" -> Icons.Outlined.AccountBalance
    else -> Icons.Outlined.Payments
}
