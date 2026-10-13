package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.Chat
import com.eleazar.localhive.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    /** Chats [userId] is in, most recent first. Fails (rather than going quiet) if the query is rejected. */
    fun getChats(userId: String, limit: Long = 50): Flow<List<Chat>>
    fun getMessages(chatId: String, limit: Long = 30): Flow<List<Message>>
    suspend fun getOlderMessages(chatId: String, before: Long, limit: Long = 30): Result<List<Message>>
    /**
     * Sends [message], creating its chat on the first message. [participantNames] are the
     * display names known to the sender; they are merged into the chat so both sides
     * (and push notifications) can label the conversation.
     */
    suspend fun sendMessage(message: Message, participantNames: Map<String, String>): Result<Unit>
    /** Clears [userId]'s unread tally for a chat they have just opened. */
    suspend fun markChatRead(chatId: String, userId: String): Result<Unit>
}
