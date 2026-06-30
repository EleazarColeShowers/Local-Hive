package com.eleazar.localhive.data.repository

import com.eleazar.localhive.domain.MessageRepository
import com.eleazar.localhive.domain.model.Chat
import com.eleazar.localhive.domain.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : MessageRepository {

    override fun getChats(userId: String): Flow<List<Chat>> = callbackFlow {
        val listener = firestore.collection("chats")
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val chats = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Chat::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(chats)
            }
        awaitClose { listener.remove() }
    }

    override fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendMessage(message: Message): Result<Unit> {
        return try {
            val ref = firestore.collection("chats").document(message.chatId)
                .collection("messages").document()
            ref.set(message.copy(id = ref.id).toMap()).await()
            firestore.collection("chats").document(message.chatId).update(
                mapOf("lastMessage" to message.content, "lastMessageTime" to message.timestamp)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createOrGetChat(
        currentUserId: String,
        currentUserName: String,
        otherUserId: String,
        otherUserName: String
    ): Result<String> {
        return try {
            val chatId = listOf(currentUserId, otherUserId).sorted().joinToString("_")
            val doc = firestore.collection("chats").document(chatId).get().await()
            if (!doc.exists()) {
                val chat = Chat(
                    id = chatId,
                    participants = listOf(currentUserId, otherUserId),
                    participantNames = mapOf(currentUserId to currentUserName, otherUserId to otherUserName),
                    lastMessage = "",
                    lastMessageTime = System.currentTimeMillis()
                )
                firestore.collection("chats").document(chatId).set(chat.toMap()).await()
            }
            Result.success(chatId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun Chat.toMap() = mapOf(
        "participants" to participants,
        "participantNames" to participantNames,
        "lastMessage" to lastMessage,
        "lastMessageTime" to lastMessageTime
    )

    private fun Message.toMap() = mapOf(
        "chatId" to chatId, "senderId" to senderId,
        "content" to content, "timestamp" to timestamp
    )
}
