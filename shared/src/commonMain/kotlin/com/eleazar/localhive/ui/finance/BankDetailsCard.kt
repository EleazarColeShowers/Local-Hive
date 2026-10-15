package com.eleazar.localhive.ui.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.BankDetails
import com.eleazar.localhive.ui.util.formatDate

/**
 * Where to send dues. The treasurer can edit it, or add it if there's none yet; for anyone else
 * it shows only once there's something to show. Who last changed it is always on view, so a
 * switch to a different account can't go unnoticed.
 */
@Composable
fun BankDetailsCard(
    bank: BankDetails?,
    isTreasurer: Boolean,
    onCopyAccount: (String) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (bank == null && !isTreasurer) return
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    "Pay dues to",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                if (isTreasurer) {
                    TextButton(onClick = onEdit) { Text(if (bank == null) "Add" else "Edit") }
                }
            }
            if (bank == null) {
                Text(
                    "Add the estate's bank account so residents know where to send their transfers.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(bank.accountNumber, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("${bank.bankName} · ${bank.accountName}", fontSize = 14.sp)
                }
                IconButton(onClick = { onCopyAccount(bank.accountNumber) }) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy account number")
                }
            }
            if (bank.updatedAt > 0) {
                Text(
                    "Set by ${bank.updatedByName.ifBlank { "the treasurer" }} on ${formatDate(bank.updatedAt, "d MMM yyyy")}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BankDetailsDialog(
    bank: BankDetails?,
    isSaving: Boolean,
    onSave: (bankName: String, accountNumber: String, accountName: String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    var bankName by rememberSaveable { mutableStateOf(bank?.bankName.orEmpty()) }
    var accountNumber by rememberSaveable { mutableStateOf(bank?.accountNumber.orEmpty()) }
    var accountName by rememberSaveable { mutableStateOf(bank?.accountName.orEmpty()) }
    var confirmRemove by rememberSaveable { mutableStateOf(false) }
    val complete = bankName.isNotBlank() && accountName.isNotBlank() && accountNumber.length in 6..20

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Estate bank account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Every member will see this, along with your name as the person who set it.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it.take(60) },
                    label = { Text("Bank") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it.filter(Char::isDigit).take(20) },
                    label = { Text("Account number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it.take(80) },
                    label = { Text("Account name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (bank != null) {
                    OutlinedButton(
                        onClick = { if (confirmRemove) { onRemove(); onDismiss() } else confirmRemove = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text(if (confirmRemove) "Yes, remove them" else "Remove bank details") }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(bankName, accountNumber, accountName); onDismiss() }, enabled = complete && !isSaving) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
