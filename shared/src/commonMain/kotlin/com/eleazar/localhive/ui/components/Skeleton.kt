package com.eleazar.localhive.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
 * Grey stand-ins shaped like what's loading, gently pulsing, shown in place of a spinner while a
 * list's first page arrives. The screen feels like it's already there, just filling in.
 */

/** The pulsing grey every placeholder shares. */
@Composable
private fun skeletonShade(): Color {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "skeleton-alpha"
    )
    return MaterialTheme.colorScheme.surfaceVariant.copy(alpha = pulse)
}

@Composable
private fun Bar(shade: Color, modifier: Modifier) {
    Box(modifier.height(12.dp).background(shade, RoundedCornerShape(6.dp)))
}

/** Feed posts: avatar, name, a couple of lines of text. */
@Composable
fun PostSkeletons(modifier: Modifier = Modifier) {
    val shade = skeletonShade()
    Column(modifier) {
        listOf(0.9f, 0.6f, 0.75f, 0.5f).forEach { lineWidth ->
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Box(Modifier.size(40.dp).background(shade, CircleShape))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Bar(shade, Modifier.width(120.dp))
                    Spacer(Modifier.height(10.dp))
                    Bar(shade, Modifier.fillMaxWidth(lineWidth))
                    Spacer(Modifier.height(6.dp))
                    Bar(shade, Modifier.fillMaxWidth(lineWidth * 0.7f))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        }
    }
}

/** People and conversations: avatar with a name and one line beside it. */
@Composable
fun RowSkeletons(modifier: Modifier = Modifier, count: Int = 6) {
    val shade = skeletonShade()
    val widths = listOf(0.55f, 0.4f, 0.65f, 0.45f, 0.6f, 0.35f)
    Column(modifier) {
        repeat(count) { i ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(44.dp).background(shade, CircleShape))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Bar(shade, Modifier.fillMaxWidth(widths[i % widths.size]))
                    Spacer(Modifier.height(8.dp))
                    Bar(shade, Modifier.fillMaxWidth(widths[(i + 2) % widths.size] * 0.8f))
                }
            }
        }
    }
}

/** Cards such as polls and dues: a rounded block with a title and a line inside. */
@Composable
fun CardSkeletons(modifier: Modifier = Modifier, count: Int = 3) {
    val shade = skeletonShade()
    Column(
        modifier.padding(horizontal = ScreenGutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(count) {
            Column(
                Modifier.fillMaxWidth()
                    .background(shade.copy(alpha = shade.alpha * 0.5f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Bar(shade, Modifier.fillMaxWidth(0.6f))
                Spacer(Modifier.height(10.dp))
                Bar(shade, Modifier.fillMaxWidth(0.35f))
                Spacer(Modifier.height(18.dp))
                Bar(shade, Modifier.fillMaxWidth())
            }
        }
    }
}
