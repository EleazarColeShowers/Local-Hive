package com.eleazar.localhive.ui.messages

import com.eleazar.localhive.ui.util.friendlyMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.MessageRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.Chat
import com.eleazar.localhive.domain.model.Message
import com.eleazar.localhive.domain.model.MessageReply
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
    /** Pull to refresh is reconnecting the chat list. */
    val isRefreshingChats: Boolean = false,
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

/** While someone keeps typing, their "typing" mark is renewed this often. */
const val TYPING_REFRESH_MILLIS = 4_000L

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
    /** When this phone last told the other person the user is typing; 0 when it hasn't. */
    private var typingSentAt = 0L

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
    fun loadPeer(userId: String, fresh: Boolean = false) {
        if (userId.isBlank() || !requestedPeers.add(userId)) return
        viewModelScope.launch {
            // Names and photos change rarely; a cached copy beats waiting on the network.
            userRepository.getUser(userId, cacheFirst = !fresh)
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

    private fun loadChats(userId: String, freshPeers: Boolean = false) {
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
                        it.copy(
                            isLoadingChats = false,
                            isRefreshingChats = false,
                            chatsError = e.friendlyMessage("Couldn't load your messages")
                        )
                    }
                }
                .collect { chats ->
                    // A conversation that was opened but never written in isn't worth listing.
                    val started = chats.filter { it.lastMessage.isNotBlank() }
                    _uiState.update {
                        it.copy(
                            chats = started,
                            isLoadingChats = false,
                            isRefreshingChats = false,
                            chatsError = null,
                            totalUnread = started.sumOf { chat -> chat.unreadFor(userId) }
                        )
                    }
                    started.forEach { chat -> loadPeer(chat.otherParticipant(userId), fresh = freshPeers) }
                }
        }
    }

    /** Pull to refresh: reconnects the chat list and fetches names and photos afresh. */
    fun refreshChats() {
        val userId = _uiState.value.currentUserId ?: return
        if (_uiState.value.isRefreshingChats) return
        _uiState.update { it.copy(isRefreshingChats = true) }
        requestedPeers.clear()
        loadChats(userId, freshPeers = true)
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
                    _uiState.update { it.copy(isLoadingMessages = false, error = e.friendlyMessage("Couldn't load messages")) }
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
                    _uiState.update { it.copy(isLoadingMoreMessages = false, error = e.friendlyMessage()) }
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
        val state = _uiState.value
        val userId = state.currentUserId ?: return
        val chat = state.chats.find { it.id == chatId } ?: return
        val seen = chat.readUpTo[userId] ?: 0L
        val newest = state.messages.firstOrNull { it.chatId == chatId }?.timestamp ?: 0L
        if (chat.unreadFor(userId) == 0 && newest <= seen) return
        viewModelScope.launch {
            messageRepository.markChatRead(chatId, userId, maxOf(newest, seen))
        }
    }

    /**
     * The composer's text changed: tells the other person the user is typing, renewed every
     * few seconds while they keep at it rather than on every key, or that they've stopped.
     */
    fun onDraftChanged(chatId: String, text: String) {
        if (text.isBlank()) {
            stopTyping(chatId)
            return
        }
        val userId = _uiState.value.currentUserId ?: return
        // A chat with no messages has no document to write to yet.
        if (_uiState.value.chats.none { it.id == chatId }) return
        val now = nowMillis()
        if (now - typingSentAt < TYPING_REFRESH_MILLIS) return
        typingSentAt = now
        viewModelScope.launch { messageRepository.setTyping(chatId, userId, now) }
    }

    /** Clears the user's typing mark, e.g. when they empty the box or leave the chat. */
    fun stopTyping(chatId: String) {
        if (typingSentAt == 0L) return
        typingSentAt = 0L
        val userId = _uiState.value.currentUserId ?: return
        viewModelScope.launch { messageRepository.setTyping(chatId, userId, 0L) }
    }

    fun sendMessage(chatId: String, content: String, replyTo: Message? = null) {
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
        // The send itself clears the typing mark.
        typingSentAt = 0L
        val message = Message(
            chatId = chatId,
            senderId = userId,
            content = text,
            timestamp = nowMillis(),
            replyTo = replyTo?.let(MessageReply::of)
        )
        viewModelScope.launch {
            messageRepository.sendMessage(message, names).onFailure { e ->
                // The rules refuse a message when either side has blocked the other, or when
                // they're no longer in the same estate.
                val refused = (e as? FirebaseFirestoreException)?.code == FirestoreExceptionCode.PERMISSION_DENIED
                _uiState.update {
                    it.copy(
                        unsentDraft = text,
                        unsentReason = if (refused) "You can't message this person. They may have left your estate." else null
                    )
                }
            }
        }
    }

    fun consumeUnsentDraft() = _uiState.update { it.copy(unsentDraft = null, unsentReason = null) }
}
