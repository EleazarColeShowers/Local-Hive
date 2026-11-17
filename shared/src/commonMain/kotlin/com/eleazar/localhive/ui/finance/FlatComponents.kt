package com.eleazar.localhive.ui.finance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.Building
import com.eleazar.localhive.domain.model.FlatPayment
import com.eleazar.localhive.domain.model.PaymentStatus
import com.eleazar.localhive.domain.model.Residence
import com.eleazar.localhive.domain.model.flatSummary
import com.eleazar.localhive.ui.util.formatNaira

/**
 * Picking a building, then a flat in it. Used by members saying where they live, and by the
 * treasurer or an admin correcting someone's pick.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomePickerDialog(
    buildings: List<Building>,
    current: Residence?,
    title: String,
    onPick: (Building, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var building by remember { mutableStateOf(buildings.firstOrNull { it.id == current?.buildingId }) }
    var flat by remember { mutableStateOf(current?.flat?.takeIf { it > 0 }) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (buildings.isEmpty()) {
                    Text("Your treasurer hasn't added the estate's buildings yet.")
                    return@Column
                }
                Text("Building", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    buildings.forEach { b ->
                        FilterChip(
                            selected = building?.id == b.id,
                            onClick = {
                                building = b
                                if ((flat ?: 0) > b.flats) flat = null
                            },
                            label = { Text(b.name) }
                        )
                    }
                }
                building?.let { b ->
                    Text("Flat", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..b.flats).forEach { n ->
                            FilterChip(selected = flat == n, onClick = { flat = n }, label = { Text("$n") })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { val b = building; val f = flat; if (b != null && f != null) onPick(b, f) },
                enabled = building != null && flat != null
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** "Paid · Ada" and the like: where one flat stands, for a row. */
private fun flatStatusLine(row: FlatRow): String {
    val payment = row.payment
    return when (row.status) {
        PaymentStatus.PAID -> "Paid" + (payment?.payerName?.takeIf { it.isNotBlank() }?.let { " by $it" } ?: "")
        PaymentStatus.PENDING -> "Proof sent" + (payment?.payerName?.takeIf { it.isNotBlank() }?.let { " by $it" } ?: "") + ", waiting for the treasurer"
        PaymentStatus.REJECTED -> "Proof not accepted. Still owed"
        null -> "Unpaid"
    }
}

/**
 * A resident's view of a per-flat levy: the flats in their building, which have paid, ticks for
 * the ones they're paying for, and one proof for all of them.
 */
@Composable
fun MyFlatPayment(
    uiState: FinanceUiState,
    onPickHome: () -> Unit,
    onToggleFlat: (Int) -> Unit,
    onSendProof: () -> Unit,
    onCopyAccount: (String) -> Unit,
    onViewReceipt: (FlatPayment) -> Unit
) {
    val levy = uiState.levy ?: return
    val building = uiState.myBuilding
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Your building", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        if (building == null) {
            Text(
                "This levy is charged per flat. Say which flat you live in to see what your building owes.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            Button(onClick = onPickHome, shape = RoundedCornerShape(12.dp)) { Text("Pick your flat") }
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${building.name} · your flat is ${uiState.myResidence?.flat}", modifier = Modifier.weight(1f), fontSize = 15.sp)
            TextButton(onClick = onPickHome) { Text("Change") }
        }
        Text(
            "Anyone in the building can pay for any flat that's still owed. Tick the flats you're paying for.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        uiState.myBuildingFlats.forEach { row ->
            val open = row.status == null || row.status == PaymentStatus.REJECTED
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = open || row.status == PaymentStatus.PAID) {
                        if (open) onToggleFlat(row.flat) else row.payment?.let(onViewReceipt)
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (open) {
                    Checkbox(checked = row.flat in uiState.selectedFlats, onCheckedChange = { onToggleFlat(row.flat) })
                } else {
                    Spacer(Modifier.width(48.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    val mine = row.flat == uiState.myResidence?.flat
                    Text("Flat ${row.flat}" + if (mine) " (yours)" else "", fontWeight = FontWeight.Medium)
                    Text(flatStatusLine(row), fontSize = 13.sp, color = paymentColor(row.status))
                }
                if (row.status == PaymentStatus.PAID) {
                    IconButton(onClick = { row.payment?.let(onViewReceipt) }) {
                        Icon(Icons.Outlined.ReceiptLong, contentDescription = "Receipt", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        val count = uiState.selectedFlats.size
        if (uiState.myBuildingFlats.any { it.status == null || it.status == PaymentStatus.REJECTED }) {
            BankDetailsCard(bank = uiState.bank, isTreasurer = false, onCopyAccount = onCopyAccount, onEdit = {})
            if (uiState.isSaving) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Sending…")
                }
            } else {
                Button(
                    onClick = onSendProof,
                    enabled = count > 0,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (count == 0) "Tick the flats you're paying for"
                        else "Send proof for ${if (count == 1) "1 flat" else "$count flats"} · ${formatNaira(levy.amount * count)}"
                    )
                }
            }
        } else {
            Text("Every flat in ${building.name} has paid or sent proof. Thank you!", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        }
        // Proof the user sent, so they can see it went through.
        uiState.flatProofs.sortedByDescending { it.submittedAt }.firstOrNull()?.let { proof ->
            if (proof.proofUrl.isNotBlank()) ProofImage(url = proof.proofUrl, contentDescription = "Your latest proof of payment")
        }
    }
}

/** The treasurer's totals for a per-flat levy, and the reminder button. */
@Composable
fun FlatTreasurerSummary(uiState: FinanceUiState, onRemind: () -> Unit) {
    val total = uiState.totalFlats
    val paid = uiState.flatPayments.count { it.paymentStatus == PaymentStatus.PAID }
    val collected = uiState.flatPayments.filter { it.paymentStatus == PaymentStatus.PAID }.sumOf { it.amount }
    val review = uiState.proofsToReview.size
    val unpicked = (uiState.memberCount - uiState.residents.size).coerceAtLeast(0)
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else paid.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            drawStopIndicator = {}
        )
        Text(
            buildString {
                append("$paid of $total flats paid · ${formatNaira(collected)} collected")
                if (review > 0) append(" · $review to review")
            },
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (unpicked > 0) {
            Text(
                if (unpicked == 1) "1 member hasn't said which flat they live in." else "$unpicked members haven't said which flat they live in.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        OutlinedButton(
            onClick = onRemind,
            enabled = paid < total && !uiState.isSaving,
            modifier = Modifier.padding(top = 12.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Remind unpaid flats")
        }
    }
}

/** Proof waiting for the treasurer, one card per proof, however many flats it covers. */
@Composable
fun ProofsToReview(reviews: List<ProofToReview>, onReview: (ProofToReview) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("To review", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = paymentColor(PaymentStatus.PENDING))
        reviews.forEach { review ->
            OutlinedCard(onClick = { onReview(review) }, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(review.payerName, fontWeight = FontWeight.Medium)
                        Text(
                            "${flatSummary(review.payments)} · ${formatNaira(review.total)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(onClick = { onReview(review) }) { Text("Review") }
                }
            }
        }
    }
}

/** Every building on a per-flat levy, with each flat's standing, for the treasurer. */
@Composable
fun BuildingPaymentList(
    uiState: FinanceUiState,
    onMarkPaid: (FlatRow) -> Unit,
    onViewReceipt: (FlatPayment) -> Unit,
    onReview: (FlatRow) -> Unit
) {
    if (uiState.buildings.isEmpty()) {
        Text(
            "No buildings yet. Add them from Dues & expenses › Buildings so each flat can be charged.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
        return
    }
    var open by remember { mutableStateOf<String?>(null) }
    Column {
        uiState.buildingRows.forEach { row ->
            val expanded = open == row.building.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { open = if (expanded) null else row.building.id }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(row.building.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        buildString {
                            append("${row.paid} of ${row.building.flats} flats paid")
                            if (row.waiting > 0) append(" · ${row.waiting} waiting")
                        },
                        fontSize = 14.sp,
                        color = if (row.paid == row.building.flats) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(if (expanded) "Hide" else "Show", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            }
            if (expanded) {
                row.flats.forEach { flat ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Flat ${flat.flat}" + (flat.residents.takeIf { it.isNotEmpty() }?.joinToString(", ", prefix = " · ") ?: ""),
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            )
                            Text(flatStatusLine(flat), fontSize = 13.sp, color = paymentColor(flat.status))
                        }
                        when (flat.status) {
                            PaymentStatus.PENDING -> FilledTonalButton(onClick = { onReview(flat) }) { Text("Review") }
                            PaymentStatus.PAID -> IconButton(onClick = { flat.payment?.let(onViewReceipt) }) {
                                Icon(Icons.Outlined.ReceiptLong, contentDescription = "Receipt", tint = MaterialTheme.colorScheme.primary)
                            }
                            else -> TextButton(onClick = { onMarkPaid(flat) }) { Text("Mark paid") }
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
