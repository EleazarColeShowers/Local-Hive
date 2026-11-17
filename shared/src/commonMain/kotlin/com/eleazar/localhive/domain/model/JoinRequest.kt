package com.eleazar.localhive.domain.model

data class JoinRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val estateId: String = "",
    val estateName: String = "",
    val requestedAt: Long = 0L,
    val status: String = "pending"
)

/** Someone an admin removed from the estate. Until an admin lets them back in, a code won't work for them. */
data class RemovedMember(
    val userId: String = "",
    val name: String = "",
    val removedAt: Long = 0L
)
