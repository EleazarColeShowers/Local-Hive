package com.eleazar.localhive.ui.gate

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.GUARD_CODE_LENGTH
import com.eleazar.localhive.domain.model.PASS_CODE_LENGTH
import com.eleazar.localhive.domain.model.PassStage
import com.eleazar.localhive.domain.model.VisitorPass
import com.eleazar.localhive.domain.model.normalizeCode
import com.eleazar.localhive.ui.components.EmptyState
import com.eleazar.localhive.ui.components.LocalHiveButton
import com.eleazar.localhive.ui.components.LocalHiveTextField
import com.eleazar.localhive.ui.components.ScreenGutter
import com.eleazar.localhive.ui.components.readable
import com.eleazar.localhive.ui.theme.HiveGreen
import com.eleazar.localhive.ui.util.clockTime
import com.eleazar.localhive.ui.util.nowMillis
import com.eleazar.localhive.ui.visitors.passDay

/**
 * The only screen a security guard has: check a visitor's code, let them in, and check them out.
 * No feed, chats, directory or money; guards aren't residents.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GateScreen(
    uiState: GateUiState,
    onLookUp: (code: String) -> Unit,
    onClearLookup: () -> Unit,
    onCheckIn: (VisitorPass) -> Unit,
    onCheckOut: (VisitorPass) -> Unit,
    onMessageShown: () -> Unit,
    onRetry: () -> Unit,
    onLeavePost: () -> Unit,
    onSignOut: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    val post = (uiState.phase as? GatePhase.OnDuty)?.post

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbar.showSnackbar(it)
            onMessageShown()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gate", fontWeight = FontWeight.SemiBold)
                        post?.let {
                            Text(it.estateName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (post != null) {
                                DropdownMenuItem(
                                    text = { Text("Stop guarding ${post.estateName}") },
                                    onClick = { menuOpen = false; confirmLeave = true }
                                )
                            }
                            DropdownMenuItem(text = { Text("Sign out") }, onClick = { menuOpen = false; onSignOut() })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val phase = uiState.phase) {
                GatePhase.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is GatePhase.Failed -> EmptyState(
                    icon = Icons.Outlined.Shield,
                    title = "Couldn't open the gate",
                    message = phase.message,
                    actionLabel = "Try again",
                    onAction = onRetry
                )
                GatePhase.NotGuard -> EmptyState(
                    icon = Icons.Outlined.Shield,
                    title = "You're not on duty at an estate",
                    message = "An admin may have removed you from their gate. Ask them for a new guard code to sign on again.",
                    actionLabel = "Enter a guard code",
                    onAction = onLeavePost
                )
                is GatePhase.OnDuty -> OnDutyContent(
                    uiState = uiState,
                    onLookUp = onLookUp,
                    onClearLookup = onClearLookup,
                    onCheckIn = onCheckIn,
                    onCheckOut = onCheckOut
                )
            }
        }
    }

    if (confirmLeave && post != null) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Stop guarding ${post.estateName}?") },
            text = { Text("You won't be able to check visitors in there until an admin gives you a new guard code.") },
            confirmButton = { TextButton(onClick = { confirmLeave = false; onLeavePost() }) { Text("Stop") } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun OnDutyContent(
    uiState: GateUiState,
    onLookUp: (code: String) -> Unit,
    onClearLookup: () -> Unit,
    onCheckIn: (VisitorPass) -> Unit,
    onCheckOut: (VisitorPass) -> Unit
) {
    var code by remember { mutableStateOf("") }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val now = nowMillis()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "check") {
            Card {
                Text("Check a visitor's code", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(10.dp))
                LocalHiveTextField(
                    value = code,
                    onValueChange = {
                        code = normalizeCode(it).take(PASS_CODE_LENGTH)
                        if (uiState.lookup !is PassLookup.Idle) onClearLookup()
                    },
                    label = "Pass code",
                    placeholder = "e.g. K7MQ4X",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(onSearch = { onLookUp(code) }),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                LocalHiveButton(
                    text = "Check code",
                    onClick = { onLookUp(code) },
                    enabled = code.length == PASS_CODE_LENGTH,
                    isLoading = uiState.lookup is PassLookup.Searching,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        uiState.error?.let { error ->
            item(key = "error") { Text(error, fontSize = 14.sp, color = MaterialTheme.colorScheme.error) }
        }
        when (val lookup = uiState.lookup) {
            is PassLookup.Found -> item(key = "found") {
                FoundPass(
                    pass = lookup.pass,
                    now = now,
                    busy = uiState.busyCode == lookup.pass.code,
                    onCheckIn = { onCheckIn(lookup.pass); code = "" },
                    onCheckOut = { onCheckOut(lookup.pass); code = "" }
                )
            }
            is PassLookup.NotFound -> item(key = "not-found") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "No valid pass for ${lookup.code.ifBlank { "that code" }}",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "The code may be wrong, expired, cancelled or already used. Don't let them in on this code; " +
                                "ask them to contact the resident.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            else -> {}
        }

        item(key = "inside-label") {
            Text(
                if (uiState.inside.isEmpty()) "Nobody checked in right now" else "Inside now (${uiState.inside.size})",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = if (uiState.inside.isEmpty()) muted else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        items(uiState.inside, key = { it.code }) { pass ->
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pass.visitorName, fontWeight = FontWeight.SemiBold)
                        Text(visiting(pass), fontSize = 13.sp, color = muted)
                        Text("In since ${clockTime(pass.checkedInAt)}", fontSize = 13.sp, color = muted)
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { onCheckOut(pass) }, enabled = uiState.busyCode == null) {
                        Text("Check out")
                    }
                }
            }
        }
    }
}

@Composable
private fun FoundPass(
    pass: VisitorPass,
    now: Long,
    busy: Boolean,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit
) {
    val green = readable(HiveGreen)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = green.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, green.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(pass.code, fontFamily = FontFamily.Monospace, fontSize = 13.sp, letterSpacing = 2.sp, color = muted)
            Text(pass.visitorName, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(visiting(pass), fontSize = 15.sp)
            Text("For ${passDay(pass.validFrom, now).lowercase()}", fontSize = 13.sp, color = muted)
            if (pass.note.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("Note: ${pass.note}", fontSize = 14.sp)
            }
            Spacer(Modifier.height(12.dp))
            when {
                pass.stage == PassStage.INSIDE -> {
                    Text("Checked in at ${clockTime(pass.checkedInAt)}", fontSize = 14.sp, color = green)
                    Spacer(Modifier.height(8.dp))
                    LocalHiveButton("Check out", onClick = onCheckOut, isLoading = busy, modifier = Modifier.fillMaxWidth())
                }
                pass.canCheckIn(now) ->
                    LocalHiveButton("Check in", onClick = onCheckIn, isLoading = busy, modifier = Modifier.fillMaxWidth())
                else -> Text(
                    "This pass is for ${passDay(pass.validFrom, now)}, not today. Don't check them in on it.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

private fun visiting(pass: VisitorPass): String =
    "Visiting ${pass.residentName.ifBlank { "a resident" }}" +
        if (pass.residentAddress.isNotBlank()) ", ${pass.residentAddress}" else ""

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

/** A guard signing on to an estate with the one-time code its admin gave them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardSignOnScreen(
    isRedeeming: Boolean,
    error: String?,
    onRedeem: (code: String) -> Unit,
    onCodeChanged: () -> Unit,
    onBack: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Sign on as a guard", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Security guards don't join the estate. You'll only see the gate: checking visitors' codes, " +
                    "and who is inside. The estate's admin gives you a one-time guard code.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LocalHiveTextField(
                value = code,
                onValueChange = {
                    code = normalizeCode(it).take(GUARD_CODE_LENGTH)
                    if (error != null) onCodeChanged()
                },
                label = "Guard code",
                isError = error != null,
                supportingText = error,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onRedeem(code) }),
                modifier = Modifier.fillMaxWidth()
            )
            LocalHiveButton(
                text = "Sign on",
                onClick = { onRedeem(code) },
                enabled = code.length == GUARD_CODE_LENGTH,
                isLoading = isRedeeming,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
