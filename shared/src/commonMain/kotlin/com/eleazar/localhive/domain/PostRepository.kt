package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.Comment
import com.eleazar.localhive.domain.model.IncidentStatus
import com.eleazar.localhive.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getPostsForEstate(estateId: String, limit: Long = 20): Flow<List<Post>>
    suspend fun getMorePostsForEstate(estateId: String, before: Long, limit: Long = 20): Result<List<Post>>
    /** Returns the new post's id. An emergency's push is sent separately, with [alertEmergency]. */
    suspend fun createPost(post: Post): Result<String>
    /**
     * Pushes a just-created emergency to the estate and waits for it to go out. Returns why it was
     * refused (too many alerts in a short time), or null once sent or if it failed for another reason.
     */
    suspend fun alertEmergency(postId: String): String?
    suspend fun getPost(postId: String): Result<Post>
    /** The post as it changes; null once it's deleted. */
    fun observePost(postId: String): Flow<Post?>
    /** The author rewrites their post's text, and for a security report or emergency, its location. */
    suspend fun updatePost(postId: String, content: String, mentions: Map<String, String>, location: String?): Result<Unit>
    /** Removes the post and its comments; open to its author and the estate's admins. */
    suspend fun deletePost(postId: String): Result<Unit>
    fun getComments(postId: String): Flow<List<Comment>>
    /** Returns the new comment's id. */
    suspend fun addComment(comment: Comment): Result<String>
    suspend fun toggleLike(postId: String, userId: String, currentlyLiked: Boolean): Result<Unit>
    /** Moves a security report or emergency along; open to its reporter and the estate's admins. */
    /** The user is on their way to help with an emergency; tells the person who raised it. */
    suspend fun respondToEmergency(postId: String, userId: String, userName: String): Result<Unit>
    /** The person who raised an emergency says they're safe: resolves it and tells the estate. */
    suspend fun markSafe(postId: String, userId: String, userName: String): Result<Unit>
    suspend fun updateIncidentStatus(postId: String, status: IncidentStatus, userId: String, userName: String): Result<Unit>
}
