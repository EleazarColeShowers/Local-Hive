package com.eleazar.localhive.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** "Admin" pill shown beside an admin's name; [owner] marks the estate's creator instead. */
@Composable
fun RoleBadge(
    modifier: Modifier = Modifier,
    owner: Boolean = false,
    /** Override where the brand green wouldn't read, e.g. on an announcement's honey fill. */
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Surface(shape = RoundedCornerShape(50), color = tint.copy(alpha = 0.13f), modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (owner) Icons.Filled.Star else Icons.Filled.Shield,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(11.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(if (owner) "Owner" else "Admin", color = tint, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
