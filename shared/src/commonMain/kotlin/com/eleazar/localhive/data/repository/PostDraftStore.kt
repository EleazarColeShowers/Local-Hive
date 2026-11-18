package com.eleazar.localhive.data.repository

import com.eleazar.localhive.data.platform.KeyValueStore

/** What someone had written in the composer when they left it, minus any photo or video. */
data class PostDraft(val text: String, val categoryKey: String?, val location: String)

/**
 * Keeps one unfinished post per person on this phone, so a call or a slip of the thumb doesn't
 * lose what they'd written. Keyed by user, so it never shows up for someone else signing in.
 */
class PostDraftStore(private val store: KeyValueStore) {

    fun load(userId: String): PostDraft? {
        val text = store.getString(key(userId, "text")).orEmpty()
        if (text.isBlank()) return null
        return PostDraft(
            text = text,
            categoryKey = store.getString(key(userId, "category"))?.takeIf { it.isNotBlank() },
            location = store.getString(key(userId, "location")).orEmpty()
        )
    }

    fun save(userId: String, draft: PostDraft) {
        store.putString(key(userId, "text"), draft.text)
        store.putString(key(userId, "category"), draft.categoryKey.orEmpty())
        store.putString(key(userId, "location"), draft.location)
    }

    fun clear(userId: String) = save(userId, PostDraft("", null, ""))

    private fun key(userId: String, field: String) = "postDraft.$userId.$field"
}
