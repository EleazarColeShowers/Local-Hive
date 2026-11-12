package com.eleazar.localhive.ui.visitors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.PassStage
import com.eleazar.localhive.domain.model.VisitorPass
import com.eleazar.localhive.ui.components.EmptyState
import com.eleazar.localhive.ui.components.LocalHiveButton
import com.eleazar.localhive.ui.components.LocalHiveTextField
import com.eleazar.localhive.ui.components.ScreenGutter
import com.eleazar.localhive.ui.components.readable
import com.eleazar.localhive.ui.theme.Clay
import com.eleazar.localhive.ui.theme.HiveGreen
import com.eleazar.localhive.ui.util.clockTime
import com.eleazar.localhive.ui.util.formatDate
import com.eleazar.localhive.ui.util.nowMillis
import com.eleazar.localhive.ui.util.startOfDay

/** "Today", "Tomorrow", or "Sat, Nov 15" for the day a pass is for. */
fun passDay(validFrom: Long, now: Long = nowMillis()): String {
    val today = startOfDay(now)
    val day = startOfDay(validFrom)
    return when {
        day == today -> "Today"
        day == startOfDay(today + 36 * 60 * 60 * 1000L) -> "Tomorrow"
        day == startOfDay(today - 12 * 60 * 60 * 1000L) -> "Yesterday"
        else -> formatDate(validFrom, "EEE, MMM d")
    }
}

/** The short status a pass shows, and its colour. */
@Composable
fun passStatus(pass: VisitorPass, now: Long = nowMillis()): Pair<String, Color> = when {
    pass.stage == PassStage.INSIDE -> "Arrived ${clockTime(pass.checkedInAt)}" to readable(HiveGreen)
    pass.stage == PassStage.LEFT -> "Left ${clockTime(pass.checkedOutAt)}" to MaterialTheme.colorScheme.onSurfaceVariant
    pass.stage == PassStage.CANCELLED -> "Cancelled" to MaterialTheme.colorScheme.onSurfaceVariant
    pass.isExpired(now) -> "Expired" to MaterialTheme.colorScheme.onSurfaceVariant
    else -> "Expected" to MaterialTheme.colorScheme.primary
}

/** What a resident sends their visitor. */
fun passShareText(pass: VisitorPass): String =
    "Your gate pass: ${pass.code}\n" +
        "For ${passDay(pass.validFrom)}, visiting ${pass.residentName}" +
        (if (pass.residentAddress.isNotBlank()) ", ${pass.residentAddress}" else "") +
        ".\nGive this code to the security guard at the gate."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitorsScreen(
    uiState: VisitorsUiState,
    onBack: () -> Unit,
    onCreate: (visitorName: String, note: String, daysFromToday: Int) -> Unit,
    onDismissCreated: () -> Unit,
    onShare: (VisitorPass) -> Unit,
    onCopy: (VisitorPass) -> Unit,
    onCancel: (code: String) -> Unit,
    onDelete: (code: String) -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf<VisitorPass?>(null) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val now = nowMillis()
    val (current, past) = uiState.passes.partition { it.isCurrent(now) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Visitors", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Expect a visitor") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "intro") {
                Text(
                    "Make a gate pass for someone you're expecting and send them the code. The guard checks " +
                        "the code at the gate, and you get a notification when they arrive and when they leave.",
                    fontSize = 14.sp,
                    color = muted
                )
            }
            if (uiState.missingAddress) {
                item(key = "address") {
                    Text(
                        "Add your house address to your profile, so guards can tell visitors where to go.",
                        fontSize = 13.sp,
                        color = readable(Clay)
                    )
                }
            }
            uiState.error?.let { error ->
                item(key = "error") { Text(error, fontSize = 14.sp, color = MaterialTheme.colorScheme.error) }
            }
            if (!uiState.isLoading && uiState.passes.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Outlined.Badge,
                        title = "No visitors yet",
                        message = "Expecting someone? Make them a pass and they'll get through the gate with its code.",
                        modifier = Modifier.padding(top = 48.dp)
                    )
                }
            }
            if (current.isNotEmpty()) {
                item(key = "current-label") { SectionLabel("Expected and inside") }
                items(current, key = { "c-${it.code}" }) { pass ->
                    PassCard(
                        pass = pass,
                        now = now,
                        onShare = { onShare(pass) },
                        onCopy = { onCopy(pass) },
                        onCancel = { confirmCancel = pass },
                        onDelete = null
                    )
                }
            }
            if (past.isNotEmpty()) {
                item(key = "past-label") { SectionLabel("Past") }
                items(past.take(30), key = { "p-${it.code}" }) { pass ->
                    PassCard(
                        pass = pass,
                        now = now,
                        onShare = null,
                        onCopy = null,
                        onCancel = null,
                        onDelete = { onDelete(pass.code) }
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreatePassDialog(
            isCreating = uiState.isCreating,
            onCreate = { name, note, day -> onCreate(name, note, day) },
            onDismiss = { showCreate = false }
        )
    }
    // Once made, the new code replaces the form.
    LaunchedEffect(uiState.created) {
        if (uiState.created != null) showCreate = false
    }
    uiState.created?.let { pass ->
        PassCreatedDialog(
            pass = pass,
            onShare = { onShare(pass) },
            onCopy = { onCopy(pass) },
            onDismiss = onDismissCreated
        )
    }
    confirmCancel?.let { pass ->
        AlertDialog(
            onDismissRequest = { confirmCancel = null },
            title = { Text("Cancel ${pass.visitorName}'s pass?") },
            text = { Text("The code ${pass.code} will stop working at the gate.") },
            confirmButton = {
                TextButton(onClick = { onCancel(pass.code); confirmCancel = null }) { Text("Cancel pass") }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = null }) { Text("Keep it") } }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun PassCard(
    pass: VisitorPass,
    now: Long,
    onShare: (() -> Unit)?,
    onCopy: (() -> Unit)?,
    onCancel: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val (status, statusColor) = passStatus(pass, now)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(pass.visitorName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        passDay(pass.validFrom, now) + (if (pass.note.isNotBlank()) " · ${pass.note}" else ""),
                        fontSize = 13.sp,
                        color = muted,
                        maxLines = 2
                    )
                }
                Text(status, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Remove ${pass.visitorName}'s pass", tint = muted)
                    }
                } else {
                    Spacer(Modifier.width(8.dp))
                }
            }
            if (pass.stage == PassStage.EXPECTED && onShare != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        pass.code,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        letterSpacing = 3.sp,
                        modifier = Modifier.weight(1f)
                    )
                    onCopy?.let {
                        IconButton(onClick = it) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy code") }
                    }
                    IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, contentDescription = "Share pass") }
                    onCancel?.let { TextButton(onClick = it) { Text("Cancel") } }
                }
            } else if (pass.stage == PassStage.INSIDE && pass.checkedInByName.isNotBlank()) {
                Text("Checked in by ${pass.checkedInByName}", fontSize = 13.sp, color = muted)
                Spacer(Modifier.height(6.dp))
            } else {
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun CreatePassDialog(
    isCreating: Boolean,
    onCreate: (visitorName: String, note: String, daysFromToday: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var day by remember { mutableIntStateOf(0) }
    val now = remember { nowMillis() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Expect a visitor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LocalHiveTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = "Visitor's name",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
                LocalHiveTextField(
                    value = note,
                    onValueChange = { note = it.take(200) },
                    label = "Note for the guard (optional)",
                    placeholder = "e.g. Plumber, grey Corolla",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Day of the visit", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    repeat(PASS_DAYS_AHEAD) { offset ->
                        FilterChip(
                            selected = day == offset,
                            onClick = { day = offset },
                            label = { Text(passDay(dayWindow(offset, now).first, now)) }
                        )
                    }
                }
                Text(
                    "The code works once, on that day only.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, note, day) },
                enabled = name.isNotBlank() && !isCreating
            ) { Text(if (isCreating) "Making pass…" else "Make pass") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PassCreatedDialog(
    pass: VisitorPass,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pass for ${pass.visitorName}") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Send them this code. It works ${passDay(pass.validFrom).let { if (it == "Today" || it == "Tomorrow") it.lowercase() else "on $it" }}.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    pass.code,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    letterSpacing = 6.sp
                )
                Spacer(Modifier.height(16.dp))
                LocalHiveButton(text = "Share with ${pass.visitorName}", onClick = onShare, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = onCopy) { Text("Copy code") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}
