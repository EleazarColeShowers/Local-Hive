package com.eleazar.localhive.domain.model

data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastMessageSenderId: String = "",
    /** Messages each participant has not opened yet, keyed by user id. */
    val unreadCounts: Map<String, Int> = emptyMap()
) {
    fun unreadFor(userId: String?): Int = unreadCounts[userId] ?: 0
}
