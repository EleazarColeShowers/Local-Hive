package com.eleazar.localhive.data.repository

import com.eleazar.localhive.domain.MessageRepository
import com.eleazar.localhive.domain.model.Chat
import com.eleazar.localhive.domain.model.Message
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
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

    override fun getChats(userId: String, limit: Long): Flow<List<Chat>> = callbackFlow {
        val listener = firestore.collection("chats")
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val chats = snapshot?.documents?.mapNotNull { mapDocToChat(it) } ?: emptyList()
                trySend(chats)
            }
        awaitClose { listener.remove() }
    }

    // Newest-first: keeps the live window bounded to the latest page regardless of history size.
    override fun getMessages(chatId: String, limit: Long): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getOlderMessages(chatId: String, before: Long, limit: Long): Result<List<Message>> {
        return try {
            val snapshot = firestore.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(before)
                .limit(limit)
                .get().await()
            val messages = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Message::class.java)?.copy(id = doc.id)
            }
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(message: Message): Result<Unit> {
        return try {
            val chatRef = firestore.collection("chats").document(message.chatId)
            val chatDoc = chatRef.get().await()
            val participants = (chatDoc.get("participants") as? List<*>)?.filterIsInstance<String>()
                ?: throw Exception("Chat not found")

            val messageRef = chatRef.collection("messages").document()

            // One batch, so a message can never land without its chat summary following it.
            val batch = firestore.batch()
            batch.set(messageRef, message.copy(id = messageRef.id).toMap())

            val chatUpdates = mutableMapOf<String, Any>(
                "lastMessage" to message.content,
                "lastMessageTime" to message.timestamp,
                "lastMessageSenderId" to message.senderId
            )
            participants.filter { it != message.senderId }.forEach { recipientId ->
                chatUpdates["unreadCounts.$recipientId"] = FieldValue.increment(1)
            }
            batch.update(chatRef, chatUpdates)

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markChatRead(chatId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection("chats").document(chatId)
                .update("unreadCounts.$userId", 0).await()
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
                    lastMessageTime = System.currentTimeMillis(),
                    unreadCounts = mapOf(currentUserId to 0, otherUserId to 0)
                )
                firestore.collection("chats").document(chatId).set(chat.toMap()).await()
            }
            Result.success(chatId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mapped by hand rather than via toObject: Firestore hands back every number as a Long,
     * which a generic Map<String, Int> would happily accept and then blow up on at read.
     */
    private fun mapDocToChat(doc: DocumentSnapshot): Chat? {
        return try {
            val unread = (doc.get("unreadCounts") as? Map<*, *>)
                ?.mapNotNull { (key, value) ->
                    val userId = key as? String ?: return@mapNotNull null
                    val count = (value as? Number)?.toInt() ?: return@mapNotNull null
                    userId to count
                }
                ?.toMap()
                ?: emptyMap()

            Chat(
                id = doc.id,
                participants = (doc.get("participants") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                participantNames = (doc.get("participantNames") as? Map<*, *>)
                    ?.mapNotNull { (key, value) ->
                        val userId = key as? String ?: return@mapNotNull null
                        val name = value as? String ?: return@mapNotNull null
                        userId to name
                    }
                    ?.toMap()
                    ?: emptyMap(),
                lastMessage = doc.getString("lastMessage") ?: "",
                lastMessageTime = doc.getLong("lastMessageTime") ?: 0L,
                lastMessageSenderId = doc.getString("lastMessageSenderId") ?: "",
                unreadCounts = unread
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun Chat.toMap() = mapOf(
        "participants" to participants,
        "participantNames" to participantNames,
        "lastMessage" to lastMessage,
        "lastMessageTime" to lastMessageTime,
        "lastMessageSenderId" to lastMessageSenderId,
        "unreadCounts" to unreadCounts
    )

    private fun Message.toMap() = mapOf(
        "chatId" to chatId, "senderId" to senderId,
        "content" to content, "timestamp" to timestamp
    )
}
