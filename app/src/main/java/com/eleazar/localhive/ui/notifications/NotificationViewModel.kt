package com.eleazar.localhive.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.NotificationRepository
import com.eleazar.localhive.domain.model.AppNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val notifications: List<AppNotification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val unreadCount: Int get() = notifications.count { !it.isRead }
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val userId = authRepository.getCurrentUserId()
        if (userId != null) {
            _uiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                notificationRepository.observeNotifications(userId)
                    .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load notifications") } }
                    .collect { notifications ->
                        _uiState.update { it.copy(notifications = notifications, isLoading = false, error = null) }
                    }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch { notificationRepository.markAsRead(notificationId) }
    }

    fun markAllAsRead() {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch { notificationRepository.markAllAsRead(userId) }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch { notificationRepository.deleteNotification(notificationId) }
    }
}
