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
