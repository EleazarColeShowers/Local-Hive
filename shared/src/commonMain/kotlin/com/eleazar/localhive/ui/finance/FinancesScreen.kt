package com.eleazar.localhive.ui.finance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.Expense
import com.eleazar.localhive.domain.model.PaymentStatus
import com.eleazar.localhive.ui.components.EmptyState
import com.eleazar.localhive.ui.util.formatDate
import com.eleazar.localhive.ui.util.formatNaira

/**
 * The estate's money: what's owed (levies) and what's been spent (expenses). Everyone sees
 * both; only the treasurer can add to them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancesScreen(
    uiState: FinanceUiState,
    onBack: () -> Unit,
    onLevyClick: (levyId: String) -> Unit,
    onCreateLevy: () -> Unit,
    onAddExpense: () -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onStatement: () -> Unit,
    onCopyAccount: (String) -> Unit,
    onSaveBank: (bankName: String, accountNumber: String, accountName: String) -> Unit,
    onRemoveBank: () -> Unit,
    onMessageShown: () -> Unit,
    onErrorShown: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var openExpense by remember { mutableStateOf<Expense?>(null) }
    var editingBank by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbar.showSnackbar(it)
            onMessageShown()
        }
    }
    // Otherwise a list that failed to load would pass for an empty one.
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbar.showSnackbar(it)
            onErrorShown()
        }
    }

    openExpense?.let { expense ->
        ExpenseDialog(
            expense = expense,
            canDelete = uiState.isTreasurer,
            onDelete = {
                onDeleteExpense(expense)
                openExpense = null
            },
            onDismiss = { openExpense = null }
        )
    }

    if (editingBank) {
        BankDetailsDialog(
            bank = uiState.bank,
            isSaving = uiState.isSaving,
            onSave = onSaveBank,
            onRemove = onRemoveBank,
            onDismiss = { editingBank = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Dues & expenses", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Only the treasurer can read every payment, so only they get the full books.
                    if (uiState.isTreasurer) {
                        TextButton(onClick = onStatement) {
                            Icon(Icons.Outlined.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Statement")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            if (uiState.isTreasurer) {
                ExtendedFloatingActionButton(
                    onClick = if (tab == 0) onCreateLevy else onAddExpense,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(if (tab == 0) "New levy" else "Add expense") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TreasurerLine(uiState)
            BankDetailsCard(
                bank = uiState.bank,
                isTreasurer = uiState.isTreasurer,
                onCopyAccount = onCopyAccount,
                onEdit = { editingBank = true },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
            TabRow(
                selectedTabIndex = tab,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                val idle = MaterialTheme.colorScheme.onSurfaceVariant
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Dues") }, unselectedContentColor = idle)
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Expenses") }, unselectedContentColor = idle)
            }
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                tab == 0 -> LevyList(uiState, onLevyClick)
                else -> ExpenseList(uiState, onExpenseClick = { openExpense = it })
            }
        }
    }
}

@Composable
private fun TreasurerLine(uiState: FinanceUiState) {
    val text = when {
        uiState.isTreasurer -> "You're the estate's treasurer"
        uiState.treasurerName != null -> "Treasurer: ${uiState.treasurerName}"
        else -> "No treasurer yet. An admin can appoint one from the admin panel."
    }
    Text(
        text,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun LevyList(uiState: FinanceUiState, onLevyClick: (String) -> Unit) {
    if (uiState.levies.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.AccountBalanceWallet,
            title = "No dues yet",
            message = if (uiState.isTreasurer) "Create a levy, like a monthly security charge, and track who has paid."
            else "Levies your treasurer sets will show here, with what you owe."
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        items(uiState.levies, key = { it.levy.id }) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLevyClick(row.levy.id) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        row.levy.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        buildString {
                            append(formatNaira(row.levy.amount))
                            if (row.levy.dueDate > 0) append(" · Due ${formatDate(row.levy.dueDate, "d MMM")}")
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                if (uiState.isTreasurer) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${row.paidCount} of ${uiState.memberCount} paid",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (row.toReview > 0) {
                            Text(
                                "${row.toReview} to review",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = paymentColor(PaymentStatus.PENDING)
                            )
                        }
                    }
                } else {
                    PaymentStatusLabel(row.myStatus)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun ExpenseList(uiState: FinanceUiState, onExpenseClick: (Expense) -> Unit) {
    if (uiState.expenses.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.ReceiptLong,
            title = "No expenses yet",
            message = if (uiState.isTreasurer) "Log what the estate spends, with a receipt, so everyone can see where the money goes."
            else "What the treasurer spends on the estate will show here."
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        items(uiState.expenses, key = { it.id }) { expense ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpenseClick(expense) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(expense.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            formatDate(expense.spentAt, "d MMM yyyy"),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (expense.receiptUrl != null) {
                            Text(" · ", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(
                                Icons.Outlined.ReceiptLong,
                                contentDescription = "Has a receipt",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
                Text(formatNaira(expense.amount), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun ExpenseDialog(expense: Expense, canDelete: Boolean, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(expense.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(formatNaira(expense.amount), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(formatDate(expense.spentAt, "EEEE, d MMMM yyyy"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (expense.note.isNotBlank()) Text(expense.note)
                expense.receiptUrl?.let { ProofImage(url = it, contentDescription = "Receipt") }
                if (confirmDelete) {
                    Text(
                        "Delete this expense? Everyone will stop seeing it.",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = {
            if (canDelete) {
                TextButton(
                    onClick = { if (confirmDelete) onDelete() else confirmDelete = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(if (confirmDelete) "Yes, delete" else "Delete") }
            }
        }
    )
}
