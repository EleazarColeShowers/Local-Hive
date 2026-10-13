package com.eleazar.localhive.domain.model

/** Someone the user has blocked. Name and photo are kept from when they were blocked, for the list in Settings. */
data class BlockedUser(
    val id: String = "",
    val name: String = "",
    val photoUrl: String? = null,
    val blockedAt: Long = 0L
)
