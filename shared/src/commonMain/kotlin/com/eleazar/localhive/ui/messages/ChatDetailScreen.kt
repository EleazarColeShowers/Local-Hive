package com.eleazar.localhive.ui.messages

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
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
import androidx.compose.material3.LocalContentColor
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

@Composable
private fun MessageList(
    messages: List<Message>,
    currentUserId: String?,
    isLoadingMore: Boolean,
    listState: LazyListState,
    colors: ChatDetailColors,
    seenUpTo: Long,
    otherName: String,
    highlightedId: String?,
    onReply: ((Message) -> Unit)?,
    onJumpTo: (messageId: String) -> Unit
) {
    // "Sent" or "Seen" goes under the user's own message when it's the last thing said.
    val newest = messages.firstOrNull()
    val status = when {
        newest == null || newest.senderId != currentUserId -> null
        seenUpTo >= newest.timestamp -> "Seen"
        else -> "Sent"
    }
    // Where each message sits in the list below (status line and day labels take slots too),
    // so tapping a quote can scroll to the message it quotes.
    val positions = remember(messages, status != null) {
        val map = HashMap<String, Int>()
        var index = if (status != null) 1 else 0
        messages.forEachIndexed { i, message ->
            map[message.id] = index++
            val above = messages.getOrNull(i + 1)
            if (above == null || startOfDay(above.timestamp) != startOfDay(message.timestamp)) index++
        }
        map
    }
    val scope = rememberCoroutineScope()
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
                    otherName = otherName,
                    isHighlighted = message.id == highlightedId,
                    onReply = onReply?.let { reply -> { reply(message) } },
                    // Only messages already loaded can be scrolled to.
                    onQuoteClick = message.replyTo?.messageId?.let { id ->
                        positions[id]?.let { position ->
                            {
                                scope.launch { listState.animateScrollToItem(position) }
                                onJumpTo(id)
                            }
                        }
                    },
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
    otherName: String,
    isHighlighted: Boolean,
    onReply: (() -> Unit)?,
    onQuoteClick: (() -> Unit)?,
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

    // Swiping a message to the right replies to it, as in WhatsApp; it springs back on release.
    val density = LocalDensity.current
    val swipeLimit = with(density) { 72.dp.toPx() }
    val swipeTrigger = with(density) { 56.dp.toPx() }
    val swipe = remember { Animatable(0f) }
    val swipeScope = rememberCoroutineScope()
    val swipeModifier = if (onReply == null) Modifier else Modifier.pointerInput(onReply) {
        detectHorizontalDragGestures(
            onDragEnd = {
                if (swipe.value >= swipeTrigger) onReply()
                swipeScope.launch { swipe.animateTo(0f) }
            },
            onDragCancel = { swipeScope.launch { swipe.animateTo(0f) } },
            onHorizontalDrag = { change, amount ->
                change.consume()
                swipeScope.launch { swipe.snapTo((swipe.value + amount).coerceIn(0f, swipeLimit)) }
            }
        )
    }
    val highlight = if (isHighlighted) colors.hiveGreen.copy(alpha = 0.12f) else Color.Transparent

    Box(modifier = modifier.fillMaxWidth().background(highlight, RoundedCornerShape(8.dp)).then(swipeModifier)) {
        if (swipe.value > 0f) {
            Icon(
                Icons.AutoMirrored.Filled.Reply,
                contentDescription = null,
                tint = colors.hiveGreen,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
                    .size(22.dp)
                    .graphicsLayer { alpha = (swipe.value / swipeTrigger).coerceAtMost(1f) }
            )
        }
    Row(
        modifier = Modifier.fillMaxWidth().offset { IntOffset(swipe.value.roundToInt(), 0) },
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
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp)
                ) {
                    message.replyTo?.let { reply ->
                        QuotedMessage(
                            // The quoted message is the user's own when it shares a sender with an own bubble, or not with theirs.
                            name = if ((reply.senderId == message.senderId) == isOwn) "You" else otherName,
                            content = reply.content,
                            barColor = if (isOwn) Color.White else colors.hiveGreen,
                            background = if (isOwn) Color.White.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                            onClick = onQuoteClick,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        )
                    }
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
                if (onReply != null) {
                    DropdownMenuItem(
                        text = { Text("Reply") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onReply()
                        }
                    )
                }
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
}

/** The message being answered, shown small at the top of a reply or above the composer. */
@Composable
private fun QuotedMessage(
    name: String,
    content: String,
    barColor: Color,
    background: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(barColor))
        Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
            Text(name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = barColor, maxLines = 1)
            Text(
                content,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = LocalContentColor.current.copy(alpha = 0.8f)
            )
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
    colors: ChatDetailColors,
    replyingTo: Message? = null,
    replyingToName: String? = null,
    onCancelReply: () -> Unit = {}
) {
    val canSend = text.isNotBlank()
    val focusRequester = remember { FocusRequester() }
    // Choosing Reply brings up the keyboard, ready to answer.
    LaunchedEffect(replyingTo?.id) {
        if (replyingTo != null) runCatching { focusRequester.requestFocus() }
    }
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            if (replyingTo != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuotedMessage(
                        name = "Replying to ${replyingToName.orEmpty()}",
                        content = replyingTo.content,
                        barColor = colors.hiveGreen,
                        background = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onCancelReply) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel reply")
                    }
                }
            }
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
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    onValueChange = onTextChange,
                    placeholder = { Text("Message") },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
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
