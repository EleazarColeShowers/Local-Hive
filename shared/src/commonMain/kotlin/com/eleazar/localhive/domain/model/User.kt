package com.eleazar.localhive.domain.model

data class User(
    val id: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val username: String? = null,
    val profileImageUrl: String? = null,
    val bio: String? = null,
    val estateId: String? = null,
    val address: String? = null,
    val occupation: String? = null,
    val phone: String? = null,
    val fcmToken: String? = null,
    /** Wants a push when a neighbour reports the light is back. */
    val lightAlerts: Boolean = false,
    /** How a neighbour's SOS sounds on their phone. */
    val sosSound: SosSound = SosSound.SIREN,
    /** Until when SOS alerts only vibrate, say for a meeting; 0 when not paused. */
    val sosPausedUntil: Long = 0L
)
