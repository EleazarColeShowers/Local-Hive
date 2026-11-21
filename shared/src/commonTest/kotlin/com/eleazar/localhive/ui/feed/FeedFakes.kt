package com.eleazar.localhive.ui.feed

import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.NotificationRepository
import com.eleazar.localhive.domain.PostRepository
import com.eleazar.localhive.domain.SosRepository
import com.eleazar.localhive.domain.StorageRepository
import com.eleazar.localhive.domain.UploadedVideo
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.VideoUploadStage
import com.eleazar.localhive.domain.model.AppNotification
import com.eleazar.localhive.domain.model.BlockedUser
import com.eleazar.localhive.domain.model.Comment
import com.eleazar.localhive.domain.model.Estate
import com.eleazar.localhive.domain.model.IncidentStatus
import com.eleazar.localhive.domain.model.JoinMode
import com.eleazar.localhive.domain.model.JoinRequest
import com.eleazar.localhive.domain.model.Post
import com.eleazar.localhive.domain.model.RemovedMember
import com.eleazar.localhive.domain.model.SosSound
import com.eleazar.localhive.domain.model.SosStanding
import com.eleazar.localhive.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

// In-memory stand-ins for what FeedViewModel talks to. Calls the feed never makes fail loudly.

private fun notUsed(): Nothing = error("Not used by the feed")

class FakeAuthRepository(private val userId: String?) : AuthRepository {
    override fun getCurrentUserId() = userId
    override fun getCurrentUserEmail(): String? = null
    override suspend fun signup(email: String, username: String, password: String): Result<User> = notUsed()
    override suspend fun login(email: String, password: String): Result<User> = notUsed()
    override suspend fun sendPasswordReset(email: String): Result<Unit> = notUsed()
    override suspend fun logout() = notUsed()
    override suspend fun deleteAccount(password: String): Result<Unit> = notUsed()
    override suspend fun getUserProfile(userId: String): Result<User> = notUsed()
    override suspend fun saveUserProfile(user: User): Result<Unit> = notUsed()
}

class FakeUserRepository(private val users: Map<String, User>) : UserRepository {
    val blocked = MutableStateFlow<List<BlockedUser>>(emptyList())

    override suspend fun getUser(userId: String, cacheFirst: Boolean) =
        users[userId]?.let { Result.success(it) } ?: Result.failure(Exception("No user $userId"))
    override fun observeUser(userId: String): Flow<User?> = flowOf(users[userId])
    override fun getEstateMembers(estateId: String): Flow<List<User>> =
        flowOf(users.values.filter { it.estateId == estateId })
    override fun observeDirectory(estateId: String, forceRefresh: Boolean) = getEstateMembers(estateId)
    override fun observeBlockedUsers(userId: String): Flow<List<BlockedUser>> = blocked
    override suspend fun directoryPage(estateId: String, afterId: String?, limit: Int): Result<List<User>> = notUsed()
    override suspend fun searchDirectory(estateId: String, prefix: String, limit: Int): Result<List<User>> = notUsed()
    override suspend fun updateUser(user: User): Result<Unit> = notUsed()
    override suspend fun updateFcmToken(userId: String, token: String): Result<Unit> = notUsed()
    override suspend fun setLightAlerts(userId: String, enabled: Boolean): Result<Unit> = notUsed()
    override suspend fun setNewPostAlerts(userId: String, enabled: Boolean): Result<Unit> = notUsed()
    override suspend fun setSosSound(userId: String, sound: SosSound): Result<Unit> = notUsed()
    override suspend fun setSosPausedUntil(userId: String, until: Long): Result<Unit> = notUsed()
    override suspend fun clearFcmToken(userId: String): Result<Unit> = notUsed()
    override suspend fun blockUser(userId: String, blocked: BlockedUser): Result<Unit> = notUsed()
    override suspend fun unblockUser(userId: String, blockedId: String): Result<Unit> = notUsed()
}

class FakePostRepository : PostRepository {
    val posts = MutableStateFlow<List<Post>>(emptyList())
    val comments = MutableStateFlow<List<Comment>>(emptyList())
    val savedComments = mutableListOf<Comment>()
    var addCommentFails = false
    var likeFails = false
    var latestPosts: Result<List<Post>> = Result.success(emptyList())

    override fun getPostsForEstate(estateId: String, limit: Long): Flow<List<Post>> = posts
    override suspend fun fetchLatestPosts(estateId: String, limit: Long) = latestPosts
    override fun getComments(postId: String): Flow<List<Comment>> = comments
    override fun observePost(postId: String): Flow<Post?> = flowOf(posts.value.find { it.id == postId })
    override suspend fun addComment(comment: Comment): Result<String> {
        if (addCommentFails) return Result.failure(Exception("Offline"))
        savedComments += comment
        return Result.success("c${savedComments.size}")
    }
    override suspend fun toggleLike(postId: String, userId: String, currentlyLiked: Boolean): Result<Unit> =
        if (likeFails) Result.failure(Exception("Offline")) else Result.success(Unit)

    override suspend fun getMorePostsForEstate(estateId: String, before: Long, limit: Long): Result<List<Post>> = notUsed()
    override suspend fun createPost(post: Post): Result<String> = notUsed()
    override suspend fun alertEmergency(postId: String): String? = notUsed()
    override suspend fun getPost(postId: String): Result<Post> = notUsed()
    override suspend fun updatePost(postId: String, content: String, mentions: Map<String, String>, location: String?): Result<Unit> = notUsed()
    override suspend fun deletePost(postId: String): Result<Unit> = notUsed()
    override suspend fun updateComment(postId: String, commentId: String, content: String, mentions: Map<String, String>): Result<Unit> = notUsed()
    override suspend fun deleteComment(postId: String, commentId: String): Result<Unit> = notUsed()
    override suspend fun respondToEmergency(postId: String, userId: String, userName: String): Result<Unit> = notUsed()
    override suspend fun markSafe(postId: String, userId: String, userName: String): Result<Unit> = notUsed()
    override suspend fun updateIncidentStatus(postId: String, status: IncidentStatus, userId: String, userName: String): Result<Unit> = notUsed()
    override suspend fun markFalseAlarm(post: Post, userId: String, userName: String): Result<Unit> = notUsed()
    override suspend fun clearFalseAlarm(post: Post, userId: String, userName: String): Result<Unit> = notUsed()
}

class FakeStorageRepository : StorageRepository {
    var uploadFails = false
    val uploaded = mutableListOf<String>()

    override suspend fun uploadPostImage(userId: String, image: String): Result<String> {
        if (uploadFails) return Result.failure(Exception("Upload failed"))
        uploaded += image
        return Result.success("https://res.cloudinary.com/demo/image/upload/${uploaded.size}.jpg")
    }
    override suspend fun uploadProfileImage(userId: String, image: String): Result<String> = notUsed()
    override suspend fun uploadReceipt(userId: String, image: String): Result<String> = notUsed()
    override suspend fun uploadPostVideo(
        userId: String,
        video: String,
        onProgress: (VideoUploadStage, Float) -> Unit
    ): Result<UploadedVideo> = notUsed()
}

class FakeEstateRepository(private val estate: Estate) : EstateRepository {
    override fun observeEstate(estateId: String): Flow<Estate?> = flowOf(estate.takeIf { it.id == estateId })
    override suspend fun createEstate(name: String, address: String, adminId: String): Result<Estate> = notUsed()
    override suspend fun joinEstate(inviteCode: String, userId: String): Result<Estate> = notUsed()
    override suspend fun getEstate(estateId: String): Result<Estate> = notUsed()
    override suspend fun estateExists(estateId: String): Boolean? = notUsed()
    override suspend fun leaveEstate(estateId: String, userId: String): Result<Unit> = notUsed()
    override suspend fun searchEstates(query: String): Result<List<Estate>> = notUsed()
    override suspend fun findSimilarEstate(name: String, address: String): Result<Estate?> = notUsed()
    override suspend fun requestToJoin(estateId: String, userId: String, userName: String, userPhotoUrl: String): Result<Unit> = notUsed()
    override suspend fun cancelRequest(estateId: String, userId: String): Result<Unit> = notUsed()
    override fun getPendingRequests(estateId: String): Flow<List<JoinRequest>> = notUsed()
    override fun getUserJoinRequest(userId: String): Flow<JoinRequest?> = notUsed()
    override suspend fun approveRequest(estateId: String, userId: String): Result<Unit> = notUsed()
    override suspend fun denyRequest(estateId: String, userId: String): Result<Unit> = notUsed()
    override suspend fun promoteToAdmin(estateId: String, userId: String): Result<Unit> = notUsed()
    override suspend fun removeAdmin(estateId: String, userId: String): Result<Unit> = notUsed()
    override fun observeRemoved(estateId: String): Flow<List<RemovedMember>> = notUsed()
    override suspend fun letBackIn(estateId: String, userId: String): Result<Unit> = notUsed()
    override suspend fun removeMember(estateId: String, userId: String): Result<Unit> = notUsed()
    override suspend fun setTreasurer(estateId: String, userId: String?): Result<Unit> = notUsed()
    override suspend fun updateEstateDetails(estateId: String, name: String, address: String): Result<Unit> = notUsed()
    override suspend fun setJoinMode(estateId: String, mode: JoinMode): Result<Unit> = notUsed()
    override fun observeInviteCode(estateId: String): Flow<String?> = notUsed()
    override suspend fun regenerateInviteCode(estateId: String): Result<String> = notUsed()
    override suspend fun transferOwnership(estateId: String, newOwnerId: String): Result<Unit> = notUsed()
}

class FakeNotificationRepository : NotificationRepository {
    val sent = mutableListOf<AppNotification>()

    override suspend fun createNotification(notification: AppNotification): Result<Unit> {
        sent += notification
        return Result.success(Unit)
    }
    override fun observeNotifications(userId: String): Flow<List<AppNotification>> = notUsed()
    override suspend fun markAsRead(notificationId: String): Result<Unit> = notUsed()
    override suspend fun markAllAsRead(userId: String): Result<Unit> = notUsed()
    override suspend fun deleteNotification(notificationId: String): Result<Unit> = notUsed()
}

class FakeSosRepository : SosRepository {
    override fun observeStanding(estateId: String, userId: String): Flow<SosStanding> = emptyFlow()
    override fun observeStandings(estateId: String): Flow<Map<String, SosStanding>> = notUsed()
    override suspend fun setDisabled(estateId: String, userId: String, disabled: Boolean, adminId: String): Result<Unit> = notUsed()
    override suspend fun restoreSiren(estateId: String, userId: String, adminId: String): Result<Unit> = notUsed()
}
