package com.eleazar.localhive.domain.model

data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastMessageSenderId: String = "",
    /** Messages each participant has not opened yet, keyed by user id. */
    val unreadCounts: Map<String, Int> = emptyMap(),
    /** Participants who have since deleted their account. */
    val deletedParticipants: List<String> = emptyList()
) {
    fun unreadFor(userId: String?): Int = unreadCounts[userId] ?: 0

    fun otherParticipant(userId: String?): String =
        participants.firstOrNull { it != userId } ?: participantsOf(id).firstOrNull { it != userId } ?: ""

    companion object {
        /**
         * One conversation per pair of neighbours, whoever starts it. Firestore rules
         * rely on this shape to decide membership, so build ids only through here.
         */
        fun idFor(userA: String, userB: String): String = listOf(userA, userB).sorted().joinToString("_")

        fun participantsOf(chatId: String): List<String> = chatId.split("_").filter { it.isNotBlank() }
    }
}
