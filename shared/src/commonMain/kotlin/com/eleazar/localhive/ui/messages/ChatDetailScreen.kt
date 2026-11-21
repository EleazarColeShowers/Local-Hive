package com.eleazar.localhive.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.Message
import kotlinx.coroutines.launch
import com.eleazar.localhive.ui.blocking.BlockConfirmDialog
import androidx.compose.material.icons.outlined.Flag
import com.eleazar.localhive.domain.model.ReportReason
import com.eleazar.localhive.ui.report.ReportDialog
import com.eleazar.localhive.ui.components.Avatar
import com.eleazar.localhive.ui.theme.HiveGreen

data class ChatDetailColors(
    val hiveGreen: Color = HiveGreen,
)

@Composable
fun rememberChatDetailColors(): ChatDetailColors {
    val colorScheme = MaterialTheme.colorScheme
    return ChatDetailColors(
        hiveGreen = colorScheme.primary,
    )
}

/** Consecutive messages from one sender closer together than this read as one run. */
internal const val GROUP_WINDOW_MILLIS = 5 * 60_000L

/** Matches the message rule in firestore.rules. */
internal const val MAX_MESSAGE_LENGTH = 5000

/**
 * Bare UI for the chat detail screen.
 * Free of ViewModel, Android Context, or navigation side effects.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    otherName: String,
    otherPhotoUrl: String?,
    messages: List<Message>,
    currentUserId: String?,
    isLoading: Boolean,
    isLoadingMoreMessages: Boolean,
    unsentDraft: String?,
    onNavigateBack: () -> Unit,
    /** Sends the text, answering the given message when the user chose to reply to one. */
    onSendMessage: (String, replyTo: Message?) -> Unit,
    onLoadOlderMessages: () -> Unit,
    onUnsentDraftShown: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenProfile: (() -> Unit)? = null,
    colors: ChatDetailColors = rememberChatDetailColors(),
    /** Why the last message didn't send, when it isn't a connection problem. */
    unsentReason: String? = null,
    isBlocked: Boolean = false,
    /** The other person has deleted their account. */
    otherDeleted: Boolean = false,
    onBlock: (() -> Unit)? = null,
    onUnblock: () -> Unit = {},
    /** Reports the conversation and the other person. */
    onReport: ((ReportReason, details: String) -> Unit)? = null,
    /** The other person is typing right now. */
    otherTyping: Boolean = false,
    /** The newest message the other person has seen, by its timestamp, for "Seen". */
    seenUpTo: Long = 0L,
    /** The composer's text changed, so the other person can be told the user is typing. */
    onDraftChanged: (String) -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmBlock by remember { mutableStateOf<Boolean?>(null) }
    var reporting by remember { mutableStateOf(false) }
    if (reporting && onReport != null) {
        ReportDialog(title = "Report $otherName", onSubmit = onReport, onDismiss = { reporting = false })
    }
    confirmBlock?.let { blocking ->
        BlockConfirmDialog(
            name = otherName,
            blocking = blocking,
            onConfirm = if (blocking) ({ onBlock?.invoke() ?: Unit }) else onUnblock,
            onDismiss = { confirmBlock = null }
        )
    }

    var messageText by rememberSaveable { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<Message?>(null) }
    // The message a tapped quote jumped to, lit up for a moment so the eye finds it.
    var highlightedId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(highlightedId) {
        if (highlightedId != null) {
            delay(1_500)
            highlightedId = null
        }
    }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Read inside long-lived effects, so they must see the latest values rather than
    // the ones captured on first composition.
    val currentMessages by rememberUpdatedState(messages)
    val loadOlder by rememberUpdatedState(onLoadOlderMessages)

    // Messages are newest-first with a reversed LazyColumn, so index 0 is the visual bottom.
    LaunchedEffect(messages.firstOrNull()?.id) {
        val newest = messages.firstOrNull() ?: return@LaunchedEffect
        if (newest.senderId == currentUserId || listState.firstVisibleItemIndex <= 2) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { lastVisible ->
                val total = listState.layoutInfo.totalItemsCount
                if (currentMessages.isNotEmpty() && lastVisible >= total - 3) loadOlder()
            }
    }

    val snackbarScope = rememberCoroutineScope()
    LaunchedEffect(unsentDraft) {
        val draft = unsentDraft ?: return@LaunchedEffect
        val reason = unsentReason
        if (messageText.isBlank()) messageText = draft
        // Clearing the draft restarts this effect, so the snackbar runs in a scope that outlives it.
        snackbarScope.launch {
            snackbarHostState.showSnackbar(reason ?: "Message not sent. Check your connection and try again.")
        }
        onUnsentDraftShown()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            modifier = if (onOpenProfile != null) {
                                Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpenProfile)
                            } else {
                                Modifier
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(imageUrl = otherPhotoUrl, name = otherName, size = 36.dp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    otherName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (otherTyping) {
                                    Text("typing…", fontSize = 13.sp, color = colors.hiveGreen, maxLines = 1)
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (onBlock != null && !otherDeleted) {
                            Box {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (isBlocked) "Unblock" else "Block",
                                                color = if (isBlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                            )
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null) },
                                        onClick = {
                                            menuOpen = false
                                            confirmBlock = !isBlocked
                                        }
                                    )
                                    if (onReport != null) {
                                        DropdownMenuItem(
                                            text = { Text("Report") },
                                            leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
                                            onClick = {
                                                menuOpen = false
                                                reporting = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            }
        },
        bottomBar = {
            when {
                otherDeleted -> ClosedComposer("This account has been deleted, so you can't reply.")
                isBlocked -> ClosedComposer("You've blocked $otherName.", actionLabel = "Unblock") { confirmBlock = false }
                else -> MessageComposer(
                text = messageText,
                onTextChange = {
                    messageText = it.take(MAX_MESSAGE_LENGTH)
                    onDraftChanged(messageText)
                },
                onSend = {
                    if (messageText.isNotBlank()) {
                        onSendMessage(messageText, replyingTo)
                        messageText = ""
                        replyingTo = null
                    }
                },
                colors = colors,
                replyingTo = replyingTo,
                replyingToName = replyingTo?.let { if (it.senderId == currentUserId) "yourself" else otherName },
                onCancelReply = { replyingTo = null }
            )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading && messages.isEmpty() -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.hiveGreen
                )
                messages.isEmpty() -> ConversationStart(
                    name = otherName,
                    photoUrl = otherPhotoUrl,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> MessageList(
                    messages = messages,
                    currentUserId = currentUserId,
                    isLoadingMore = isLoadingMoreMessages,
                    listState = listState,
                    colors = colors,
                    seenUpTo = seenUpTo,
                    otherName = otherName,
                    highlightedId = highlightedId,
                    // Replying needs an open composer.
                    onReply = if (otherDeleted || isBlocked) null else ({ replyingTo = it }),
                    onJumpTo = { highlightedId = it }
                )
            }
        }
    }
}
