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
    val phone: String? = null
)
