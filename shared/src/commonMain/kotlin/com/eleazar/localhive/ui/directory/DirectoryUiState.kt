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
    /** How many people the estate has, from the estate itself. */
    val memberCount: Int = 0,
    val isLoading: Boolean = false,
    /** A pull to refresh is fetching the members from the server. */
    val isRefreshing: Boolean = false,
    /**
     * The estate is too big to load whole, so members come a page at a time as the list scrolls
     * and search asks the server.
     */
    val paged: Boolean = false,
    /** Paged: there are members past the ones loaded. */
    val canLoadMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** Paged: the server is looking up the search. */
    val isSearching: Boolean = false,
    val error: String? = null
)
