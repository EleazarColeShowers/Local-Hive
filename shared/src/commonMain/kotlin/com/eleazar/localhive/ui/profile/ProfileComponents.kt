package com.eleazar.localhive.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// ───────────────────────── Building blocks ─────────────────────────

@Composable
internal fun SectionLabel(text: String, colors: ProfileColors) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
    )
}

@Composable
internal fun SectionCard(
    colors: ProfileColors,
    content: @Composable ColumnScope.() -> Unit
) {
    // A plain group of rows, as in system settings, rather than a floating card.
    Column(modifier = Modifier.fillMaxWidth(), content = content)
}

@Composable
private fun IconBadge(icon: ImageVector, colors: ProfileColors, size: Dp = 40.dp) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
    }
}

@Composable
internal fun InfoRow(icon: ImageVector, label: String, value: String, colors: ProfileColors) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon, colors)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.darkGray
            )
        }
    }
}

@Composable
internal fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    colors: ProfileColors,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon, colors)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.darkGray
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.darkGray.copy(alpha = 0.55f)
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.darkGray.copy(alpha = 0.35f)
        )
    }
}

@Composable
internal fun InviteCodeDialog(
    inviteCode: String,
    estateName: String,
    colors: ProfileColors,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconBadge(Icons.Default.Share, colors, size = 52.dp)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Invite to $estateName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.darkGray,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Share this code with neighbours so they can join",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.darkGray.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.hiveGreen.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, colors.hiveGreen.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "INVITE CODE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.hiveGreen.copy(alpha = 0.75f),
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = inviteCode,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.hiveGreen,
                            letterSpacing = 6.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { onCopy(); onDismiss() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.hiveGreen)
                    ) {
                        Text("Copy", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onShare(); onDismiss() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.hiveGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun fieldColors(colors: ProfileColors) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = colors.hiveGreen,
    focusedLabelColor = colors.hiveGreen,
    unfocusedBorderColor = colors.darkGray.copy(alpha = 0.2f),
    cursorColor = colors.hiveGreen,
    focusedContainerColor = colors.cardBackground,
    unfocusedContainerColor = colors.cardBackground
)
