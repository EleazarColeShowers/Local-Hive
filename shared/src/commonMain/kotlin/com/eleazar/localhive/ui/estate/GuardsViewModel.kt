package com.eleazar.localhive.ui.estate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.GateRepository
import com.eleazar.localhive.domain.model.Guard
import com.eleazar.localhive.domain.model.GuardInvite
import com.eleazar.localhive.ui.util.friendlyMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GuardsUiState(
    val guards: List<Guard> = emptyList(),
    val isCreatingInvite: Boolean = false,
    /** A code just made, shown once for the admin to give the guard. */
    val invite: GuardInvite? = null,
    val error: String? = null
)

/** An estate admin deciding who works the gate. */
class GuardsViewModel(
    private val authRepository: AuthRepository,
    private val gateRepository: GateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuardsUiState())
    val uiState = _uiState.asStateFlow()

    private var watching: Job? = null

    fun load(estateId: String) {
        if (watching != null) return
        watching = viewModelScope.launch {
            gateRepository.observeGuards(estateId).collect { guards -> _uiState.update { it.copy(guards = guards) } }
        }
    }

    fun createInvite(estateId: String) {
        val adminId = authRepository.getCurrentUserId() ?: return
        if (_uiState.value.isCreatingInvite) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingInvite = true, error = null) }
            gateRepository.createGuardInvite(estateId, adminId).fold(
                onSuccess = { invite -> _uiState.update { it.copy(isCreatingInvite = false, invite = invite) } },
                onFailure = { e ->
                    _uiState.update { it.copy(isCreatingInvite = false, error = e.friendlyMessage("Couldn't make a guard code")) }
                }
            )
        }
    }

    fun dismissInvite() = _uiState.update { it.copy(invite = null) }

    fun removeGuard(estateId: String, guardId: String) {
        viewModelScope.launch {
            gateRepository.removeGuard(estateId, guardId).onFailure { e ->
                _uiState.update { it.copy(error = e.friendlyMessage("Couldn't remove that guard")) }
            }
        }
    }
}
