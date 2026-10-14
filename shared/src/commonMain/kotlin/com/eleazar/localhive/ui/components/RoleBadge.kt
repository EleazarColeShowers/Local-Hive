package com.eleazar.localhive.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "Admin" label shown beside an admin's name; [owner] marks the estate's creator instead, and
 * [treasurer] the member who looks after the estate's money.
 */
@Composable
fun RoleBadge(
    modifier: Modifier = Modifier,
    owner: Boolean = false,
    treasurer: Boolean = false,
    /** Override where the brand green wouldn't read, e.g. on an announcement's honey fill. */
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            when {
                treasurer -> Icons.Filled.AccountBalanceWallet
                owner -> Icons.Filled.Star
                else -> Icons.Filled.Shield
            },
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(2.dp))
        Text(when { treasurer -> "Treasurer"; owner -> "Owner"; else -> "Admin" }, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
