package com.eleazar.localhive.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/** The counter only appears this close to the limit, so it doesn't nag while there's room. */
private fun showWithin(max: Int): Int = minOf(200, max / 5)

/** Whether [length] is close enough to [max] to show how many characters are left. */
fun nearLimit(length: Int, max: Int): Boolean = max - length <= showWithin(max)

/** Characters left before [max], turning red in the last 20. Shows nothing until [nearLimit]. */
@Composable
fun CharactersLeft(length: Int, max: Int, modifier: Modifier = Modifier) {
    if (!nearLimit(length, max)) return
    val remaining = (max - length).coerceAtLeast(0)
    Text(
        "$remaining",
        fontSize = 13.sp,
        textAlign = TextAlign.End,
        color = if (remaining <= 20) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/** For a text field's `supportingText`: the counter near the limit, otherwise nothing. */
fun charactersLeftSupport(length: Int, max: Int): (@Composable () -> Unit)? =
    if (nearLimit(length, max)) {
        { CharactersLeft(length, max, Modifier.fillMaxWidth()) }
    } else {
        null
    }
