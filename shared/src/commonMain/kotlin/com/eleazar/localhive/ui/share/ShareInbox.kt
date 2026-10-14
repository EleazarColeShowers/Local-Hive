package com.eleazar.localhive.ui.share

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/**
 * Something another app shared into LocalHive, waiting to become a post.
 *
 * @param media the platform's URI string for a shared photo or video, if there is one
 */
data class IncomingShare(val text: String, val media: String?, val isVideo: Boolean)

/**
 * Holds a share for the activity that received it until the composer picks it up, which may be
 * after signing in. It belongs to that one activity, not the process: a share opens its own copy
 * of the app over the sharing app, and an already-open copy must not react to it.
 */
class ShareInbox : ViewModel() {
    private val _pending = MutableStateFlow<IncomingShare?>(null)
    val pending = _pending.asStateFlow()

    fun offer(share: IncomingShare) {
        _pending.value = share
    }

    /** Hands over the waiting share, at most once. */
    fun take(): IncomingShare? = _pending.getAndUpdate { null }
}

val LocalShareInbox = staticCompositionLocalOf<ShareInbox?> { null }
