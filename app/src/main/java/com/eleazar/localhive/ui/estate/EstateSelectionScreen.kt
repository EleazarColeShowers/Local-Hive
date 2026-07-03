package com.eleazar.localhive.ui.estate

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.BorderStroke
import com.eleazar.localhive.R
import com.eleazar.localhive.domain.model.JoinRequest

@Composable
fun EstateSelectionScreen(
    onEstateJoined: () -> Unit,
    viewModel: EstateViewModel = hiltViewModel()
) {
    var showCreate by remember { mutableStateOf(false) }
    var showJoin by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.success) {
        if (uiState.success) onEstateJoined()
    }

    if (showCreate) {
        CreateEstateScreen(onSuccess = onEstateJoined, onBack = { showCreate = false })
        return
    }
    if (showJoin) {
        JoinEstateScreen(onSuccess = onEstateJoined, onBack = { showJoin = false })
        return
    }

    val green = colorResource(id = R.color.hivegreen)
    val gold = colorResource(id = R.color.backgroundgold)
    val dark = colorResource(id = R.color.darkgray)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(gold, gold.copy(alpha = 0.85f), Color(0xFFFFF8E7)))
            )
    ) {
        AnimatedContent(
            targetState = uiState.pendingRequest,
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { pendingRequest ->
            if (pendingRequest != null) {
                PendingRequestContent(
                    request = pendingRequest,
                    dark = dark,
                    green = green,
                    onCancel = { viewModel.cancelRequest() }
                )
            } else {
                SelectionContent(
                    dark = dark,
                    green = green,
                    onCreateClick = { showCreate = true },
                    onJoinClick = { showJoin = true }
                )
            }
        }
    }
}

@Composable
private fun SelectionContent(
    dark: Color,
    green: Color,
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
            containerColor = Color.White,
            contentColor = dark,
            borderColor = green.copy(alpha = 0.3f),
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
    borderColor: Color = Color.Transparent,
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
