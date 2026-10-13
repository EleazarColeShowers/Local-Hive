package com.eleazar.localhive.domain.model

/** Optional tag a neighbour can put on a post. [key] is what's stored on the post. */
enum class PostCategory(val key: String, val label: String, val adminOnly: Boolean = false) {
    /** Estate-wide message from an admin, pinned and styled to stand out. */
    ANNOUNCEMENT("announcement", "Announcement", adminOnly = true),
    NOTICE("notice", "Notice"),
    QUESTION("question", "Question"),
    FOR_SALE("for_sale", "For sale"),
    EVENT("event", "Event");

    companion object {
        fun fromKey(key: String?): PostCategory? = entries.firstOrNull { it.key == key }
    }
}
