package com.eleazar.localhive.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.MessageRepository
import com.eleazar.localhive.domain.model.Chat
import com.eleazar.localhive.domain.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessagesUiState(
    val chats: List<Chat> = emptyList(),
    // Newest-first, matching the reversed chat LazyColumn.
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMoreMessages: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val currentUserId: String? = null,
    val currentUserName: String? = null,
    val newChatId: String? = null,
    val error: String? = null
)

private const val MESSAGE_PAGE_SIZE = 30L

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
    private val userRepository: com.eleazar.localhive.domain.UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState = _uiState.asStateFlow()

    private var liveMessages: List<Message> = emptyList()
    private var olderMessages: List<Message> = emptyList()
    private var messagesJob: kotlinx.coroutines.Job? = null

    init {
        val userId = authRepository.getCurrentUserId()
        _uiState.update { it.copy(currentUserId = userId) }
        userId?.let {
            loadChats(it)
            loadCurrentUserName(it)
        }
    }

    private fun loadCurrentUserName(userId: String) {
        viewModelScope.launch {
            userRepository.getUser(userId).onSuccess { user ->
                _uiState.update { it.copy(currentUserName = user.displayName ?: user.username ?: "Neighbor") }
            }
        }
    }

    private fun loadChats(userId: String) {
        viewModelScope.launch {
            messageRepository.getChats(userId)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { chats -> _uiState.update { it.copy(chats = chats) } }
        }
    }

    fun loadMessages(chatId: String) {
        liveMessages = emptyList()
        olderMessages = emptyList()
        _uiState.update { it.copy(messages = emptyList(), hasMoreMessages = true) }
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            messageRepository.getMessages(chatId, MESSAGE_PAGE_SIZE)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { messages ->
                    liveMessages = messages
                    _uiState.update { it.copy(messages = mergeMessages()) }
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
        olderMessages.forEach { merged.putIfAbsent(it.id, it) }
        return merged.values.sortedByDescending { it.timestamp }
    }

    fun sendMessage(chatId: String, content: String) {
        val userId = _uiState.value.currentUserId ?: return
        viewModelScope.launch {
            val message = Message(
                chatId = chatId,
                senderId = userId,
                content = content,
                timestamp = System.currentTimeMillis()
            )
            messageRepository.sendMessage(message)
        }
    }

    fun startChat(otherUserId: String, otherUserName: String) {
        val currentUserId = _uiState.value.currentUserId ?: return
        val currentUserName = _uiState.value.currentUserName ?: "Neighbor"
        viewModelScope.launch {
            messageRepository.createOrGetChat(currentUserId, currentUserName, otherUserId, otherUserName)
                .onSuccess { chatId -> _uiState.update { it.copy(newChatId = chatId) } }
        }
    }

    fun clearNewChat() = _uiState.update { it.copy(newChatId = null) }
}
