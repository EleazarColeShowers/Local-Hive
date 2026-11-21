package com.eleazar.localhive.data.repository

import com.eleazar.localhive.data.platform.KeyValueStore
import com.eleazar.localhive.ui.util.nowMillis
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore

/** The rules allow this many devices in pushTokens/{uid}.tokens. */
private const val MAX_DEVICES = 10

/** This device's push token as last saved, so it can be taken off again later. */
private const val KEY_DEVICE_TOKEN = "push_device_token"

/**
 * Adds this device's [token] to the user's `tokens` map (token -> when last seen), so each of
 * their phones and tablets gets pushes. Drops the token this device had before, if FCM has since
 * replaced it, and the oldest devices past the limit.
 */
internal suspend fun FirebaseFirestore.saveDeviceToken(store: KeyValueStore, userId: String, token: String) {
    val ref = pushTokens().document(userId)
    val previous = store.getString(KEY_DEVICE_TOKEN)?.takeIf { it != token }
    val doc = runCatching { ref.get() }.getOrNull()?.takeIf { it.exists }
    val now = nowMillis()

    val tokens = (doc?.longMap("tokens").orEmpty() - setOfNotNull(previous) + (token to now))
        .entries.sortedByDescending { it.value }
        .take(MAX_DEVICES)
        .associate { it.key to it.value }

    if (doc == null) {
        ref.set(mapOf("tokens" to tokens, "updatedAt" to now))
    } else {
        // The single token test builds kept moves into the map when it's this device's.
        val legacy = doc.string("token")
        if (legacy != null && (legacy == token || legacy == previous)) {
            ref.update("tokens" to tokens, "updatedAt" to now, "token" to FieldValue.delete)
        } else {
            ref.update("tokens" to tokens, "updatedAt" to now)
        }
    }
    store.putString(KEY_DEVICE_TOKEN, token)
    removeProfileToken(userId)
}

/**
 * Stops pushes for [userId] reaching this device, leaving their other devices as they are.
 * Without a saved token (a test build from before the map) it clears the single token those kept.
 */
internal suspend fun FirebaseFirestore.removeDeviceToken(store: KeyValueStore, userId: String) {
    val ref = pushTokens().document(userId)
    val mine = store.getString(KEY_DEVICE_TOKEN)
    val doc = runCatching { ref.get() }.getOrNull()?.takeIf { it.exists }
    if (doc != null) {
        val tokens = doc.longMap("tokens").orEmpty()
        val legacy = doc.string("token")
        val keep = tokens - setOfNotNull(mine)
        when {
            keep.isEmpty() && (legacy == null || legacy == mine || mine == null) -> ref.delete()
            mine == null || legacy == mine -> ref.update("tokens" to keep, "token" to FieldValue.delete)
            keep.size != tokens.size -> ref.update("tokens" to keep)
        }
    }
    removeProfileToken(userId)
}
