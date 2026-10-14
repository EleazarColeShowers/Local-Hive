package com.eleazar.localhive.data.repository

import com.eleazar.localhive.data.remote.LocalHiveApi
import com.eleazar.localhive.domain.PostRepository
import com.eleazar.localhive.domain.model.Comment
import com.eleazar.localhive.domain.model.IncidentStatus
import com.eleazar.localhive.domain.model.Post
import com.eleazar.localhive.domain.model.PostCategory
import com.eleazar.localhive.ui.util.nowMillis
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Firestore's cap on writes in one batch. */
private const val BATCH_LIMIT = 500

class PostRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val api: LocalHiveApi
) : PostRepository {

    private val posts get() = firestore.collection("posts")

    override fun getPostsForEstate(estateId: String, limit: Long): Flow<List<Post>> =
        posts
            .where { "estateId" equalTo estateId }
            .orderBy("createdAt", Direction.DESCENDING)
            .limit(limit)
            .snapshots
            .map { snapshot -> snapshot.documents.map { it.toPost() } }

    override suspend fun getMorePostsForEstate(estateId: String, before: Long, limit: Long): Result<List<Post>> = runCatching {
        posts
            .where { "estateId" equalTo estateId }
            .orderBy("createdAt", Direction.DESCENDING)
            .startAfter(before)
            .limit(limit)
            .get()
            .documents.map { it.toPost() }
    }

    override suspend fun createPost(post: Post): Result<String> = runCatching {
        val ref = posts.document
        ref.set(post.toMap())
        // Pushes for the categories that need one; the Worker re-reads the post to check it.
        when (PostCategory.fromKey(post.category)) {
            PostCategory.ANNOUNCEMENT -> api.notifyInBackground("type" to "announcement", "postId" to ref.id)
            PostCategory.EMERGENCY -> api.notifyInBackground("type" to "emergency", "postId" to ref.id)
            PostCategory.SECURITY -> api.notifyInBackground("type" to "security_report", "postId" to ref.id)
            else -> Unit
        }
        ref.id
    }

    override suspend fun getPost(postId: String): Result<Post> = runCatching {
        val doc = posts.document(postId).get()
        if (!doc.exists) throw Exception("Post not found")
        doc.toPost()
    }

    override fun observePost(postId: String): Flow<Post?> =
        posts.document(postId).snapshots.map { doc -> if (doc.exists) doc.toPost() else null }

    override suspend fun updatePost(
        postId: String,
        content: String,
        mentions: Map<String, String>,
        location: String?
    ): Result<Unit> = runCatching {
        val fields = mutableListOf<Pair<String, Any?>>(
            "content" to content,
            "mentions" to mentions,
            "editedAt" to nowMillis()
        )
        if (location != null) fields += "location" to location
        posts.document(postId).update(*fields.toTypedArray())
    }

    /**
     * Comments go first, in the same batch as the post where they fit: the rules for deleting a
     * comment look the post up, which they can't once it's gone.
     */
    override suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        val post = posts.document(postId)
        val comments = post.collection("comments").get().documents
        val chunks = comments.chunked(BATCH_LIMIT - 1)
        chunks.dropLast(1).forEach { chunk ->
            firestore.batch().apply { chunk.forEach { delete(it.reference) } }.commit()
        }
        firestore.batch().apply {
            chunks.lastOrNull()?.forEach { delete(it.reference) }
            delete(post)
        }.commit()
    }

    override fun getComments(postId: String): Flow<List<Comment>> =
        posts.document(postId).collection("comments")
            .orderBy("createdAt", Direction.ASCENDING)
            .snapshots
            .map { snapshot -> snapshot.documents.map { it.toComment() } }

    override suspend fun addComment(comment: Comment): Result<String> = runCatching {
        val ref = posts.document(comment.postId).collection("comments").document
        ref.set(comment.toMap())
        posts.document(comment.postId).update("commentCount" to FieldValue.increment(1))
        ref.id
    }

    override suspend fun toggleLike(postId: String, userId: String, currentlyLiked: Boolean): Result<Unit> = runCatching {
        val ref = posts.document(postId)
        if (currentlyLiked) {
            ref.update(
                "likedBy" to FieldValue.arrayRemove(userId),
                "likeCount" to FieldValue.increment(-1)
            )
        } else {
            ref.update(
                "likedBy" to FieldValue.arrayUnion(userId),
                "likeCount" to FieldValue.increment(1)
            )
        }
    }

    override suspend fun updateIncidentStatus(
        postId: String,
        status: IncidentStatus,
        userId: String,
        userName: String
    ): Result<Unit> = runCatching {
        posts.document(postId).update(
            "incidentStatus" to status.key,
            "statusUpdatedBy" to userId,
            "statusUpdatedByName" to userName,
            "statusUpdatedAt" to nowMillis()
        )
    }

    override suspend fun respondToEmergency(postId: String, userId: String, userName: String): Result<Unit> = runCatching {
        posts.document(postId).update("responders.$userId" to userName)
        api.notifyInBackground("type" to "sos_responding", "postId" to postId)
    }

    override suspend fun markSafe(postId: String, userId: String, userName: String): Result<Unit> = runCatching {
        updateIncidentStatus(postId, IncidentStatus.RESOLVED, userId, userName).getOrThrow()
        api.notifyInBackground("type" to "sos_safe", "postId" to postId)
    }

    private fun Post.toMap(): Map<String, Any?> = mapOf(
        "authorId" to authorId, "authorName" to authorName,
        "authorImageUrl" to authorImageUrl, "estateId" to estateId,
        "content" to content, "imageUrl" to imageUrl,
        "videoUrl" to videoUrl, "videoThumbnailUrl" to videoThumbnailUrl,
        "createdAt" to createdAt, "likeCount" to likeCount,
        "commentCount" to commentCount, "likedBy" to likedBy,
        "category" to category, "mentions" to mentions
    ) + if (PostCategory.fromKey(category)?.isIncident == true) {
        mapOf(
            "location" to location, "emergencyType" to emergencyType,
            "incidentStatus" to incidentStatus
        )
    } else {
        emptyMap()
    }

    private fun Comment.toMap(): Map<String, Any?> = mapOf(
        "postId" to postId, "authorId" to authorId,
        "authorName" to authorName, "content" to content,
        "createdAt" to createdAt, "mentions" to mentions
    )
}
