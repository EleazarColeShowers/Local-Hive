package com.eleazar.localhive.ui.feed

import com.eleazar.localhive.domain.model.Comment
import com.eleazar.localhive.domain.model.Post
import com.eleazar.localhive.domain.model.SosStanding
import com.eleazar.localhive.domain.model.User

/** Everything the feed, post detail and composer screens render. */
data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** Pull to refresh is fetching the newest posts from the server. */
    val isRefreshing: Boolean = false,
    /** Why the last pull to refresh failed, shown once. */
    val refreshError: String? = null,
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
    /** The estate's treasurer, who may post announcements too (about dues, say). */
    val treasurerId: String = "",
    /** Other members of the estate, who can be tagged with `@`. */
    val members: List<User> = emptyList(),
    val selectedPost: Post? = null,
    val comments: List<Comment> = emptyList(),
    val isPosting: Boolean = false,
    /** A comment's photo is uploading, so the composer holds on to it until it's sent. */
    val sendingComment: Boolean = false,
    /** Goes up each time a comment is saved, so the composer knows it can let go of its draft. */
    val commentsPosted: Int = 0,
    /** 0–1 while a video is being prepared or uploaded; null when there's no upload to report. */
    val uploadProgress: Float? = null,
    /** The video is being shrunk on the phone, before its upload starts. */
    val preparingVideo: Boolean = false,
    /** A post was published or an edit saved, so the composer can close. */
    val postCreated: Boolean = false,
    /** An SOS was posted to the feed but its push was refused; why, to tell the sender. */
    val emergencyNotPushed: String? = null,
    /** The open post doesn't exist, or has just been deleted by someone. */
    val postMissing: Boolean = false,
    /** The user deleted a post, so a screen showing only that post can close. */
    val postDeleted: Boolean = false,
    /** The user's own SOS standing: switched off by an admin, or sent without the siren. */
    val sosStanding: SosStanding = SosStanding()
) {
    val isAdmin: Boolean get() = currentUserId != null && currentUserId in adminIds

    /** Admins and the treasurer speak for the estate. Matches the rules. */
    val canAnnounce: Boolean get() = isAdmin || (!currentUserId.isNullOrBlank() && currentUserId == treasurerId)
}
