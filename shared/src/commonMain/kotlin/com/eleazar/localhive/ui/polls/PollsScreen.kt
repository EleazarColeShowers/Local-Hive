package com.eleazar.localhive.ui.polls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.Poll
import com.eleazar.localhive.ui.components.EmptyState
import com.eleazar.localhive.ui.components.ScreenGutter
import com.eleazar.localhive.ui.util.nowMillis
import com.eleazar.localhive.ui.util.relativeTime

/** "Closes in 2d", "Closed", or "Open" for a poll with no end date. */
fun Poll.timing(now: Long = nowMillis()): String {
    if (!isOpen(now)) return "Closed"
    if (closesAt == 0L) return "Open"
    val minutes = (closesAt - now) / 60_000L
    return when {
        minutes < 60 -> "Closes in ${minutes.coerceAtLeast(1)}m"
        minutes < 48 * 60 -> "Closes in ${minutes / 60}h"
        else -> "Closes in ${minutes / (24 * 60)}d"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollsScreen(
    uiState: PollsUiState,
    onVote: (pollId: String, option: Int) -> Unit,
    onClose: (pollId: String) -> Unit,
    onDelete: (pollId: String) -> Unit,
    onCreatePoll: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Polls", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            // Polls speak for the estate, so only admins start them; the rules refuse anyone else.
            if (uiState.isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = onCreatePoll,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New poll") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            uiState.polls.isEmpty() -> EmptyState(
                icon = Icons.Outlined.HowToVote,
                title = "No polls yet",
                message = if (uiState.isAdmin) "Ask the estate a question and see where everyone stands."
                else "When an admin asks the estate something, it'll show up here.",
                modifier = Modifier.padding(padding).padding(top = 48.dp)
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.error?.let { error ->
                    item(key = "error") { Text(error, fontSize = 14.sp, color = MaterialTheme.colorScheme.error) }
                }
                items(uiState.polls, key = { it.id }) { poll ->
                    PollCard(
                        poll = poll,
                        myVote = uiState.myVotes[poll.id],
                        isAdmin = uiState.isAdmin,
                        onVote = { option -> onVote(poll.id, option) },
                        onClose = { onClose(poll.id) },
                        onDelete = { onDelete(poll.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun PollCard(
    poll: Poll,
    myVote: Int?,
    isAdmin: Boolean,
    onVote: (option: Int) -> Unit,
    onClose: () -> Unit,
    onDelete: () -> Unit
) {
    val open = poll.isOpen(nowMillis())
    // Results only once you've voted, so the running tally doesn't sway anyone.
    val showResults = myVote != null || !open
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    poll.question,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (isAdmin) {
                    Box(modifier = Modifier.size(24.dp).wrapContentSize(unbounded = true)) {
                        IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Poll options", tint = muted, modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (open) {
                                DropdownMenuItem(text = { Text("Close voting") }, onClick = { menuOpen = false; onClose() })
                            }
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = { menuOpen = false; confirmDelete = true }
                            )
                        }
                    }
                }
            }
            Text(
                "${poll.createdByName.ifBlank { "An admin" }} · ${relativeTime(poll.createdAt)} · ${poll.timing()}",
                fontSize = 12.sp,
                color = muted,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            poll.options.forEachIndexed { index, option ->
                if (showResults) {
                    ResultRow(
                        label = option,
                        votes = poll.counts.getOrElse(index) { 0 },
                        total = poll.voteCount,
                        mine = myVote == index
                    )
                } else {
                    OutlinedButton(
                        onClick = { onVote(index) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text(option, color = accent, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            Text(
                buildString {
                    append(if (poll.voteCount == 1) "1 vote" else "${poll.voteCount} votes")
                    when {
                        myVote != null -> append(" · You voted")
                        open -> append(" · Votes are anonymous and final")
                    }
                },
                fontSize = 12.sp,
                color = muted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this poll?") },
            text = { Text("Its results will be gone for everyone. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ResultRow(label: String, votes: Int, total: Int, mine: Boolean) {
    val fraction = if (total > 0) votes.toFloat() / total else 0f
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .height(42.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(accent.copy(alpha = if (mine) 0.28f else 0.14f))
        )
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                fontWeight = if (mine) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (mine) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.CheckCircle, contentDescription = "Your vote", tint = accent, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                "${(fraction * 100).toInt()}%",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
