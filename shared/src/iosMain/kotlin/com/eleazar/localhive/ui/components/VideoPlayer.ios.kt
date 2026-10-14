package com.eleazar.localhive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayer(url: String, modifier: Modifier, autoPlay: Boolean) {
    val player = remember(url) { NSURL.URLWithString(url)?.let { AVPlayer(uRL = it) } } ?: return

    DisposableEffect(player) {
        if (autoPlay) player.play()
        onDispose { player.pause() }
    }

    // AVPlayerViewController letterboxes inside this frame itself.
    UIKitViewController(
        factory = {
            AVPlayerViewController().apply {
                this.player = player
                showsPlaybackControls = true
            }
        },
        modifier = modifier.aspectRatio(16f / 9f).background(Color.Black)
    )
}
