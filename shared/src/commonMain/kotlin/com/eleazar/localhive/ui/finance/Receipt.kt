package com.eleazar.localhive.ui.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.Levy
import com.eleazar.localhive.domain.model.LevyPayment
import com.eleazar.localhive.domain.model.FlatPayment
import com.eleazar.localhive.domain.model.flatSummary
import com.eleazar.localhive.ui.util.formatDate
import com.eleazar.localhive.ui.util.formatNaira

/** What a receipt is for: one member's payment, or one payment covering some flats. */
data class ReceiptDetails(
    val number: String,
    val amount: Long,
    val payerName: String,
    val levyTitle: String,
    val receivedAt: Long,
    /** Whether a transfer was proved with a screenshot, rather than recorded by the treasurer. */
    val byTransfer: Boolean,
    val confirmedByName: String,
    /** The flats paid for, on a per-flat levy. */
    val covers: String = ""
)

fun memberReceipt(levy: Levy, payment: LevyPayment, payerName: String) = ReceiptDetails(
    number = payment.receiptNumber,
    amount = payment.amount,
    payerName = payerName,
    levyTitle = levy.title,
    receivedAt = payment.receivedAt,
    byTransfer = payment.proofUrl != null,
    confirmedByName = payment.confirmedByName
)

/** The receipt for every flat one proof (or one cash payment) covered. */
fun flatReceipt(levy: Levy, payments: List<FlatPayment>) = ReceiptDetails(
    number = flatReceiptNumber(levy.id, payments.first().groupId),
    amount = payments.sumOf { it.amount },
    payerName = payments.first().payerName.ifBlank { "Paid in cash" },
    levyTitle = levy.title,
    receivedAt = payments.maxOf { it.receivedAt },
    byTransfer = payments.first().payerId.isNotBlank(),
    confirmedByName = payments.first().confirmedByName,
    covers = flatSummary(payments)
)

/** Everything a receipt says, so the dialog and the shared text can't disagree. */
private fun receiptLines(estateName: String, receipt: ReceiptDetails): List<Pair<String, String>> =
    buildList {
        add("Receipt no." to receipt.number)
        add("Received from" to receipt.payerName)
        add("For" to receipt.levyTitle)
        if (receipt.covers.isNotBlank()) add("Covers" to receipt.covers)
        add("Date" to formatDate(receipt.receivedAt, "d MMMM yyyy"))
        // Without proof, the treasurer recorded it themselves: cash, or a transfer they saw.
        add("Method" to if (receipt.byTransfer) "Bank transfer" else "Recorded by the treasurer")
        if (receipt.confirmedByName.isNotBlank()) add("Confirmed by" to "${receipt.confirmedByName}, treasurer")
        if (estateName.isNotBlank()) add("Estate" to estateName)
    }

/** A receipt as plain text, to send on WhatsApp or keep. */
fun receiptText(estateName: String, receipt: ReceiptDetails): String = buildString {
    appendLine("PAYMENT RECEIPT")
    appendLine(formatNaira(receipt.amount))
    appendLine()
    receiptLines(estateName, receipt).forEach { (label, value) -> appendLine("$label: $value") }
    appendLine()
    append("Issued through LocalHive")
}

/** The receipt for a confirmed payment, for the resident who paid or the treasurer. */
@Composable
fun ReceiptDialog(
    estateName: String,
    receipt: ReceiptDetails,
    onShare: (text: String) -> Unit,
    onDismiss: () -> Unit,
    /** The treasurer only: taking back a payment confirmed by mistake. */
    onTakeBack: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Payment receipt", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatNaira(receipt.amount), fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                receiptLines(estateName, receipt).forEach { (label, value) ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.weight(0.42f))
                        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(0.58f))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 4.dp))
                onTakeBack?.let {
                    TextButton(
                        onClick = it,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Take back this payment") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onShare(receiptText(estateName, receipt)) }) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text("Share")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

/**
 * Taking back a confirmed payment: for a transfer that bounced, or one confirmed by mistake. The
 * reason is required, and goes in the finance log for every member to see.
 */
@Composable
fun TakeBackDialog(who: String, onConfirm: (reason: String) -> Unit, onDismiss: () -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Take back $who's payment?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("It will show as unpaid again. Every member can see this in the activity log, with your reason.")
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(200) },
                    label = { Text("Reason") },
                    placeholder = { Text("e.g. The transfer was reversed") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(reason.trim()) },
                enabled = reason.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Take back") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
