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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
            .background(
                Brush.verticalGradient(listOf(gold, gold.copy(alpha = 0.85f), colors.surfaceGradientEnd))
            )
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
            text = "Find Your Estate",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = dark,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Join your neighbourhood community\nor start a new one",
            fontSize = 15.sp,
            color = dark.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(top = 10.dp, bottom = 44.dp)
        )

        EstateOptionCard(
            icon = Icons.Default.Search,
            title = "Search & Join Estate",
            subtitle = "Find your estate and send a join request",
            containerColor = green,
            contentColor = Color.White,
            onClick = onJoinClick
        )

        Spacer(Modifier.height(14.dp))

        EstateOptionCard(
            icon = Icons.Default.Add,
            title = "Create Estate",
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(contentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = contentColor, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = contentColor)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, fontSize = 13.sp, color = contentColor.copy(alpha = 0.7f), lineHeight = 18.sp)
            }
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
            "Request Sent!",
            fontSize = 28.sp,
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
            Text("Cancel Request", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}
