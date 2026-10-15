package com.eleazar.localhive.ui.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.ui.util.formatDate
import com.eleazar.localhive.ui.util.formatNaira
import com.eleazar.localhive.ui.util.nowMillis

/**
 * The treasurer's books: what's in hand overall, then one month at a time, money in and out,
 * with either exportable as a spreadsheet for the estate meeting.
 *
 * @param onExport gets a file name and the CSV to share
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementScreen(
    uiState: FinanceUiState,
    onBack: () -> Unit,
    onExport: (fileName: String, csv: String) -> Unit,
    onErrorShown: () -> Unit,
) {
    val entries = uiState.statement
    val months = remember(entries) { statementMonths(entries, nowMillis()) }
    var monthIndex by rememberSaveable { mutableIntStateOf(0) }
    val index = monthIndex.coerceIn(0, months.lastIndex)
    val (monthKey, monthMoment) = months[index]
    val monthEntries = remember(entries, monthKey) { entries.filter { monthKey(it.at) == monthKey } }
    val monthLabel = formatDate(monthMoment, "MMMM yyyy")
    var showExport by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbar.showSnackbar(it); onErrorShown() }
    }

    fun export(all: Boolean) {
        val slug = uiState.estateName.lowercase().filter { it.isLetterOrDigit() || it == ' ' }.trim().replace(' ', '-')
            .ifBlank { "estate" }
        if (all) onExport("$slug-statement-all.csv", statementCsv(entries, uiState.estateName, "All time"))
        else onExport("$slug-statement-$monthKey.csv", statementCsv(monthEntries, uiState.estateName, monthLabel))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Statement", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (uiState.isTreasurer) {
                        Box {
                            IconButton(onClick = { showExport = true }) {
                                Icon(Icons.Outlined.FileDownload, contentDescription = "Export")
                            }
                            DropdownMenu(expanded = showExport, onDismissRequest = { showExport = false }) {
                                DropdownMenuItem(
                                    text = { Text("Export $monthLabel (CSV)") },
                                    onClick = { showExport = false; export(all = false) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export everything (CSV)") },
                                    onClick = { showExport = false; export(all = true) }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        when {
            !uiState.isTreasurer && !uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Only the treasurer can see the full statement.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item(key = "balance") { BalanceCard(entries.totals()) }
                item(key = "month") {
                    MonthHeader(
                        label = monthLabel,
                        totals = monthEntries.totals(),
                        canGoBack = index < months.lastIndex,
                        canGoForward = index > 0,
                        onBack = { monthIndex = index + 1 },
                        onForward = { monthIndex = index - 1 }
                    )
                }
                if (monthEntries.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            "Nothing came in or went out in $monthLabel.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                items(monthEntries, key = { it.id }) { entry ->
                    EntryRow(entry)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(totals: StatementTotals) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Balance", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                formatNaira(totals.balance),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = if (totals.balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "${formatNaira(totals.collected)} collected · ${formatNaira(totals.spent)} spent, all time",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun MonthHeader(
    label: String,
    totals: StatementTotals,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            IconButton(onClick = onBack, enabled = canGoBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Earlier month")
            }
            Text(
                label,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = onForward, enabled = canGoForward) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Later month")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MonthFigure("In", formatNaira(totals.collected))
            MonthFigure("Out", formatNaira(totals.spent))
            MonthFigure("Net", formatNaira(totals.balance))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun MonthFigure(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EntryRow(entry: StatementEntry) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.title, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                buildString {
                    append(formatDate(entry.at, "d MMM"))
                    append(if (entry.isIncome) " · ${entry.member.ifBlank { "Member" }}" else " · Expense")
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            (if (entry.isIncome) "+" else "−") + formatNaira(kotlin.math.abs(entry.amount)),
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = if (entry.isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}
