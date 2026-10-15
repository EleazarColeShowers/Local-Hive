package com.eleazar.localhive.ui.estate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.Estate
import com.eleazar.localhive.domain.model.JoinMode

/** The estate itself: its name and address, how people join, and its invite code. */
@Composable
internal fun EstateSettingsTab(
    estate: Estate?,
    inviteCode: String,
    isSaving: Boolean,
    onSaveDetails: (name: String, address: String) -> Unit,
    onSetJoinMode: (JoinMode) -> Unit,
    onRegenerateCode: () -> Unit,
    onCopyCode: (String) -> Unit,
    dark: Color,
    green: Color,
    cardBg: Color
) {
    if (estate == null) return
    var confirmRegenerate by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "details") {
            SettingsCard(title = "Estate details", dark = dark, cardBg = cardBg) {
                // Keyed on the saved values, so the fields follow a change another admin makes.
                var name by rememberSaveable(estate.name) { mutableStateOf(estate.name) }
                var address by rememberSaveable(estate.address) { mutableStateOf(estate.address) }
                val changed = name.trim() != estate.name || address.trim() != estate.address
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it.take(200) },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onSaveDetails(name, address) },
                    enabled = changed && name.isNotBlank() && !isSaving,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = green)
                ) { Text("Save details", color = Color.White) }
            }
        }

        item(key = "join") {
            SettingsCard(title = "How people join", dark = dark, cardBg = cardBg) {
                JoinMode.entries.forEach { mode ->
                    val selected = estate.joinMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected,
                                enabled = !isSaving,
                                role = Role.RadioButton,
                                onClick = { if (!selected) onSetJoinMode(mode) }
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = green)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mode.label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = dark)
                            Text(mode.description, fontSize = 13.sp, color = dark.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }

        item(key = "code") {
            SettingsCard(title = "Invite code", dark = dark, cardBg = cardBg) {
                if (!estate.joinMode.acceptsCodes) {
                    Text(
                        "Codes are off while the estate takes requests only. The code below starts working again " +
                            "if you switch back.",
                        fontSize = 13.sp,
                        color = dark.copy(alpha = 0.6f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        inviteCode.ifBlank { "——————" },
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp,
                        color = dark,
                        modifier = Modifier.weight(1f)
                    )
                    if (inviteCode.isNotBlank()) {
                        IconButton(onClick = { onCopyCode(inviteCode) }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy code", tint = green)
                        }
                    }
                }
                Text(
                    "If the code has been shared somewhere it shouldn't have, make a new one. The old code stops working " +
                        "straight away; members already in the estate aren't affected.",
                    fontSize = 13.sp,
                    color = dark.copy(alpha = 0.6f)
                )
                OutlinedButton(
                    onClick = { confirmRegenerate = true },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = green)
                    Spacer(Modifier.width(8.dp))
                    Text("Make a new code", color = green)
                }
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }

    if (confirmRegenerate) {
        AlertDialog(
            onDismissRequest = { confirmRegenerate = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Make a new invite code?", fontWeight = FontWeight.Bold) },
            text = { Text("$inviteCode will stop working for anyone who hasn't used it yet. Share the new code with people you still want to invite.") },
            confirmButton = {
                TextButton(onClick = { confirmRegenerate = false; onRegenerateCode() }) {
                    Text("Make new code", color = green, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmRegenerate = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    dark: Color,
    cardBg: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = dark)
            content()
        }
    }
}
