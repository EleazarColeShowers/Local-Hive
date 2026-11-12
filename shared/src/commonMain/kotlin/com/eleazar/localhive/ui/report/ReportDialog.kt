package com.eleazar.localhive.ui.report

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.data.repository.MAX_REPORT_DETAILS
import com.eleazar.localhive.domain.model.ReportReason

/**
 * Asks why something is being reported, with room for more detail, then thanks the user.
 * Also used for feedback from Settings, with [ReportReason.forFeedback] and [feedback] set.
 */
@Composable
fun ReportDialog(
    title: String,
    onSubmit: (ReportReason, details: String) -> Unit,
    onDismiss: () -> Unit,
    reasons: List<ReportReason> = ReportReason.forContent,
    feedback: Boolean = false
) {
    // Saved by key, since only strings and numbers are sure to save on every platform.
    var reasonKey by rememberSaveable { mutableStateOf<String?>(null) }
    val reason = reasons.firstOrNull { it.key == reasonKey }
    var details by rememberSaveable { mutableStateOf("") }
    var sent by rememberSaveable { mutableStateOf(false) }

    if (sent) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(if (feedback) "Thanks for your feedback" else "Thanks for reporting this", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    buildString {
                        append(
                            if (feedback) "We read everything that's sent and will get back to you by email if we need to."
                            else "We review every report and remove anything that breaks our rules. The person won't know it was you."
                        )
                        if (reason == ReportReason.CHILD_SAFETY || reason == ReportReason.THREATS || reason == ReportReason.SAFETY) {
                            append("\n\nIf anyone is in immediate danger, call the police first.")
                        }
                    }
                )
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Done", fontWeight = FontWeight.Bold) } }
        )
        return
    }

    val needsDetails = feedback || reason == ReportReason.OTHER
    val canSend = reason != null && (!needsDetails || details.isNotBlank())
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    if (feedback) "What would you like to tell us?" else "Why are you reporting this?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Column(Modifier.selectableGroup()) {
                    reasons.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = reason == option,
                                    onClick = { reasonKey = option.key },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = reason == option, onClick = null)
                            Text(option.label, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = details,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    onValueChange = { details = it.take(MAX_REPORT_DETAILS) },
                    label = { Text(if (needsDetails) "Tell us more" else "Anything else? (optional)") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    reason?.let { onSubmit(it, details.trim()) }
                    sent = true
                },
                enabled = canSend
            ) { Text(if (feedback) "Send" else "Report", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
