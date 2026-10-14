package com.eleazar.localhive.domain.model

/** Where a security report or emergency stands. [key] is what's stored on the post. */
enum class IncidentStatus(val key: String, val label: String) {
    OPEN("open", "Open"),
    HANDLING("handling", "Being handled"),
    RESOLVED("resolved", "Resolved");

    companion object {
        fun fromKey(key: String?): IncidentStatus? = entries.firstOrNull { it.key == key }
    }
}

/** What an SOS is about. [key] is what's stored on the post. */
enum class EmergencyType(val key: String, val label: String) {
    BREAK_IN("break_in", "Break-in"),
    FIRE("fire", "Fire"),
    MEDICAL("medical", "Medical"),
    OTHER("other", "Other");

    companion object {
        fun fromKey(key: String?): EmergencyType? = entries.firstOrNull { it.key == key }
    }
}
