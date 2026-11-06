package com.eleazar.localhive.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.Message
import kotlinx.coroutines.launch
import com.eleazar.localhive.ui.blocking.BlockConfirmDialog
import androidx.compose.material.icons.outlined.Flag
import com.eleazar.localhive.domain.model.ReportReason
import com.eleazar.localhive.ui.report.ReportDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.outlined.ContentCopy
import com.eleazar.localhive.ui.components.Avatar
import com.eleazar.localhive.ui.components.CharactersLeft
import com.eleazar.localhive.ui.platform.rememberPlatformActions
import com.eleazar.localhive.ui.components.LinkifiedText
import com.eleazar.localhive.ui.theme.HiveGreen
import com.eleazar.localhive.ui.util.clockTime
import com.eleazar.localhive.ui.util.dayLabel
import com.eleazar.localhive.ui.util.startOfDay

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
private const val GROUP_WINDOW_MILLIS = 5 * 60_000L

/** Matches the message rule in firestore.rules. */
private const val MAX_MESSAGE_LENGTH = 5000

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
    onSendMessage: (String) -> Unit,
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
                        onSendMessage(messageText)
                        messageText = ""
                    }
                },
                colors = colors
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
                    seenUpTo = seenUpTo
                )
            }
        }
    }
}

@Composable
private fun MessageList(
    messages: List<Message>,
    currentUserId: String?,
    isLoadingMore: Boolean,
    listState: LazyListState,
    colors: ChatDetailColors,
    seenUpTo: Long
) {
    // "Sent" or "Seen" goes under the user's own message when it's the last thing said.
    val newest = messages.firstOrNull()
    val status = when {
        newest == null || newest.senderId != currentUserId -> null
        seenUpTo >= newest.timestamp -> "Seen"
        else -> "Sent"
    }
    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
    ) {
        if (status != null) {
            item(key = "status") {
                Text(
                    status,
                    fontSize = 12.sp,
                    color = if (status == "Seen") colors.hiveGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp, end = 4.dp)
                )
            }
        }
        messages.forEachIndexed { index, message ->
            // Reversed layout: index - 1 sits visually below this message, index + 1 above.
            val below = messages.getOrNull(index - 1)
            val above = messages.getOrNull(index + 1)
            val startsDay = above == null || startOfDay(above.timestamp) != startOfDay(message.timestamp)
            val joinsAbove = !startsDay && above.continuesInto(message)
            val joinsBelow = below != null &&
                startOfDay(below.timestamp) == startOfDay(message.timestamp) &&
                message.continuesInto(below)

            item(key = message.id) {
                MessageBubble(
                    message = message,
                    isOwn = message.senderId == currentUserId,
                    joinsAbove = joinsAbove,
                    joinsBelow = joinsBelow,
                    colors = colors,
                    modifier = Modifier.padding(top = if (joinsAbove) 2.dp else 8.dp)
                )
            }
            if (startsDay) {
                item(key = "day-${message.id}") { DaySeparator(dayLabel(message.timestamp)) }
            }
        }
        if (isLoadingMore) {
            item(key = "loading-older") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = colors.hiveGreen, strokeWidth = 2.dp)
                }
            }
        }
    }
}

private fun Message?.continuesInto(next: Message): Boolean =
    this != null && senderId == next.senderId && kotlin.math.abs(next.timestamp - timestamp) < GROUP_WINDOW_MILLIS

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isOwn: Boolean,
    joinsAbove: Boolean,
    joinsBelow: Boolean,
    colors: ChatDetailColors,
    modifier: Modifier = Modifier
) {
    val large = 18.dp
    val small = 6.dp
    // The sender-side corners tighten where a bubble joins its neighbours, so a run reads as one block.
    val shape = if (isOwn) {
        RoundedCornerShape(
            topStart = large, bottomStart = large,
            topEnd = if (joinsAbove) small else large,
            bottomEnd = if (joinsBelow) small else large
        )
    } else {
        RoundedCornerShape(
            topEnd = large, bottomEnd = large,
            topStart = if (joinsAbove) small else large,
            bottomStart = if (joinsBelow) small else large
        )
    }
    // Grey for theirs, as most messengers do; white wouldn't show on the white page.
    val container = if (isOwn) colors.hiveGreen else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (isOwn) Color.White else MaterialTheme.colorScheme.onSurface

    val platform = rememberPlatformActions()
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        Box {
            Surface(
                shape = shape,
                color = container,
                contentColor = content,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .widthIn(min = 64.dp, max = 296.dp)
                    .clip(shape)
                    // A long press offers to copy, as in most messengers.
                    .combinedClickable(
                        onClickLabel = null,
                        onLongClickLabel = "Message options",
                        onClick = {},
                        onLongClick = { menuOpen = true }
                    )
            ) {
                Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp)) {
                    LinkifiedText(text = message.content, fontSize = 15.sp, lineHeight = 21.sp)
                    Text(
                        text = clockTime(message.timestamp),
                        fontSize = 11.sp,
                        color = content.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                    )
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Copy") },
                    leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        platform.copyToClipboard("Message", message.content, "Copied")
                    }
                )
            }
        }
    }
}

@Composable
private fun DaySeparator(label: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ConversationStart(name: String, photoUrl: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Avatar(imageUrl = photoUrl, name = name, size = 80.dp)
        Spacer(Modifier.height(16.dp))
        Text(name, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(
            "This is the start of your conversation. Say hello!",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

/** Takes the composer's place when this conversation can't continue. */
@Composable
private fun ClosedComposer(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    message,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    modifier = Modifier.weight(1f).padding(vertical = 8.dp)
                )
                if (actionLabel != null) {
                    TextButton(onClick = onAction) { Text(actionLabel, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun MessageComposer(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    colors: ChatDetailColors
) {
    val canSend = text.isNotBlank()
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            CharactersLeft(
                text.length,
                MAX_MESSAGE_LENGTH,
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 76.dp, top = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text("Message") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 5,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = colors.hiveGreen
                    )
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = onSend,
                    enabled = canSend,
                    shape = CircleShape,
                    // A one-line field is 56dp tall; lifting by 2dp centres the button on it.
                    modifier = Modifier.padding(bottom = 2.dp).size(52.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colors.hiveGreen,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}
