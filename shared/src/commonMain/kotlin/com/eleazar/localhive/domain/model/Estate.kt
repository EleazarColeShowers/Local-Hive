package com.eleazar.localhive.domain.model

data class Estate(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val adminIds: List<String> = emptyList(),
    /** Whoever created the estate. Always an admin; nobody can take that away from them. */
    val ownerId: String = "",
    val inviteCode: String = "",
    val memberCount: Int = 0,
    val createdAt: Long = 0L
) {
    fun isAdmin(userId: String?): Boolean = userId != null && userId in adminIds
    fun isOwner(userId: String?): Boolean = userId != null && userId == ownerId
}
