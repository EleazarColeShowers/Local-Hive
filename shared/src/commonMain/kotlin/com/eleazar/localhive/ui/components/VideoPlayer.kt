package com.eleazar.localhive.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Plays the video at [url] with the platform's own controls, sized to the video's shape.
 * Playback pauses when the app goes to the background and stops when this leaves the screen.
 */
@Composable
expect fun VideoPlayer(url: String, modifier: Modifier = Modifier, autoPlay: Boolean = false)
