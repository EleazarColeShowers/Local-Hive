package com.eleazar.localhive.ui.estate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.Estate
import com.eleazar.localhive.domain.model.JoinRequest
import com.eleazar.localhive.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EstateUiState(
    val isLoading: Boolean = false,
    val estate: Estate? = null,
    val error: String? = null,
    val success: Boolean = false,
    val searchResults: List<Estate> = emptyList(),
    val isSearching: Boolean = false,
    val requestedEstateIds: Set<String> = emptySet(),
    val pendingRequest: JoinRequest? = null,
    val pendingRequests: List<JoinRequest> = emptyList(),
    val members: List<User> = emptyList(),
    val isApproved: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class EstateViewModel @Inject constructor(
    private val estateRepository: EstateRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EstateUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        val userId = authRepository.getCurrentUserId()
        if (userId != null) {
            viewModelScope.launch {
                estateRepository.getUserJoinRequest(userId).collect { request ->
                    _uiState.update { it.copy(pendingRequest = request) }
                }
            }
        }
        viewModelScope.launch {
            _searchQuery.debounce(300).collect { query ->
                if (_uiState.value.isSearching) performSearch(query)
            }
        }
    }

    fun createEstate(name: String, address: String) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            estateRepository.createEstate(name, address, userId).fold(
                onSuccess = { estate ->
                    _uiState.update { it.copy(isLoading = false, estate = estate, success = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun joinEstate(inviteCode: String) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            estateRepository.joinEstate(inviteCode, userId).fold(
                onSuccess = { estate ->
                    _uiState.update { it.copy(isLoading = false, estate = estate, success = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun searchEstates(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(isSearching = true) }
        if (query.isBlank()) {
            viewModelScope.launch { performSearch("") }
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isLoading = true) }
        estateRepository.searchEstates(query).fold(
            onSuccess = { results ->
                _uiState.update { it.copy(isLoading = false, searchResults = results) }
            },
            onFailure = {
                _uiState.update { it.copy(isLoading = false, searchResults = emptyList()) }
            }
        )
    }

    fun requestToJoin(estateId: String) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val user = userRepository.getUser(userId).getOrNull()
            estateRepository.requestToJoin(
                estateId = estateId,
                userId = userId,
                userName = user?.displayName ?: user?.username ?: "",
                userPhotoUrl = user?.profileImageUrl ?: ""
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(requestedEstateIds = it.requestedEstateIds + estateId) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            )
        }
    }

    fun cancelRequest() {
        val userId = authRepository.getCurrentUserId() ?: return
        val estateId = _uiState.value.pendingRequest?.estateId ?: return
        viewModelScope.launch {
            estateRepository.cancelRequest(estateId, userId)
            _uiState.update { it.copy(pendingRequest = null) }
        }
    }

    fun loadPendingRequests(estateId: String) {
        viewModelScope.launch {
            estateRepository.getPendingRequests(estateId).collect { requests ->
                _uiState.update { it.copy(pendingRequests = requests) }
            }
        }
    }

    fun loadMembers(estateId: String) {
        viewModelScope.launch {
            userRepository.getEstateMembers(estateId).collect { members ->
                _uiState.update { it.copy(members = members) }
            }
        }
    }

    fun approveRequest(estateId: String, userId: String) {
        viewModelScope.launch {
            estateRepository.approveRequest(estateId, userId).fold(
                onSuccess = {},
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun denyRequest(estateId: String, userId: String) {
        viewModelScope.launch {
            estateRepository.denyRequest(estateId, userId).fold(
                onSuccess = {},
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun promoteToAdmin(estateId: String, userId: String) {
        viewModelScope.launch {
            estateRepository.promoteToAdmin(estateId, userId).fold(
                onSuccess = {},
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun loadEstate(estateId: String) {
        viewModelScope.launch {
            estateRepository.getEstate(estateId).fold(
                onSuccess = { estate -> _uiState.update { it.copy(estate = estate) } },
                onFailure = {}
            )
        }
    }

    fun getCurrentUserId(): String? = authRepository.getCurrentUserId()

    fun clearError() = _uiState.update { it.copy(error = null) }
}
