package com.eleazar.localhive.ui.feed

import com.eleazar.localhive.domain.model.Comment
import com.eleazar.localhive.domain.model.Post

/** Everything the feed, post detail and composer screens render. */
data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMorePosts: Boolean = true,
    val error: String? = null,
    val currentUserId: String? = null,
    val currentUserName: String? = null,
    val currentUserImageUrl: String? = null,
    val estateId: String? = null,
    val estateName: String? = null,
    val memberCount: Int = 0,
    /** Current admins, so posts and comments can be badged and the composer can offer announcements. */
    val adminIds: Set<String> = emptySet(),
    val selectedPost: Post? = null,
    val comments: List<Comment> = emptyList(),
    val isPosting: Boolean = false,
    val postCreated: Boolean = false
) {
    val isAdmin: Boolean get() = currentUserId != null && currentUserId in adminIds
}
