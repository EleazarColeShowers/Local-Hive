package com.eleazar.localhive.ui.directory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DirectoryViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val estateRepository: EstateRepository
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
            _uiState.update { it.copy(isLoading = true) }
            userRepository.getUser(userId, cacheFirst = true).onSuccess { user ->
                user.estateId?.let { estateId ->
                    launch {
                        estateRepository.observeEstate(estateId).collect { estate ->
                            if (estate != null) _uiState.update { it.copy(adminIds = estate.adminIds.toSet()) }
                        }
                    }
                    userRepository.getEstateMembers(estateId)
                        .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                        .collect { members ->
                            _uiState.update { state ->
                                val query = state.searchQuery
                                val filtered = if (query.isBlank()) {
                                    members.filter { it.id != userId }
                                } else {
                                    members.filter { u ->
                                        u.id != userId &&
                                        (u.displayName?.contains(query, ignoreCase = true) == true ||
                                         u.username?.contains(query, ignoreCase = true) == true)
                                    }
                                }
                                state.copy(members = members, filteredMembers = filtered, isLoading = false)
                            }
                        }
                }
            }.onFailure {
                _uiState.update { state -> state.copy(isLoading = false, error = it.message) }
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
