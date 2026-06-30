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
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val currentUserId: String? = null,
    val currentUserName: String? = null,
    val newChatId: String? = null,
    val error: String? = null
)

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
    private val userRepository: com.eleazar.localhive.domain.UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState = _uiState.asStateFlow()

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
        viewModelScope.launch {
            messageRepository.getMessages(chatId)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { messages -> _uiState.update { it.copy(messages = messages) } }
        }
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
