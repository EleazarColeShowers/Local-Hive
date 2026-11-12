package com.eleazar.localhive.ui.estate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eleazar.localhive.domain.model.Guard
import com.eleazar.localhive.domain.model.GuardInvite
import com.eleazar.localhive.ui.components.LocalHiveButton
import com.eleazar.localhive.ui.platform.rememberPlatformActions
import com.eleazar.localhive.ui.util.clockTime
import com.eleazar.localhive.ui.util.formatDate
import org.koin.compose.viewmodel.koinViewModel

/** The admin panel's Guards tab, with its own ViewModel. */
@Composable
internal fun GuardsTabRoute(
    estateId: String,
    estateName: String,
    dark: Color,
    green: Color,
    cardBg: Color,
    viewModel: GuardsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val platform = rememberPlatformActions()
    LaunchedEffect(estateId) { viewModel.load(estateId) }

    GuardsTab(
        guards = uiState.guards,
        isCreatingInvite = uiState.isCreatingInvite,
        invite = uiState.invite,
        error = uiState.error,
        estateName = estateName,
        dark = dark,
        green = green,
        cardBg = cardBg,
        onCreateInvite = { viewModel.createInvite(estateId) },
        onDismissInvite = viewModel::dismissInvite,
        onCopyInvite = { code -> platform.copyToClipboard("Guard code", code, "Code copied!") },
        onShareInvite = { code ->
            platform.shareText(
                "Your LocalHive guard code for $estateName: $code\n" +
                    "Sign up or log in to LocalHive, tap \"I'm a security guard\" and enter the code. It works once, within a day.",
                "Share guard code"
            )
        },
        onRemove = { guardId -> viewModel.removeGuard(estateId, guardId) }
    )
}

@Composable
private fun GuardsTab(
    guards: List<Guard>,
    isCreatingInvite: Boolean,
    invite: GuardInvite?,
    error: String?,
    estateName: String,
    dark: Color,
    green: Color,
    cardBg: Color,
    onCreateInvite: () -> Unit,
    onDismissInvite: () -> Unit,
    onCopyInvite: (String) -> Unit,
    onShareInvite: (String) -> Unit,
    onRemove: (guardId: String) -> Unit
) {
    var confirmRemove by remember { mutableStateOf<Guard?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "intro") {
            Text(
                "Guards check visitors in and out at the gate. They don't join $estateName: they can't see the " +
                    "feed, chats, dues or the member list. They see one visitor pass at a time, when the visitor gives them its code, " +
                    "and who is inside right now.",
                fontSize = 14.sp,
                color = dark.copy(alpha = 0.7f)
            )
        }
        item(key = "add") {
            LocalHiveButton(
                text = "Add a guard",
                onClick = onCreateInvite,
                isLoading = isCreatingInvite,
                modifier = Modifier.fillMaxWidth()
            )
        }
        error?.let { item(key = "error") { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) } }
        item(key = "label") {
            Text(
                if (guards.isEmpty()) "No guards yet" else "Guards (${guards.size})",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = dark,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        items(guards, key = { it.userId }) { guard ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = cardBg,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(guard.name.ifBlank { "Guard" }, fontWeight = FontWeight.SemiBold, color = dark)
                        if (guard.addedAt > 0) {
                            Text(
                                "Since ${formatDate(guard.addedAt, "MMM d, yyyy")}",
                                fontSize = 13.sp,
                                color = dark.copy(alpha = 0.6f)
                            )
                        }
                    }
                    TextButton(onClick = { confirmRemove = guard }) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    invite?.let { code ->
        AlertDialog(
            onDismissRequest = onDismissInvite,
            title = { Text("Guard code") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Give this to the guard. They sign up or log in, tap \"I'm a security guard\" and enter it. " +
                            "It works once, until ${clockTime(code.expiresAt)} ${formatDate(code.expiresAt, "MMM d")}.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        code.code,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        letterSpacing = 4.sp,
                        color = green
                    )
                    Spacer(Modifier.height(16.dp))
                    LocalHiveButton(text = "Share", onClick = { onShareInvite(code.code) }, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = { onCopyInvite(code.code) }) { Text("Copy code") }
                }
            },
            confirmButton = { TextButton(onClick = onDismissInvite) { Text("Done") } }
        )
    }

    confirmRemove?.let { guard ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text("Remove ${guard.name.ifBlank { "this guard" }}?") },
            text = { Text("They'll lose access to the gate straight away. You can add them again with a new code.") },
            confirmButton = {
                TextButton(onClick = { onRemove(guard.userId); confirmRemove = null }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = null }) { Text("Cancel") } }
        )
    }
}
