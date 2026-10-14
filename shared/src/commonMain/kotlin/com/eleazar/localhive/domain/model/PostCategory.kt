package com.eleazar.localhive.domain.model

/** Optional tag a neighbour can put on a post. [key] is what's stored on the post. */
enum class PostCategory(
    val key: String,
    val label: String,
    val adminOnly: Boolean = false,
    /** Offered as a chip in the composer; emergencies are raised from the SOS screen instead. */
    val inComposer: Boolean = true
) {
    /** Estate-wide message from an admin, pinned and styled to stand out. */
    ANNOUNCEMENT("announcement", "Announcement", adminOnly = true),
    NOTICE("notice", "Notice"),
    QUESTION("question", "Question"),
    FOR_SALE("for_sale", "For sale"),
    EVENT("event", "Event"),
    /** Something suspicious or unsafe for the admins to look into. Tracked with an [IncidentStatus]. */
    SECURITY("security", "Security report"),
    /** An SOS: alerts the whole estate at once. Tracked with an [IncidentStatus]. */
    EMERGENCY("emergency", "Emergency", inComposer = false);

    /** Has a status that the reporter and the estate's admins move from open to resolved. */
    val isIncident: Boolean get() = this == SECURITY || this == EMERGENCY

    companion object {
        fun fromKey(key: String?): PostCategory? = entries.firstOrNull { it.key == key }
    }
}
