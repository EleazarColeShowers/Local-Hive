package com.eleazar.localhive.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.MessageRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.Chat
import com.eleazar.localhive.domain.model.Message
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.code
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.eleazar.localhive.ui.util.nowMillis

data class MessagesUiState(
    /** Conversations with at least one message, most recent first. */
    val chats: List<Chat> = emptyList(),
    val isLoadingChats: Boolean = true,
    val chatsError: String? = null,
    /** Names and photos of the people the user talks to, keyed by user id. */
    val peers: Map<String, ChatPeer> = emptyMap(),
    // Newest-first, matching the reversed chat LazyColumn.
    val messages: List<Message> = emptyList(),
    val isLoadingMessages: Boolean = false,
    val isLoadingMoreMessages: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val currentUserId: String? = null,
    val currentUserName: String? = null,
    val totalUnread: Int = 0,
    /** Text of a message that failed to send, handed back so the user can retry it. */
    val unsentDraft: String? = null,
    /** Why [unsentDraft] didn't go, when it isn't a connection problem. */
    val unsentReason: String? = null,
    val error: String? = null
)

private const val MESSAGE_PAGE_SIZE = 30L

class MessagesViewModel(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState = _uiState.asStateFlow()

    private var liveMessages: List<Message> = emptyList()
    private var olderMessages: List<Message> = emptyList()
    private var messagesJob: Job? = null
    private var chatsJob: Job? = null
    private val requestedPeers = mutableSetOf<String>()

    init {
        val userId = authRepository.getCurrentUserId()
        _uiState.update { it.copy(currentUserId = userId, isLoadingChats = userId != null) }
        if (userId != null) {
            loadChats(userId)
            loadCurrentUserName(userId)
        }
    }

    private fun loadCurrentUserName(userId: String) {
        viewModelScope.launch {
            userRepository.getUser(userId, cacheFirst = true).onSuccess { user ->
                _uiState.update { it.copy(currentUserName = user.displayName ?: user.username) }
            }
        }
    }

    /** Fetches the profile behind [userId] once, for names and avatars. */
    fun loadPeer(userId: String) {
        if (userId.isBlank() || !requestedPeers.add(userId)) return
        viewModelScope.launch {
            // Names and photos change rarely; a cached copy beats waiting on the network.
            userRepository.getUser(userId, cacheFirst = true)
                .onSuccess { user ->
                    val peer = ChatPeer(
                        name = user.displayName?.takeIf { it.isNotBlank() }
                            ?: user.username?.takeIf { it.isNotBlank() },
                        photoUrl = user.profileImageUrl
                    )
                    _uiState.update { it.copy(peers = it.peers + (userId to peer)) }
                }
                .onFailure { requestedPeers.remove(userId) }
        }
    }

    private fun loadChats(userId: String) {
        chatsJob?.cancel()
        chatsJob = viewModelScope.launch {
            // Conversations with blocked people drop out of the list (and the unread badge).
            messageRepository.getChats(userId)
                .combine(userRepository.observeBlockedUsers(userId)) { chats, blocked ->
                    val blockedIds = blocked.mapTo(HashSet()) { it.id }
                    chats.filter { it.otherParticipant(userId) !in blockedIds }
                }
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoadingChats = false, chatsError = e.message ?: "Couldn't load your messages")
                    }
                }
                .collect { chats ->
                    // A conversation that was opened but never written in isn't worth listing.
                    val started = chats.filter { it.lastMessage.isNotBlank() }
                    _uiState.update {
                        it.copy(
                            chats = started,
                            isLoadingChats = false,
                            chatsError = null,
                            totalUnread = started.sumOf { chat -> chat.unreadFor(userId) }
                        )
                    }
                    started.forEach { chat -> loadPeer(chat.otherParticipant(userId)) }
                }
        }
    }

    fun retryChats() {
        val userId = _uiState.value.currentUserId ?: return
        _uiState.update { it.copy(isLoadingChats = true, chatsError = null) }
        loadChats(userId)
    }

    fun openChat(chatId: String) {
        val userId = _uiState.value.currentUserId
        if (userId != null) loadPeer(Chat.participantsOf(chatId).firstOrNull { it != userId } ?: "")

        liveMessages = emptyList()
        olderMessages = emptyList()
        _uiState.update {
            it.copy(messages = emptyList(), isLoadingMessages = true, hasMoreMessages = true, error = null)
        }
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            messageRepository.getMessages(chatId, MESSAGE_PAGE_SIZE)
                .catch { e ->
                    _uiState.update { it.copy(isLoadingMessages = false, error = e.message ?: "Couldn't load messages") }
                }
                .collect { messages ->
                    liveMessages = messages
                    _uiState.update {
                        it.copy(
                            messages = mergeMessages(),
                            isLoadingMessages = false,
                            // A short first page means there is no older history to page in.
                            hasMoreMessages = if (olderMessages.isEmpty()) {
                                messages.size.toLong() == MESSAGE_PAGE_SIZE
                            } else {
                                it.hasMoreMessages
                            }
                        )
                    }
                }
        }
    }

    fun loadOlderMessages(chatId: String) {
        val state = _uiState.value
        if (state.isLoadingMoreMessages || !state.hasMoreMessages) return
        val oldest = mergeMessages().lastOrNull()?.timestamp ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreMessages = true) }
            messageRepository.getOlderMessages(chatId, oldest, MESSAGE_PAGE_SIZE)
                .onSuccess { more ->
                    olderMessages = olderMessages + more
                    _uiState.update {
                        it.copy(
                            messages = mergeMessages(),
                            isLoadingMoreMessages = false,
                            hasMoreMessages = more.size.toLong() == MESSAGE_PAGE_SIZE
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoadingMoreMessages = false, error = e.message) }
                }
        }
    }

    private fun mergeMessages(): List<Message> {
        val merged = LinkedHashMap<String, Message>()
        liveMessages.forEach { merged[it.id] = it }
        olderMessages.forEach { merged.getOrPut(it.id) { it } }
        return merged.values.sortedByDescending { it.timestamp }
    }

    /**
     * Called when a chat is on screen, including as new messages land in it, so the
     * badge does not reappear for a conversation the user is already reading.
     */
    fun markChatRead(chatId: String) {
        val userId = _uiState.value.currentUserId ?: return
        val chat = _uiState.value.chats.find { it.id == chatId } ?: return
        if (chat.unreadFor(userId) == 0) return
        viewModelScope.launch {
            messageRepository.markChatRead(chatId, userId)
        }
    }

    fun sendMessage(chatId: String, content: String) {
        val text = content.trim()
        val userId = _uiState.value.currentUserId ?: return
        if (text.isEmpty()) return
        val otherUserId = Chat.participantsOf(chatId).firstOrNull { it != userId } ?: return

        val state = _uiState.value
        val names = buildMap {
            state.currentUserName?.let { put(userId, it) }
            (state.peers[otherUserId]?.name ?: state.chats.find { it.id == chatId }?.participantNames?.get(otherUserId))
                ?.let { put(otherUserId, it) }
        }
        val message = Message(
            chatId = chatId,
            senderId = userId,
            content = text,
            timestamp = nowMillis()
        )
        viewModelScope.launch {
            messageRepository.sendMessage(message, names).onFailure { e ->
                // The rules refuse a message when either side has blocked the other.
                val refused = (e as? FirebaseFirestoreException)?.code == FirestoreExceptionCode.PERMISSION_DENIED
                _uiState.update {
                    it.copy(unsentDraft = text, unsentReason = if (refused) "You can't message this person." else null)
                }
            }
        }
    }

    fun consumeUnsentDraft() = _uiState.update { it.copy(unsentDraft = null, unsentReason = null) }
}
