package com.eleazar.localhive.ui.estate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.compose.foundation.BorderStroke
import com.eleazar.localhive.domain.model.JoinRequest
import com.eleazar.localhive.domain.model.RemovedMember
import com.eleazar.localhive.ui.util.formatDate

@Composable
internal fun RequestsTab(
    requests: List<JoinRequest>,
    removed: List<RemovedMember>,
    onApprove: (String) -> Unit,
    onDeny: (String) -> Unit,
    onLetBackIn: (RemovedMember) -> Unit,
    dark: Color,
    green: Color,
    cardBg: Color
) {
    if (requests.isEmpty() && removed.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = dark.copy(alpha = 0.2f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("No pending requests", color = dark.copy(alpha = 0.4f), fontSize = 15.sp)
                Text(
                    "Join requests will appear here",
                    color = dark.copy(alpha = 0.3f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val removedIds = removed.map { it.userId }.toSet()
        items(requests, key = { it.userId }) { request ->
            JoinRequestCard(
                request = request,
                onApprove = { onApprove(request.userId) },
                onDeny = { onDeny(request.userId) },
                dark = dark,
                green = green,
                cardBg = cardBg
            )
            if (request.userId in removedIds) {
                Text(
                    "An admin removed this person before. Approving lets them back in.",
                    color = dark.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
        }
        if (removed.isNotEmpty()) {
            item(key = "removed-header") {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text("Removed", color = dark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        "They can't rejoin with an invite code until you let them back in. They can still ask to join.",
                        color = dark.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            items(removed, key = { "removed-${it.userId}" }) { member ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(cardBg, RoundedCornerShape(12.dp)).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(member.name.ifBlank { "Former member" }, color = dark, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        if (member.removedAt > 0) {
                            Text("Removed ${formatDate(member.removedAt, "d MMM yyyy")}", color = dark.copy(alpha = 0.5f), fontSize = 13.sp)
                        }
                    }
                    TextButton(onClick = { onLetBackIn(member) }) { Text("Let back in", color = green) }
                }
            }
        }
    }
}

@Composable
private fun JoinRequestCard(
    request: JoinRequest,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    dark: Color,
    green: Color,
    cardBg: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (request.userPhotoUrl.isNotBlank()) {
                AsyncImage(
                    model = request.userPhotoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(green.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = green, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    request.userName.ifBlank { "Unknown user" },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = dark
                )
                Text("Wants to join", fontSize = 12.sp, color = dark.copy(alpha = 0.5f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onDeny,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935).copy(alpha = 0.15f))
                ) {
                    Icon(Icons.Default.Close, "Deny", tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onApprove,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(green.copy(alpha = 0.12f))
                ) {
                    Icon(Icons.Default.Check, "Approve", tint = green, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
