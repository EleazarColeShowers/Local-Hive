package com.eleazar.localhive.ui.messages

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.Message
import com.eleazar.localhive.ui.components.CharactersLeft

/** Takes the composer's place when this conversation can't continue. */
@Composable
internal fun ClosedComposer(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
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
internal fun MessageComposer(
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
