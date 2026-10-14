package com.eleazar.localhive.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Watches the signed-in user's estate while they're inside it, so being removed by an admin
 * takes them out of the estate's screens straight away instead of leaving them on a feed
 * they can no longer read.
 */
class MembershipViewModel(
    authRepository: AuthRepository,
    userRepository: UserRepository
) : ViewModel() {

    private val _removed = MutableStateFlow(false)
    val removed = _removed.asStateFlow()

    init {
        authRepository.getCurrentUserId()?.let { userId ->
            viewModelScope.launch {
                var hadEstate = false
                userRepository.observeUser(userId).collect { user ->
                    // A missing profile is a deleted account or a failed read, not a removal.
                    if (user == null) return@collect
                    if (user.estateId != null) hadEstate = true
                    else if (hadEstate) _removed.value = true
                }
            }
        }
    }
}
