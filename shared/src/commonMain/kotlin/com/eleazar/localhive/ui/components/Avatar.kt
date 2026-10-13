package com.eleazar.localhive.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.eleazar.localhive.ui.theme.Clay
import com.eleazar.localhive.ui.theme.HoneyInk
import com.eleazar.localhive.ui.theme.HiveGreen
import com.eleazar.localhive.ui.theme.Plum
import com.eleazar.localhive.ui.theme.Teal

/** Each person keeps one colour everywhere, so faces are easier to tell apart in lists. */
private val AvatarPalette = listOf(HiveGreen, HoneyInk, Clay, Teal, Plum)

fun avatarColorFor(name: String?): Color {
    val key = name?.trim()?.lowercase().orEmpty()
    if (key.isEmpty()) return HiveGreen
    // FNV-1a over the UTF-8 bytes: stable across launches and platforms, and it spreads
    // similar names better than a 31-multiplier hash, which clumped them into one colour.
    var hash = 0x811c9dc5u
    for (byte in key.encodeToByteArray()) {
        hash = (hash xor byte.toUByte().toUInt()) * 0x01000193u
    }
    return AvatarPalette[(hash % AvatarPalette.size.toUInt()).toInt()]
}

@Composable
fun Avatar(
    imageUrl: String?,
    name: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    initialColor: Color? = null
) {
    val initial = name?.trim()?.firstOrNull()?.uppercase() ?: "?"
    val base = avatarColorFor(name)
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Surface(
        modifier = modifier.size(size).clip(CircleShape),
        color = backgroundColor ?: base.copy(alpha = if (dark) 0.3f else 0.16f)
    ) {
        if (imageUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value / 2.4f).sp,
                    // Full-strength colour reads too dark on a dark background; lift it there.
                    color = initialColor ?: if (dark) base.copy(alpha = 1f).let { lerpToWhite(it) } else base
                )
            }
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Profile picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

private fun lerpToWhite(c: Color, t: Float = 0.35f) = Color(
    red = c.red + (1f - c.red) * t,
    green = c.green + (1f - c.green) * t,
    blue = c.blue + (1f - c.blue) * t,
    alpha = 1f
)
