package com.eleazar.localhive.ui.feed

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import com.eleazar.localhive.ui.components.color
import com.eleazar.localhive.ui.components.hint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.EmergencyType
import com.eleazar.localhive.ui.components.ScreenGutter
import com.eleazar.localhive.ui.components.icon
import com.eleazar.localhive.ui.components.readable
import com.eleazar.localhive.ui.theme.AlertRed
import kotlinx.coroutines.launch

/** Fills the button while it's held. */
private val HoldFill = Color(0xFF7F0000)

/** Long enough that a pocket or a stray tap can't send it, short enough not to slow anyone down. */
private const val HOLD_MILLIS = 1500

/**
 * The SOS screen: what's happening and where, then a press-and-hold button that alerts the whole
 * estate.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyAlertScreen(
    uiState: FeedUiState,
    onSend: (type: EmergencyType, location: String, details: String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var typeKey by rememberSaveable { mutableStateOf<String?>(null) }
    val type = EmergencyType.fromKey(typeKey)
    // Starts as the address on the user's profile, once that has loaded, unless they've typed.
    var location by rememberSaveable { mutableStateOf<String?>(null) }
    val shownLocation = location ?: uiState.currentUserAddress.orEmpty()
    var details by rememberSaveable { mutableStateOf("") }
    val muted = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Emergency alert", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !uiState.isPosting) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = ScreenGutter, vertical = 12.dp)
                ) {
                    HoldToSendButton(
                        label = type?.takeIf { it != EmergencyType.OTHER }?.let { "Hold to send ${it.label.lowercase()} alert" }
                            ?: "Hold to send alert",
                        sending = uiState.isPosting,
                        enabled = uiState.estateId != null && !uiState.isPosting,
                        onSend = { onSend(type ?: EmergencyType.OTHER, shownLocation.trim(), details.trim()) }
                    )
                    Text(
                        "If someone's life is in danger, call 112 as well.",
                        fontSize = 13.sp,
                        color = muted,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenGutter, vertical = 8.dp)
        ) {
            Text(
                "Everyone in ${uiState.estateName ?: "your estate"} gets this straight away, with a notification " +
                    "and at the top of the feed.",
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(20.dp))
            Text("What's happening?", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(10.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.selectableGroup()
            ) {
                EmergencyType.entries.chunked(2).forEach { row ->
                    // Both tiles in a row match the taller one, however large the system font is.
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                        row.forEach { option ->
                            EmergencyTypeTile(
                                type = option,
                                selected = option == type,
                                enabled = !uiState.isPosting,
                                onClick = { typeKey = if (option == type) null else option.key },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                }
            }
            Text(
                if (type == null) "Not sure? Skip it. The alert still goes out."
                else "Tap ${type.label} again to clear it.",
                fontSize = 12.sp,
                color = muted,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = shownLocation,
                onValueChange = { location = it.take(120) },
                label = { Text("Where?") },
                placeholder = { Text("e.g. Block C, flat 4") },
                singleLine = true,
                enabled = !uiState.isPosting,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = details,
                onValueChange = { details = it.take(500) },
                label = { Text("Anything else? (optional)") },
                placeholder = { Text("e.g. Two men climbing the back fence") },
                minLines = 2,
                maxLines = 4,
                enabled = !uiState.isPosting,
                modifier = Modifier.fillMaxWidth()
            )

            uiState.error?.let { error ->
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(error, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/** Fills up while held and sends once full; letting go early cancels. */
@Composable
private fun HoldToSendButton(label: String, sending: Boolean, enabled: Boolean, onSend: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val currentOnSend by rememberUpdatedState(onSend)
    val currentEnabled by rememberUpdatedState(enabled)
    val shape = RoundedCornerShape(50)

    LaunchedEffect(sending) {
        // A failed send leaves the button ready to hold again.
        if (!sending) progress.snapTo(0f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(shape)
            .background(if (enabled || sending) AlertRed else AlertRed.copy(alpha = 0.35f))
            // Screen readers can't press and hold, so a double tap sends it for them.
            .semantics { onClick(label = "Send emergency alert") { if (currentEnabled) currentOnSend(); currentEnabled } }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    if (!currentEnabled) return@detectTapGestures
                    val fill = scope.launch {
                        progress.animateTo(1f, tween(HOLD_MILLIS, easing = LinearEasing))
                        currentOnSend()
                    }
                    tryAwaitRelease()
                    if (progress.value < 1f) {
                        fill.cancel()
                        scope.launch { progress.animateTo(0f, tween(200)) }
                    }
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(if (sending) 1f else progress.value)
                .background(HoldFill)
        )
        if (sending) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Alerting your estate…", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else {
            Text(
                if (progress.value > 0f) "Keep holding…" else label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        }
    }
}

/**
 * One kind of emergency: its icon, name and what it covers. Picked, it fills with the kind's own
 * colour and shows a tick, so the choice is obvious at a glance.
 */
@Composable
private fun EmergencyTypeTile(
    type: EmergencyType,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = readable(type.color)
    val container by animateColorAsState(
        if (selected) type.color else MaterialTheme.colorScheme.surface,
        label = "tileContainer"
    )
    val content by animateColorAsState(
        if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        label = "tileContent"
    )
    val scale by animateFloatAsState(if (selected) 1f else 0.97f, label = "tileScale")

    Surface(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = container,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = if (selected) 6.dp else 0.dp,
        modifier = modifier
            .heightIn(min = 116.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics { role = Role.RadioButton }
    ) {
        Box(modifier = Modifier.padding(14.dp)) {
            Column {
                Surface(
                    shape = CircleShape,
                    color = if (selected) Color.White.copy(alpha = 0.2f) else accent.copy(alpha = 0.12f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            type.icon,
                            contentDescription = null,
                            tint = if (selected) Color.White else accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(type.label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = content)
                Text(
                    type.hint,
                    fontSize = 12.sp,
                    color = content.copy(alpha = if (selected) 0.85f else 0.6f),
                    maxLines = 1
                )
            }
            if (selected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).size(22.dp)
                )
            }
        }
    }
}
