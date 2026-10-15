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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.Levy
import com.eleazar.localhive.domain.model.LevyPayment
import com.eleazar.localhive.ui.util.formatDate
import com.eleazar.localhive.ui.util.formatNaira

/** Everything a receipt says, so the dialog and the shared text can't disagree. */
private fun receiptLines(estateName: String, levy: Levy, payment: LevyPayment, payerName: String): List<Pair<String, String>> =
    buildList {
        add("Receipt no." to payment.receiptNumber)
        add("Received from" to payerName)
        add("For" to levy.title)
        add("Date" to formatDate(payment.receivedAt, "d MMMM yyyy"))
        // Without proof, the treasurer recorded it themselves: cash, or a transfer they saw.
        add("Method" to if (payment.proofUrl != null) "Bank transfer" else "Recorded by the treasurer")
        if (payment.confirmedByName.isNotBlank()) add("Confirmed by" to "${payment.confirmedByName}, treasurer")
        if (estateName.isNotBlank()) add("Estate" to estateName)
    }

/** A receipt as plain text, to send on WhatsApp or keep. */
fun receiptText(estateName: String, levy: Levy, payment: LevyPayment, payerName: String): String = buildString {
    appendLine("PAYMENT RECEIPT")
    appendLine(formatNaira(payment.amount))
    appendLine()
    receiptLines(estateName, levy, payment, payerName).forEach { (label, value) -> appendLine("$label: $value") }
    appendLine()
    append("Issued through LocalHive")
}

/** The receipt for a confirmed payment, for the resident who paid or the treasurer. */
@Composable
fun ReceiptDialog(
    estateName: String,
    levy: Levy,
    payment: LevyPayment,
    payerName: String,
    onShare: (text: String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Payment receipt", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatNaira(payment.amount), fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                receiptLines(estateName, levy, payment, payerName).forEach { (label, value) ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.weight(0.42f))
                        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(0.58f))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 4.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = { onShare(receiptText(estateName, levy, payment, payerName)) }) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text("Share")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
