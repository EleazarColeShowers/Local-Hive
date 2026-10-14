package com.eleazar.localhive.ui.share

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat

/** Reads a single text, photo or video share; anything else is not a share we take. */
fun Intent?.toIncomingShare(resolver: ContentResolver): IncomingShare? {
    if (this?.action != Intent.ACTION_SEND) return null
    val media = IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java)
    val type = media?.let { resolver.getType(it) } ?: type.orEmpty()
    val body = getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
    // Browsers send the page title as the subject and the address as the text.
    val subject = getStringExtra(Intent.EXTRA_SUBJECT)?.trim().orEmpty()
    val text = if (subject.isNotEmpty() && subject !in body) "$subject\n$body".trim() else body
    val usableMedia = media?.takeIf { type.startsWith("image/") || type.startsWith("video/") }
    if (text.isEmpty() && usableMedia == null) return null
    return IncomingShare(
        text = text,
        media = usableMedia?.toString(),
        isVideo = usableMedia != null && type.startsWith("video/")
    )
}
