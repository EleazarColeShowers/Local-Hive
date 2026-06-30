package com.eleazar.localhive.ui.directory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DirectoryUiState(
    val members: List<User> = emptyList(),
    val filteredMembers: List<User> = emptyList(),
    val selectedUser: User? = null,
    val searchQuery: String = "",
    val currentUserId: String? = null,
    val error: String? = null
)

@HiltViewModel
class DirectoryViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DirectoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val userId = authRepository.getCurrentUserId()
        _uiState.update { it.copy(currentUserId = userId) }
        userId?.let { loadMembers(it) }
    }

    private fun loadMembers(userId: String) {
        viewModelScope.launch {
            userRepository.getUser(userId).onSuccess { user ->
                user.estateId?.let { estateId ->
                    userRepository.getEstateMembers(estateId)
                        .catch { e -> _uiState.update { it.copy(error = e.message) } }
                        .collect { members ->
                            _uiState.update { state ->
                                state.copy(
                                    members = members,
                                    filteredMembers = members.filter { it.id != userId }
                                )
                            }
                        }
                }
            }
        }
    }

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            userRepository.getUser(userId).onSuccess { user ->
                _uiState.update { it.copy(selectedUser = user) }
            }
        }
    }

    fun search(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) {
                state.members.filter { it.id != state.currentUserId }
            } else {
                state.members.filter { user ->
                    user.id != state.currentUserId &&
                    (user.displayName?.contains(query, ignoreCase = true) == true ||
                     user.username?.contains(query, ignoreCase = true) == true)
                }
            }
            state.copy(searchQuery = query, filteredMembers = filtered)
        }
    }

    fun clearSelectedUser() = _uiState.update { it.copy(selectedUser = null) }
}
