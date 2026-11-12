package com.eleazar.localhive.ui.gate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.GateRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.GuardPost
import com.eleazar.localhive.domain.model.PASS_CODE_LENGTH
import com.eleazar.localhive.domain.model.User
import com.eleazar.localhive.domain.model.VisitorPass
import com.eleazar.localhive.domain.model.isValidCode
import com.eleazar.localhive.domain.model.normalizeCode
import com.eleazar.localhive.ui.util.friendlyMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the guard sees after typing a code. */
sealed interface PassLookup {
    data object Idle : PassLookup
    data object Searching : PassLookup
    data class Found(val pass: VisitorPass) : PassLookup
    /** No usable pass: a wrong code, or one that's expired, cancelled or already used. */
    data class NotFound(val code: String) : PassLookup
}

sealed interface GatePhase {
    data object Loading : GatePhase
    data class OnDuty(val post: GuardPost) : GatePhase
    /** Not a guard anywhere, or an admin has removed them. */
    data object NotGuard : GatePhase
    /** Couldn't tell, usually because the phone is offline. */
    data class Failed(val message: String) : GatePhase
}

data class GateUiState(
    val phase: GatePhase = GatePhase.Loading,
    val guardName: String = "",
    val lookup: PassLookup = PassLookup.Idle,
    /** Visitors checked in and not yet out. */
    val inside: List<VisitorPass> = emptyList(),
    /** The pass being checked in or out, so its button doesn't take a second tap. */
    val busyCode: String? = null,
    val message: String? = null,
    val error: String? = null,
    // Signing on with an admin's code.
    val isRedeeming: Boolean = false,
    val redeemError: String? = null,
    val signedOn: Boolean = false,
    // Leaving, by signing out or giving up the post.
    val signedOut: Boolean = false,
    val leftPost: Boolean = false
)

/**
 * A security guard's view of the estate: the gate and nothing else. Guards aren't members, so
 * everything here goes through [GateRepository], whose rules let them see one pass at a time.
 */
class GateViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val gateRepository: GateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GateUiState())
    val uiState = _uiState.asStateFlow()

    private var user: User? = null
    private var dutyJob: Job? = null

    /** Finds the guard's post and starts watching the gate. Called by the Gate screen. */
    fun load() {
        if (dutyJob != null) return
        val userId = authRepository.getCurrentUserId() ?: run {
            _uiState.update { it.copy(phase = GatePhase.NotGuard) }
            return
        }
        dutyJob = viewModelScope.launch {
            val me = userRepository.getUser(userId).getOrElse { e ->
                _uiState.update { it.copy(phase = GatePhase.Failed(e.friendlyMessage())) }
                dutyJob = null
                return@launch
            }
            user = me
            _uiState.update { it.copy(guardName = guardName(me)) }
            val post = gateRepository.findGuardPost(me).getOrElse { e ->
                _uiState.update { it.copy(phase = GatePhase.Failed(e.friendlyMessage())) }
                dutyJob = null
                return@launch
            }
            if (post == null) {
                _uiState.update { it.copy(phase = GatePhase.NotGuard) }
                return@launch
            }
            _uiState.update { it.copy(phase = GatePhase.OnDuty(post)) }
            launch {
                gateRepository.observeInside(post.estateId)
                    .catch { e -> _uiState.update { it.copy(error = e.friendlyMessage()) } }
                    .collect { inside -> _uiState.update { it.copy(inside = inside) } }
            }
            launch {
                // An admin removing the guard takes the gate away at once.
                gateRepository.observeStillGuard(post.estateId, userId)
                    .catch { }
                    .collect { still -> if (!still) _uiState.update { it.copy(phase = GatePhase.NotGuard, inside = emptyList()) } }
            }
        }
    }

    fun retry() {
        dutyJob?.cancel()
        dutyJob = null
        _uiState.update { it.copy(phase = GatePhase.Loading) }
        load()
    }

    fun lookUp(input: String) {
        val post = onDuty() ?: return
        val code = normalizeCode(input)
        if (!isValidCode(code, PASS_CODE_LENGTH)) {
            _uiState.update { it.copy(lookup = PassLookup.NotFound(code)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(lookup = PassLookup.Searching, error = null) }
            gateRepository.lookUpPass(post.estateId, code).fold(
                onSuccess = { pass ->
                    _uiState.update { it.copy(lookup = pass?.let(PassLookup::Found) ?: PassLookup.NotFound(code)) }
                },
                onFailure = { e -> _uiState.update { it.copy(lookup = PassLookup.Idle, error = e.friendlyMessage()) } }
            )
        }
    }

    fun clearLookup() = _uiState.update { it.copy(lookup = PassLookup.Idle) }

    fun checkIn(pass: VisitorPass) = mark(pass, checkIn = true)

    fun checkOut(pass: VisitorPass) = mark(pass, checkIn = false)

    private fun mark(pass: VisitorPass, checkIn: Boolean) {
        val post = onDuty() ?: return
        val me = user ?: return
        if (_uiState.value.busyCode != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(busyCode = pass.code, error = null) }
            val result = if (checkIn) {
                gateRepository.checkIn(post.estateId, pass.code, me.id, guardName(me))
            } else {
                gateRepository.checkOut(post.estateId, pass.code, me.id, guardName(me))
            }
            result.fold(
                onSuccess = {
                    val done = if (checkIn) "${pass.visitorName} checked in" else "${pass.visitorName} checked out"
                    _uiState.update { it.copy(busyCode = null, lookup = PassLookup.Idle, message = done) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(busyCode = null, error = e.friendlyMessage("Couldn't save that. The pass may have just changed; check the code again."))
                    }
                }
            )
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    /** Signs on as a guard with the code an admin gave them. */
    fun redeem(code: String) {
        val userId = authRepository.getCurrentUserId() ?: return
        if (_uiState.value.isRedeeming) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRedeeming = true, redeemError = null) }
            val me = userRepository.getUser(userId).getOrElse { e ->
                _uiState.update { it.copy(isRedeeming = false, redeemError = e.friendlyMessage()) }
                return@launch
            }
            gateRepository.redeemGuardCode(code, me).fold(
                onSuccess = { _uiState.update { it.copy(isRedeeming = false, signedOn = true) } },
                onFailure = { e ->
                    _uiState.update { it.copy(isRedeeming = false, redeemError = e.friendlyMessage("Couldn't sign you on. Check the code and try again.")) }
                }
            )
        }
    }

    fun clearRedeemError() = _uiState.update { it.copy(redeemError = null) }

    /** Stops guarding this estate. Also tidies up after an admin has already removed them. */
    fun leavePost() {
        val userId = authRepository.getCurrentUserId() ?: return
        val estateId = (_uiState.value.phase as? GatePhase.OnDuty)?.post?.estateId ?: user?.guardEstateId
        viewModelScope.launch {
            if (estateId != null) {
                gateRepository.leaveGuardPost(estateId, userId).onFailure { e ->
                    _uiState.update { it.copy(error = e.friendlyMessage("Couldn't leave. Try again.")) }
                    return@launch
                }
            }
            dutyJob?.cancel()
            _uiState.update { it.copy(leftPost = true) }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            dutyJob?.cancel()
            authRepository.logout()
            _uiState.update { it.copy(signedOut = true) }
        }
    }

    private fun onDuty(): GuardPost? = (_uiState.value.phase as? GatePhase.OnDuty)?.post

    private fun guardName(user: User) = user.displayName ?: user.username ?: "Guard"
}
