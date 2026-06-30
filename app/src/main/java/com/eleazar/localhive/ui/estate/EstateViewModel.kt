package com.eleazar.localhive.ui.estate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.model.Estate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EstateUiState(
    val isLoading: Boolean = false,
    val estate: Estate? = null,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class EstateViewModel @Inject constructor(
    private val estateRepository: EstateRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EstateUiState())
    val uiState = _uiState.asStateFlow()

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

    fun clearError() = _uiState.update { it.copy(error = null) }
}
