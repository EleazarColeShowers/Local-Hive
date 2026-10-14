package com.eleazar.localhive.ui.feed

import com.eleazar.localhive.domain.model.Comment
import com.eleazar.localhive.domain.model.Post
import com.eleazar.localhive.domain.model.User

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
    /** From the user's profile; where an SOS says they are unless they change it. */
    val currentUserAddress: String? = null,
    val estateId: String? = null,
    val estateName: String? = null,
    val memberCount: Int = 0,
    /** Current admins, so posts and comments can be badged and the composer can offer announcements. */
    val adminIds: Set<String> = emptySet(),
    /** Other members of the estate, who can be tagged with `@`. */
    val members: List<User> = emptyList(),
    val selectedPost: Post? = null,
    val comments: List<Comment> = emptyList(),
    val isPosting: Boolean = false,
    /** 0–1 while a video is uploading; null when there's no upload to report. */
    val uploadProgress: Float? = null,
    /** A post was published or an edit saved, so the composer can close. */
    val postCreated: Boolean = false,
    /** The open post doesn't exist, or has just been deleted by someone. */
    val postMissing: Boolean = false,
    /** The user deleted a post, so a screen showing only that post can close. */
    val postDeleted: Boolean = false
) {
    val isAdmin: Boolean get() = currentUserId != null && currentUserId in adminIds
}
