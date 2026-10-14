package com.eleazar.localhive.ui.platform

import androidx.compose.runtime.Composable

enum class MediaKind { IMAGE, IMAGE_OR_VIDEO }

/**
 * Something the user picked. [uri] is the platform's URI string for it, which the repositories
 * read and Coil can draw: a content:// URI on Android, a file in the app's temporary folder on iOS.
 */
data class PickedMedia(val uri: String, val isVideo: Boolean)

/** Opens the system photo picker; [onPicked] gets the user's choice, and nothing if they cancel. */
@Composable
expect fun rememberMediaPicker(kind: MediaKind, onPicked: (PickedMedia) -> Unit): () -> Unit

/** A picked video's first frame, as something Coil can draw, since Coil only reads photos. */
@Composable
expect fun rememberVideoFrame(uri: String?): Any?

/** System features a screen hands work off to. */
interface PlatformActions {
    /** Copies [text] and briefly tells the user with [confirmation]. */
    fun copyToClipboard(label: String, text: String, confirmation: String)

    /** Opens the system share sheet for [text]. */
    fun shareText(text: String, title: String)

    /** The installed version, as shown to people, e.g. "1.0". */
    val appVersion: String
}

@Composable
expect fun rememberPlatformActions(): PlatformActions
