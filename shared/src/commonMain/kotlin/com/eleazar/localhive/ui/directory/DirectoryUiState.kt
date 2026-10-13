package com.eleazar.localhive.ui.directory

import com.eleazar.localhive.domain.model.User

/** What the directory and neighbour profile screens render. */
data class DirectoryUiState(
    val members: List<User> = emptyList(),
    val filteredMembers: List<User> = emptyList(),
    val selectedUser: User? = null,
    val searchQuery: String = "",
    val currentUserId: String? = null,
    val adminIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)
