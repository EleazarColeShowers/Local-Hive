package com.eleazar.localhive.data.repository

import android.util.Log
import com.eleazar.localhive.data.remote.api.LocalHiveApi
import com.eleazar.localhive.domain.MessageRepository
import com.eleazar.localhive.domain.model.Chat
import com.eleazar.localhive.domain.model.Message
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "MessageRepository"

class MessageRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val api: LocalHiveApi
) : MessageRepository {

    // Sorted on the device rather than with orderBy, so the list doesn't hinge on a
    // composite index being deployed. A user's chat count is small enough for that.
    override fun getChats(userId: String, limit: Long): Flow<List<Chat>> = callbackFlow {
        val listener = firestore.collection("chats")
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Chat list listener failed", error)
                    close(error)
                    return@addSnapshotListener
                }
                val chats = snapshot?.documents
                    ?.mapNotNull { mapDocToChat(it) }
                    ?.sortedByDescending { it.lastMessageTime }
                    ?.take(limit.toInt())
                    ?: emptyList()
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
                if (error != null) {
                    Log.w(TAG, "Message listener failed for $chatId", error)
                    close(error)
                    return@addSnapshotListener
                }
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

    /**
     * A merge write rather than read-then-write: it creates the chat on the first message
     * and updates it after, without a round trip first — so the message shows up locally
     * straight away and the send still queues while offline.
     */
    override suspend fun sendMessage(message: Message, participantNames: Map<String, String>): Result<Unit> {
        return try {
            val participants = Chat.participantsOf(message.chatId)
            require(participants.size == 2 && message.senderId in participants) {
                "Not a participant of chat ${message.chatId}"
            }

            val chatRef = firestore.collection("chats").document(message.chatId)
            val messageRef = chatRef.collection("messages").document()

            val unreadCounts = participants.associateWith<String, Any> { userId ->
                // Sending implies the sender has read everything up to now.
                if (userId == message.senderId) 0 else FieldValue.increment(1)
            }
            val chatFields = mapOf(
                "participants" to participants,
                "participantNames" to participantNames.filter { (id, name) -> id in participants && name.isNotBlank() },
                "lastMessage" to message.content,
                "lastMessageTime" to message.timestamp,
                "lastMessageSenderId" to message.senderId,
                "unreadCounts" to unreadCounts
            )

            firestore.batch()
                .set(chatRef, chatFields, SetOptions.merge())
                .set(messageRef, message.copy(id = messageRef.id).toMap())
                .commit()
                .await()
            // Only once the server has it, so the Worker can read the message it's pushing.
            api.notifyInBackground("type" to "message", "chatId" to message.chatId, "messageId" to messageRef.id)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Send failed for ${message.chatId}", e)
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

            val participants = (doc.get("participants") as? List<*>)?.filterIsInstance<String>()
                ?.takeIf { it.isNotEmpty() }
                ?: Chat.participantsOf(doc.id)

            Chat(
                id = doc.id,
                participants = participants,
                participantNames = (doc.get("participantNames") as? Map<*, *>)
                    ?.mapNotNull { (key, value) ->
                        val userId = key as? String ?: return@mapNotNull null
                        val name = value as? String ?: return@mapNotNull null
                        userId to name
                    }
                    ?.toMap()
                    ?: emptyMap(),
                lastMessage = doc.getString("lastMessage") ?: "",
                lastMessageTime = (doc.get("lastMessageTime") as? Number)?.toLong() ?: 0L,
                lastMessageSenderId = doc.getString("lastMessageSenderId") ?: "",
                deletedParticipants = (doc.get("deletedParticipants") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                unreadCounts = unread
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun Message.toMap() = mapOf(
        "chatId" to chatId, "senderId" to senderId,
        "content" to content, "timestamp" to timestamp
    )
}
