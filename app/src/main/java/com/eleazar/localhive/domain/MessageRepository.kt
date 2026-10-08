package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.Chat
import com.eleazar.localhive.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getChats(userId: String, limit: Long = 50): Flow<List<Chat>>
    fun getMessages(chatId: String, limit: Long = 30): Flow<List<Message>>
    suspend fun getOlderMessages(chatId: String, before: Long, limit: Long = 30): Result<List<Message>>
    suspend fun sendMessage(message: Message): Result<Unit>
    /** Clears [userId]'s unread tally for a chat they have just opened. */
    suspend fun markChatRead(chatId: String, userId: String): Result<Unit>
    suspend fun createOrGetChat(currentUserId: String, currentUserName: String, otherUserId: String, otherUserName: String): Result<String>
}
