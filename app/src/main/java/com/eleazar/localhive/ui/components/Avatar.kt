package com.eleazar.localhive.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.eleazar.localhive.R

/**
 * A profile picture, falling back to the initial of [name] while a neighbour has not
 * uploaded one — which is every account created before avatars existed.
 */
@Composable
fun Avatar(
    imageUrl: String?,
    name: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    backgroundColor: Color = colorResource(id = R.color.hivegreen).copy(alpha = 0.2f),
    initialColor: Color = colorResource(id = R.color.hivegreen)
) {
    val initial = name?.trim()?.firstOrNull()?.uppercase() ?: "?"

    Surface(
        modifier = modifier.size(size).clip(CircleShape),
        color = backgroundColor
    ) {
        if (imageUrl.isNullOrBlank()) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initial,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value / 2.5f).sp,
                    color = initialColor
                )
            }
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
