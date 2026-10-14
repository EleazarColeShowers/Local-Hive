package com.eleazar.localhive.data.repository

import com.eleazar.localhive.data.platform.logWarning
import com.eleazar.localhive.data.remote.LocalHiveApi
import com.eleazar.localhive.domain.MessageRepository
import com.eleazar.localhive.domain.model.Chat
import com.eleazar.localhive.domain.model.Message
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val TAG = "MessageRepository"

class MessageRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val api: LocalHiveApi
) : MessageRepository {

    private val chats get() = firestore.collection("chats")

    // Sorted on the device rather than with orderBy, so the list doesn't hinge on a
    // composite index being deployed. A user's chat count is small enough for that.
    override fun getChats(userId: String, limit: Long): Flow<List<Chat>> =
        chats
            .where { "participants" contains userId }
            .snapshots
            .map { snapshot ->
                snapshot.documents
                    .mapNotNull { mapDocToChat(it) }
                    .sortedByDescending { it.lastMessageTime }
                    .take(limit.toInt())
            }
            .catch { e ->
                logWarning(TAG, "Chat list listener failed", e)
                throw e
            }

    // Newest-first: keeps the live window bounded to the latest page regardless of history size.
    override fun getMessages(chatId: String, limit: Long): Flow<List<Message>> =
        chats.document(chatId).collection("messages")
            .orderBy("timestamp", Direction.DESCENDING)
            .limit(limit)
            .snapshots
            .map { snapshot -> snapshot.documents.map { it.toMessage() } }
            .catch { e ->
                logWarning(TAG, "Message listener failed for $chatId", e)
                throw e
            }

    override suspend fun getOlderMessages(chatId: String, before: Long, limit: Long): Result<List<Message>> = runCatching {
        chats.document(chatId).collection("messages")
            .orderBy("timestamp", Direction.DESCENDING)
            .startAfter(before)
            .limit(limit)
            .get()
            .documents.map { it.toMessage() }
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

            val chatRef = chats.document(message.chatId)
            val messageRef = chatRef.collection("messages").document

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
                .set(chatRef, chatFields, merge = true)
                .set(messageRef, message.toMap())
                .commit()
            // Only once the server has it, so the Worker can read the message it's pushing.
            api.notifyInBackground("type" to "message", "chatId" to message.chatId, "messageId" to messageRef.id)
            Result.success(Unit)
        } catch (e: Exception) {
            logWarning(TAG, "Send failed for ${message.chatId}", e)
            Result.failure(e)
        }
    }

    override suspend fun markChatRead(chatId: String, userId: String): Result<Unit> = runCatching {
        chats.document(chatId).update("unreadCounts.$userId" to 0)
    }

    private fun mapDocToChat(doc: DocumentSnapshot): Chat? = runCatching {
        Chat(
            id = doc.id,
            participants = doc.stringList("participants")?.takeIf { it.isNotEmpty() } ?: Chat.participantsOf(doc.id),
            participantNames = doc.stringMap("participantNames").orEmpty(),
            lastMessage = doc.string("lastMessage").orEmpty(),
            lastMessageTime = doc.long("lastMessageTime") ?: 0L,
            lastMessageSenderId = doc.string("lastMessageSenderId").orEmpty(),
            deletedParticipants = doc.stringList("deletedParticipants").orEmpty(),
            unreadCounts = doc.intMap("unreadCounts").orEmpty()
        )
    }.getOrNull()

    private fun Message.toMap(): Map<String, Any?> = mapOf(
        "chatId" to chatId, "senderId" to senderId,
        "content" to content, "timestamp" to timestamp
    )
}
