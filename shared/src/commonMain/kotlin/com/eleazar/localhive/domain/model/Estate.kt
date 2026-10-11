package com.eleazar.localhive.domain.model

data class Estate(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val adminIds: List<String> = emptyList(),
    val inviteCode: String = "",
    val memberCount: Int = 0,
    val createdAt: Long = 0L
)
