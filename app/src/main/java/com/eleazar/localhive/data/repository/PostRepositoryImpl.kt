package com.eleazar.localhive.data.repository

import com.eleazar.localhive.data.remote.api.LocalHiveApi
import com.eleazar.localhive.domain.PostRepository
import com.eleazar.localhive.domain.model.PostCategory
import com.eleazar.localhive.domain.model.Comment
import com.eleazar.localhive.domain.model.Post
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val api: LocalHiveApi
) : PostRepository {

    override fun getPostsForEstate(estateId: String, limit: Long): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .whereEqualTo("estateId", estateId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getMorePostsForEstate(estateId: String, before: Long, limit: Long): Result<List<Post>> {
        return try {
            val snapshot = firestore.collection("posts")
                .whereEqualTo("estateId", estateId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .startAfter(before)
                .limit(limit)
                .get().await()
            val posts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(id = doc.id)
            }
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createPost(post: Post): Result<Unit> {
        return try {
            val ref = firestore.collection("posts").document()
            firestore.collection("posts").document(ref.id).set(post.copy(id = ref.id).toMap()).await()
            if (post.category == PostCategory.ANNOUNCEMENT.key) {
                api.notifyInBackground("type" to "announcement", "postId" to ref.id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPost(postId: String): Result<Post> {
        return try {
            val doc = firestore.collection("posts").document(postId).get().await()
            val post = doc.toObject(Post::class.java)?.copy(id = doc.id)
                ?: throw Exception("Post not found")
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = firestore.collection("posts").document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val comments = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Comment::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(comments)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addComment(comment: Comment): Result<Unit> {
        return try {
            val ref = firestore.collection("posts").document(comment.postId)
                .collection("comments").document()
            ref.set(comment.copy(id = ref.id).toMap()).await()
            firestore.collection("posts").document(comment.postId)
                .update("commentCount", FieldValue.increment(1)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleLike(postId: String, userId: String, currentlyLiked: Boolean): Result<Unit> {
        return try {
            val ref = firestore.collection("posts").document(postId)
            if (currentlyLiked) {
                ref.update(
                    "likedBy", FieldValue.arrayRemove(userId),
                    "likeCount", FieldValue.increment(-1)
                ).await()
            } else {
                ref.update(
                    "likedBy", FieldValue.arrayUnion(userId),
                    "likeCount", FieldValue.increment(1)
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun Post.toMap() = mapOf(
        "authorId" to authorId, "authorName" to authorName,
        "authorImageUrl" to authorImageUrl, "estateId" to estateId,
        "content" to content, "imageUrl" to imageUrl,
        "createdAt" to createdAt, "likeCount" to likeCount,
        "commentCount" to commentCount, "likedBy" to likedBy,
        "category" to category
    )

    private fun Comment.toMap() = mapOf(
        "postId" to postId, "authorId" to authorId,
        "authorName" to authorName, "content" to content,
        "createdAt" to createdAt
    )
}
