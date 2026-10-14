package com.eleazar.localhive.ui.finance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.PaymentStatus
import com.eleazar.localhive.ui.components.Avatar
import com.eleazar.localhive.ui.util.formatDate
import com.eleazar.localhive.ui.util.formatNaira

private enum class MemberFilter(val label: String) { ALL("All"), REVIEW("To review"), UNPAID("Unpaid"), PAID("Paid") }

/**
 * One levy. A resident sees what they owe and sends proof; the treasurer also sees every
 * member's standing, checks proof, records cash payments and sends reminders.
 *
 * @param onSendProof the route opens its photo picker and uploads what's picked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevyDetailScreen(
    uiState: FinanceUiState,
    onBack: () -> Unit,
    onSendProof: () -> Unit,
    onConfirm: (MemberPayment) -> Unit,
    onReject: (MemberPayment) -> Unit,
    onMarkPaid: (MemberPayment) -> Unit,
    onRemind: () -> Unit,
    onDelete: () -> Unit,
    onMessageShown: () -> Unit,
    onErrorShown: () -> Unit,
) {
    val levy = uiState.levy
    val snackbar = remember { SnackbarHostState() }
    var reviewing by remember { mutableStateOf<MemberPayment?>(null) }
    var markingPaid by remember { mutableStateOf<MemberPayment?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { snackbar.showSnackbar(it); onMessageShown() }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbar.showSnackbar(it); onErrorShown() }
    }

    reviewing?.let { member ->
        AlertDialog(
            onDismissRequest = { reviewing = null },
            title = { Text("Proof from ${member.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    levy?.let { Text("${it.title} · ${formatNaira(it.amount)}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    member.payment?.proofUrl?.let { ProofImage(url = it, contentDescription = "Proof of payment") }
                    Text("Check the amount and name match your account before confirming.", fontSize = 14.sp)
                }
            },
            confirmButton = {
                Button(onClick = { onConfirm(member); reviewing = null }) { Text("Confirm paid") }
            },
            dismissButton = {
                TextButton(
                    onClick = { onReject(member); reviewing = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Not accepted") }
            }
        )
    }

    markingPaid?.let { member ->
        AlertDialog(
            onDismissRequest = { markingPaid = null },
            title = { Text("Mark ${member.name} as paid?") },
            text = { Text("Use this for cash, or a transfer you've already seen. ${member.name} will be told it's confirmed.") },
            confirmButton = { Button(onClick = { onMarkPaid(member); markingPaid = null }) { Text("Mark paid") } },
            dismissButton = { TextButton(onClick = { markingPaid = null }) { Text("Cancel") } }
        )
    }

    if (confirmDelete && levy != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${levy.title}?") },
            text = { Text("The levy and every payment recorded against it will be removed for everyone. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { confirmDelete = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Levy", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (uiState.isTreasurer && levy != null) {
                        Box {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Delete levy", color = MaterialTheme.colorScheme.error) },
                                    onClick = { showMenu = false; confirmDelete = true }
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
            levy == null && uiState.levyMissing -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("This levy has been removed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            levy == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item(key = "summary") {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(levy.title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        Text(formatNaira(levy.amount), fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        if (levy.dueDate > 0) {
                            Text(
                                "Due ${formatDate(levy.dueDate, "EEEE, d MMMM yyyy")}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        if (levy.note.isNotBlank()) {
                            Text(levy.note, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }

                if (!uiState.isTreasurer) {
                    item(key = "mine") {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 12.dp))
                        MyPayment(uiState, onSendProof)
                    }
                } else {
                    item(key = "progress") {
                        TreasurerSummary(uiState, onRemind)
                    }
                    item(key = "members") {
                        MemberFilterRow(uiState, onReview = { reviewing = it }, onMarkPaid = { markingPaid = it })
                    }
                }
            }
        }
    }
}

@Composable
private fun MyPayment(uiState: FinanceUiState, onSendProof: () -> Unit) {
    val payment = uiState.myPayment
    val status = payment?.paymentStatus
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Your payment", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        PaymentStatusLabel(status, modifier = Modifier.padding(top = 2.dp))
        Text(
            when (status) {
                null -> "Pay the treasurer by transfer or cash, then send a screenshot of the transfer here so they can confirm it."
                PaymentStatus.PENDING -> "You sent proof ${formatDate(payment.submittedAt, "d MMM")}. The treasurer will confirm it."
                PaymentStatus.REJECTED -> "The treasurer couldn't confirm the proof you sent. Check the transfer and send it again."
                PaymentStatus.PAID -> "Confirmed ${formatDate(payment.reviewedAt.takeIf { it > 0 } ?: payment.submittedAt, "d MMM yyyy")}. Thank you!"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        payment?.proofUrl?.let { ProofImage(url = it, contentDescription = "Your proof of payment") }
        if (status != PaymentStatus.PAID) {
            if (uiState.isSaving) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Sending…")
                }
            } else {
                Button(
                    onClick = onSendProof,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (payment == null) "Send proof of payment" else "Send new proof")
                }
            }
        }
    }
}

@Composable
private fun TreasurerSummary(uiState: FinanceUiState, onRemind: () -> Unit) {
    val rows = uiState.memberPayments
    val paid = rows.count { it.status == PaymentStatus.PAID }
    val review = rows.count { it.status == PaymentStatus.PENDING }
    val owing = rows.count { it.status != PaymentStatus.PAID }
    val collected = rows.filter { it.status == PaymentStatus.PAID }.sumOf { it.payment?.amount ?: 0L }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        LinearProgressIndicator(
            progress = { if (rows.isEmpty()) 0f else paid.toFloat() / rows.size },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            drawStopIndicator = {}
        )
        Text(
            buildString {
                append("$paid of ${rows.size} paid · ${formatNaira(collected)} collected")
                if (review > 0) append(" · $review to review")
            },
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        OutlinedButton(
            onClick = onRemind,
            enabled = owing > 0 && !uiState.isSaving,
            modifier = Modifier.padding(top = 12.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Remind unpaid ($owing)")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberFilterRow(
    uiState: FinanceUiState,
    onReview: (MemberPayment) -> Unit,
    onMarkPaid: (MemberPayment) -> Unit
) {
    var filter by rememberSaveable { mutableStateOf(MemberFilter.ALL) }
    Column {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MemberFilter.entries.forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { filter = option },
                    label = { Text(option.label) }
                )
            }
        }
        val shown = uiState.memberPayments.filter {
            when (filter) {
                MemberFilter.ALL -> true
                MemberFilter.REVIEW -> it.status == PaymentStatus.PENDING
                MemberFilter.UNPAID -> it.status == null || it.status == PaymentStatus.REJECTED
                MemberFilter.PAID -> it.status == PaymentStatus.PAID
            }
        }
        if (shown.isEmpty()) {
            Text(
                "Nobody here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
        shown.forEach { member ->
            MemberPaymentRow(member, isMe = member.userId == uiState.currentUserId, onReview = onReview, onMarkPaid = onMarkPaid)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 68.dp))
        }
    }
}

@Composable
private fun MemberPaymentRow(
    member: MemberPayment,
    isMe: Boolean,
    onReview: (MemberPayment) -> Unit,
    onMarkPaid: (MemberPayment) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = member.status == PaymentStatus.PENDING) { onReview(member) }
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(imageUrl = member.photoUrl, name = member.name, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(if (isMe) "${member.name} (you)" else member.name, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            PaymentStatusLabel(member.status)
        }
        when (member.status) {
            PaymentStatus.PENDING -> FilledTonalButton(onClick = { onReview(member) }) { Text("Review") }
            PaymentStatus.PAID -> Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Paid",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp)
            )
            else -> TextButton(onClick = { onMarkPaid(member) }) { Text("Mark paid") }
        }
    }
}
