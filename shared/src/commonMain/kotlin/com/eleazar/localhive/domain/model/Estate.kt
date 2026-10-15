package com.eleazar.localhive.domain.model

data class Estate(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val adminIds: List<String> = emptyList(),
    /** Whoever created the estate. Always an admin; nobody can take that away from them. */
    val ownerId: String = "",
    /** The member who looks after the estate's money, or blank when nobody has been appointed. */
    val treasurerId: String = "",
    val inviteCode: String = "",
    val memberCount: Int = 0,
    val createdAt: Long = 0L,
    val joinMode: JoinMode = JoinMode.BOTH
) {
    fun isAdmin(userId: String?): Boolean = userId != null && userId in adminIds
    fun isOwner(userId: String?): Boolean = userId != null && userId == ownerId
    fun isTreasurer(userId: String?): Boolean = !userId.isNullOrBlank() && userId == treasurerId
}

/** How newcomers get into an estate, set by its admins. [key] is what's stored on the estate. */
enum class JoinMode(val key: String, val label: String, val description: String) {
    BOTH("both", "Invite code or request", "Anyone with the code joins straight away; others can ask and wait for an admin."),
    CODE_ONLY("code", "Invite code only", "Only people given the code can join. Nobody can send a request."),
    REQUEST_ONLY("request", "Requests only", "Everyone has to ask, and an admin approves each person. Invite codes stop working.");

    val acceptsCodes: Boolean get() = this != REQUEST_ONLY
    val acceptsRequests: Boolean get() = this != CODE_ONLY

    companion object {
        fun fromKey(key: String?): JoinMode = entries.firstOrNull { it.key == key } ?: BOTH
    }
}
