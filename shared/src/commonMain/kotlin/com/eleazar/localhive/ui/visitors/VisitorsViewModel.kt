package com.eleazar.localhive.ui.visitors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.GateRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.User
import com.eleazar.localhive.domain.model.VisitorPass
import com.eleazar.localhive.ui.util.friendlyMessage
import com.eleazar.localhive.ui.util.nowMillis
import com.eleazar.localhive.ui.util.startOfDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val HOUR_MILLIS = 60 * 60 * 1000L

/** How many days ahead a resident can make a pass for, today included. Matches the rules' week. */
const val PASS_DAYS_AHEAD = 7

data class VisitorsUiState(
    val isLoading: Boolean = true,
    /** Newest first. */
    val passes: List<VisitorPass> = emptyList(),
    val isCreating: Boolean = false,
    /** The pass just made, shown with its code to share. */
    val created: VisitorPass? = null,
    /** True when the resident hasn't put their address on their profile, which guards use to direct visitors. */
    val missingAddress: Boolean = false,
    val error: String? = null
)

class VisitorsViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val gateRepository: GateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VisitorsUiState())
    val uiState = _uiState.asStateFlow()

    private var user: User? = null

    init {
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            _uiState.update { it.copy(isLoading = false) }
        } else viewModelScope.launch {
            val me = userRepository.getUser(userId, cacheFirst = true).getOrElse { e ->
                _uiState.update { it.copy(isLoading = false, error = e.friendlyMessage()) }
                return@launch
            }
            user = me
            _uiState.update { it.copy(missingAddress = me.address.isNullOrBlank()) }
            val estateId = me.estateId ?: run {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            gateRepository.observeMyPasses(estateId, userId)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.friendlyMessage()) } }
                .collect { passes -> _uiState.update { it.copy(isLoading = false, passes = passes) } }
        }
    }

    /** A pass for [visitorName] on the day [daysFromToday] from today (0 is today). */
    fun createPass(visitorName: String, note: String, daysFromToday: Int) {
        val me = user ?: return
        val estateId = me.estateId ?: return
        if (_uiState.value.isCreating) return
        val (from, until) = dayWindow(daysFromToday.coerceIn(0, PASS_DAYS_AHEAD - 1))
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }
            gateRepository.createPass(estateId, me, visitorName, note, from, until).fold(
                onSuccess = { pass -> _uiState.update { it.copy(isCreating = false, created = pass) } },
                onFailure = { e ->
                    _uiState.update { it.copy(isCreating = false, error = e.friendlyMessage("Couldn't make the pass")) }
                }
            )
        }
    }

    fun dismissCreated() = _uiState.update { it.copy(created = null) }

    fun cancelPass(code: String) = act { estateId -> gateRepository.cancelPass(estateId, code) }

    fun deletePass(code: String) = act { estateId -> gateRepository.deletePass(estateId, code) }

    fun clearError() = _uiState.update { it.copy(error = null) }

    private fun act(block: suspend (estateId: String) -> Result<Unit>) {
        val estateId = user?.estateId ?: return
        viewModelScope.launch {
            block(estateId).onFailure { e -> _uiState.update { it.copy(error = e.friendlyMessage()) } }
        }
    }
}

/**
 * Midnight to midnight, on the device's clock, of the day [daysFromToday] from now. Found from
 * noon of that day, so a clock change overnight can't land it on the wrong date.
 */
fun dayWindow(daysFromToday: Int, now: Long = nowMillis()): Pair<Long, Long> {
    val from = startOfDay(startOfDay(now) + daysFromToday * 24 * HOUR_MILLIS + 12 * HOUR_MILLIS)
    val until = startOfDay(from + 36 * HOUR_MILLIS)
    return from to until
}
