package com.eleazar.localhive.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.PostCategory
import com.eleazar.localhive.ui.theme.Clay
import com.eleazar.localhive.ui.theme.HoneyInk
import com.eleazar.localhive.ui.theme.HiveGreen
import com.eleazar.localhive.ui.theme.Plum
import com.eleazar.localhive.ui.theme.Teal

val PostCategory.color: Color
    get() = when (this) {
        // Honey is kept for announcements so they're the one thing that reads as "official".
        PostCategory.ANNOUNCEMENT -> HoneyInk
        PostCategory.NOTICE -> Clay
        PostCategory.QUESTION -> Teal
        PostCategory.FOR_SALE -> HiveGreen
        PostCategory.EVENT -> Plum
    }

val PostCategory.icon: ImageVector
    get() = when (this) {
        PostCategory.ANNOUNCEMENT -> Icons.Outlined.Campaign
        PostCategory.NOTICE -> Icons.Outlined.Info
        PostCategory.QUESTION -> Icons.AutoMirrored.Outlined.HelpOutline
        PostCategory.FOR_SALE -> Icons.Outlined.Sell
        PostCategory.EVENT -> Icons.Outlined.Event
    }

/** Small tinted pill naming a post's category. With [onClick] it acts as a toggle chip. */
@Composable
fun CategoryTag(
    category: PostCategory,
    modifier: Modifier = Modifier,
    selected: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val tint = if (dark) Color(
        red = category.color.red + (1f - category.color.red) * 0.35f,
        green = category.color.green + (1f - category.color.green) * 0.35f,
        blue = category.color.blue + (1f - category.color.blue) * 0.35f
    ) else category.color
    val container = if (selected) tint.copy(alpha = if (dark) 0.22f else 0.13f) else Color.Transparent
    val content = if (selected) tint else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
    val border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f))
    val shape = RoundedCornerShape(50)
    val body: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(
                horizontal = if (onClick != null) 12.dp else 8.dp,
                vertical = if (onClick != null) 7.dp else 3.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(category.icon, contentDescription = null, tint = content, modifier = Modifier.size(if (onClick != null) 16.dp else 13.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                category.label,
                color = content,
                fontSize = if (onClick != null) 13.sp else 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = container, border = border, modifier = modifier, content = body)
    } else {
        Surface(shape = shape, color = container, border = border, modifier = modifier, content = body)
    }
}
