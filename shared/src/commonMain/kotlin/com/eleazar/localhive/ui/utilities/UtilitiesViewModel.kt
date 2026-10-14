package com.eleazar.localhive.ui.utilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.UtilityRepository
import com.eleazar.localhive.domain.model.UtilityKind
import com.eleazar.localhive.domain.model.UtilityReport
import com.eleazar.localhive.domain.model.UtilityStatus
import com.eleazar.localhive.ui.util.nowMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

data class UtilitiesUiState(
    val estateId: String? = null,
    val statuses: Map<UtilityKind, UtilityStatus> = emptyMap(),
    /** Newest first. */
    val reports: List<UtilityReport> = emptyList(),
    /** How long each supply was on over the last day, where anyone reported on it. */
    val lastDay: Map<UtilityKind, Uptime> = emptyMap(),
    /** Supplies with a report on its way, so their buttons don't take a second tap. */
    val reporting: Set<UtilityKind> = emptySet(),
    /** The user gets a push when someone reports the light is back. */
    val lightAlerts: Boolean = false,
    val error: String? = null
)

class UtilitiesViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val utilityRepository: UtilityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UtilitiesUiState())
    val uiState = _uiState.asStateFlow()

    private var userName = "A neighbour"

    init {
        val userId = authRepository.getCurrentUserId()
        if (userId != null) viewModelScope.launch {
            val user = userRepository.getUser(userId, cacheFirst = true).getOrNull() ?: return@launch
            userName = user.displayName ?: user.username ?: userName
            _uiState.update { it.copy(lightAlerts = user.lightAlerts) }
            val estateId = user.estateId ?: return@launch
            _uiState.update { it.copy(estateId = estateId) }
            launch {
                utilityRepository.observeStatuses(estateId)
                    .catch { e -> _uiState.update { it.copy(error = e.message) } }
                    .collect { statuses -> _uiState.update { it.copy(statuses = statuses) } }
            }
            launch {
                utilityRepository.observeReports(estateId)
                    .catch { e -> _uiState.update { it.copy(error = e.message) } }
                    .collect { reports ->
                        val now = nowMillis()
                        val lastDay = UtilityKind.entries.mapNotNull { kind ->
                            uptime(reports.filter { it.kind == kind }, DAY_MILLIS, now)?.let { kind to it }
                        }.toMap()
                        _uiState.update { it.copy(reports = reports, lastDay = lastDay) }
                    }
            }
        }
    }

    fun setLightAlerts(enabled: Boolean) {
        val userId = authRepository.getCurrentUserId() ?: return
        _uiState.update { it.copy(lightAlerts = enabled) }
        viewModelScope.launch {
            userRepository.setLightAlerts(userId, enabled).onFailure { e ->
                _uiState.update { it.copy(lightAlerts = !enabled, error = e.message ?: "Couldn't save that") }
            }
        }
    }

    fun report(kind: UtilityKind, isOn: Boolean) {
        val state = _uiState.value
        val estateId = state.estateId ?: return
        val userId = authRepository.getCurrentUserId() ?: return
        if (kind in state.reporting) return
        viewModelScope.launch {
            _uiState.update { it.copy(reporting = it.reporting + kind, error = null) }
            utilityRepository.report(estateId, kind, isOn, state.statuses[kind], userId, userName)
                .onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Couldn't send your report") } }
            _uiState.update { it.copy(reporting = it.reporting - kind) }
        }
    }
}
