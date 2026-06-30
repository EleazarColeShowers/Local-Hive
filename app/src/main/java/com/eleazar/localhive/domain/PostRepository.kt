package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.Comment
import com.eleazar.localhive.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getPostsForEstate(estateId: String): Flow<List<Post>>
    suspend fun createPost(post: Post): Result<Unit>
    suspend fun getPost(postId: String): Result<Post>
    fun getComments(postId: String): Flow<List<Comment>>
    suspend fun addComment(comment: Comment): Result<Unit>
    suspend fun toggleLike(postId: String, userId: String, currentlyLiked: Boolean): Result<Unit>
}
