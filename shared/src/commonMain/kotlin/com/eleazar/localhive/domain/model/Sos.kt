package com.eleazar.localhive.domain.model

/**
 * How a neighbour's SOS sounds on this person's phone. Kept on their profile rather than the
 * phone, because on iPhone the Worker has to pick the sound when it sends the push.
 */
enum class SosSound(val key: String, val label: String, val description: String) {
    SIREN("siren", "Siren", "A loud siren that's hard to miss"),
    SOUND("sound", "Notification sound", "Your usual notification sound"),
    VIBRATE("vibrate", "Vibrate only", "No sound. On iPhone the alert arrives silently");

    companion object {
        fun fromKey(key: String?): SosSound = entries.firstOrNull { it.key == key } ?: SIREN
    }
}

/** False alarms that count against someone: those in the last 30 days. Matches the Worker. */
const val FALSE_ALARM_WINDOW_MILLIS = 30L * 24 * 60 * 60 * 1000

/** This many recent false alarms and a member's alerts go out without the siren. */
const val FALSE_ALARM_LIMIT = 2

/** Where one member stands with SOS in their estate: what the admins have set, and their false alarms. */
data class SosStanding(
    /** An admin has stopped them sending SOS alerts at all. */
    val disabled: Boolean = false,
    /** When an admin last gave them the siren back; false alarms before it no longer count. */
    val restoredAt: Long = 0L,
    /** When each of their alerts was marked a false alarm. */
    val falseAlarmTimes: List<Long> = emptyList()
) {
    fun recentFalseAlarms(now: Long): Int {
        val since = maxOf(now - FALSE_ALARM_WINDOW_MILLIS, restoredAt)
        return falseAlarmTimes.count { it > since }
    }

    /** Their alerts still go out, but as an ordinary notification with no siren. */
    fun sirenRemoved(now: Long): Boolean = recentFalseAlarms(now) >= FALSE_ALARM_LIMIT
}
