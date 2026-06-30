package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.Chat
import com.eleazar.localhive.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getChats(userId: String): Flow<List<Chat>>
    fun getMessages(chatId: String): Flow<List<Message>>
    suspend fun sendMessage(message: Message): Result<Unit>
    suspend fun createOrGetChat(currentUserId: String, currentUserName: String, otherUserId: String, otherUserName: String): Result<String>
}
