package com.eleazar.localhive.ui.estate

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.JoinRequest

/**
 * Bare UI for estate selection. No ViewModel or Android Context side effects.
 */
@Composable
fun EstateSelectionScreen(
    pendingRequest: JoinRequest?,
    colors: EstateColors,
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit,
    onCancelRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val green = colors.hiveGreen
    val gold = colors.backgroundGold
    val dark = colors.darkGray
    val cardBg = colors.cardBackground

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gold)
    ) {
        AnimatedContent(
            targetState = pendingRequest,
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { request ->
            if (request != null) {
                PendingRequestContent(
                    request = request,
                    dark = dark,
                    green = green,
                    onCancel = onCancelRequest
                )
            } else {
                SelectionContent(
                    dark = dark,
                    green = green,
                    cardBg = cardBg,
                    onCreateClick = onCreateClick,
                    onJoinClick = onJoinClick
                )
            }
        }
    }
}

@Composable
private fun SelectionContent(
    dark: Color,
    green: Color,
    cardBg: Color,
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(green.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                tint = green,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Find your estate",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = dark,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Join your neighbourhood community\nor start a new one",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 36.dp)
        )

        EstateOptionCard(
            icon = Icons.Default.Search,
            title = "Join an estate",
            subtitle = "Find your estate and send a join request",
            containerColor = cardBg,
            contentColor = dark,
            onClick = onJoinClick
        )

        Spacer(Modifier.height(12.dp))

        EstateOptionCard(
            icon = Icons.Default.Add,
            title = "Create an estate",
            subtitle = "Start a new community for your neighbourhood",
            containerColor = cardBg,
            contentColor = dark,
            onClick = onCreateClick
        )
    }
}

@Composable
private fun EstateOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    // An outlined choice row: icon, what it does, and a chevron to say it leads somewhere.
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = contentColor)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 19.sp)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PendingRequestContent(
    request: JoinRequest,
    dark: Color,
    green: Color,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(green.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = green,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "Request sent",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = dark,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Your request to join",
            fontSize = 15.sp,
            color = dark.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Text(
            request.estateName,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = green,
            textAlign = TextAlign.Center
        )
        Text(
            "has been sent to the admin.\nYou'll be notified when it's approved.",
            fontSize = 15.sp,
            color = dark.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 40.dp)
        )

        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = dark.copy(alpha = 0.6f)),
            border = BorderStroke(1.dp, dark.copy(alpha = 0.25f))
        ) {
            Text("Cancel request", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}
