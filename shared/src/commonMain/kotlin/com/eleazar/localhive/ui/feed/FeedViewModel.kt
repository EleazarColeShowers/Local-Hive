package com.eleazar.localhive.ui.feed

import com.eleazar.localhive.ui.util.friendlyMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.NotificationRepository
import com.eleazar.localhive.domain.PostRepository
import com.eleazar.localhive.domain.StorageRepository
import com.eleazar.localhive.domain.UploadedVideo
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.AppNotification
import com.eleazar.localhive.domain.model.Comment
import com.eleazar.localhive.domain.model.EmergencyType
import com.eleazar.localhive.domain.model.IncidentStatus
import com.eleazar.localhive.domain.model.NotificationType
import com.eleazar.localhive.domain.model.Post
import com.eleazar.localhive.domain.model.PostCategory
import com.eleazar.localhive.domain.model.User
import com.eleazar.localhive.domain.model.isEditable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import com.eleazar.localhive.domain.SosRepository
import com.eleazar.localhive.ui.util.nowMillis

private const val POST_PAGE_SIZE = 20L
private const val REFRESH_TIMEOUT_MS = 15_000L
/** Long enough for the pull indicator to register that a refresh happened. */
private const val MIN_REFRESH_MS = 400L

class FeedViewModel(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val storageRepository: StorageRepository,
    private val estateRepository: EstateRepository,
    private val notificationRepository: NotificationRepository,
    private val sosRepository: SosRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState = _uiState.asStateFlow()

    private var livePosts: List<Post> = emptyList()
    private var olderPosts: List<Post> = emptyList()
    private var allComments: List<Comment> = emptyList()
    /** Authors whose posts and comments the user has chosen not to see. */
    private var blockedIds: Set<String> = emptySet()
    private var allMembers: List<User> = emptyList()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        val userId = authRepository.getCurrentUserId() ?: return
        _uiState.update { it.copy(currentUserId = userId) }
        viewModelScope.launch {
            userRepository.observeBlockedUsers(userId).collect { blocked ->
                blockedIds = blocked.mapTo(HashSet()) { it.id }
                _uiState.update { it.copy(posts = mergePosts(), comments = visibleComments(), members = taggableMembers()) }
            }
        }
        viewModelScope.launch {
            userRepository.getUser(userId, cacheFirst = true).onSuccess { user ->
                _uiState.update {
                    it.copy(
                        currentUserName = user.displayName ?: user.username,
                        currentUserImageUrl = user.profileImageUrl,
                        currentUserAddress = user.address,
                        estateId = user.estateId
                    )
                }
                user.estateId?.let {
                    loadPosts(it)
                    loadEstate(it)
                    loadMembers(it)
                    loadSosStanding(it, userId)
                }
            }
        }
    }

    /** Whether an admin has turned off the user's SOS, or false alarms have cost them the siren. */
    private fun loadSosStanding(estateId: String, userId: String) {
        viewModelScope.launch {
            sosRepository.observeStanding(estateId, userId)
                .catch { /* Without it the SOS screen just doesn't warn; the rules still decide. */ }
                .collect { standing -> _uiState.update { it.copy(sosStanding = standing) } }
        }
    }

    private fun loadEstate(estateId: String) {
        viewModelScope.launch {
            // Live: being made (or unmade) an admin takes effect without a restart.
            estateRepository.observeEstate(estateId).collect { estate ->
                if (estate != null) {
                    _uiState.update {
                        it.copy(estateName = estate.name, memberCount = estate.memberCount, adminIds = estate.adminIds.toSet())
                    }
                }
            }
        }
    }

    private fun loadMembers(estateId: String) {
        viewModelScope.launch {
            userRepository.getEstateMembers(estateId)
                .catch { /* Tagging just offers no suggestions; posting still works. */ }
                .collect { members ->
                    allMembers = members
                    _uiState.update { it.copy(members = taggableMembers()) }
                }
        }
    }

    /** Everyone in the estate but the user themselves and anyone they've blocked. */
    private fun taggableMembers(): List<User> {
        val me = _uiState.value.currentUserId
        return allMembers.filter { it.id != me && it.id !in blockedIds }
    }

    private fun loadPosts(estateId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            postRepository.getPostsForEstate(estateId, POST_PAGE_SIZE)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.friendlyMessage()) } }
                .collect { posts ->
                    livePosts = posts
                    val merged = mergePosts()
                    _uiState.update { state ->
                        state.copy(
                            posts = merged,
                            isLoading = false,
                            // Keep an open post's likes and comment count live too.
                            selectedPost = state.selectedPost?.let { open -> merged.find { it.id == open.id } ?: open }
                        )
                    }
                }
        }
    }

    fun loadMorePosts() {
        val state = _uiState.value
        val estateId = state.estateId ?: return
        if (state.isLoadingMore || !state.hasMorePosts) return
        // Paged by everything loaded, blocked authors included, so hidden posts don't shift the cursor.
        val oldest = (livePosts + olderPosts).minOfOrNull { it.createdAt } ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            postRepository.getMorePostsForEstate(estateId, oldest, POST_PAGE_SIZE)
                .onSuccess { more ->
                    olderPosts = olderPosts + more
                    _uiState.update {
                        it.copy(
                            posts = mergePosts(),
                            isLoadingMore = false,
                            hasMorePosts = more.size.toLong() == POST_PAGE_SIZE
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoadingMore = false, error = e.friendlyMessage()) }
                }
        }
    }

    /**
     * Pull to refresh: the newest page straight from the server, dropping older pages so the feed
     * starts over from the top. The live listener keeps it current from there.
     */
    fun refresh() {
        val state = _uiState.value
        if (state.isRefreshing) return
        // Always flip isRefreshing on and back off: the pull indicator only slides away when it
        // sees that change, and stays stuck at the top if the flag never moves or moves too fast.
        _uiState.update { it.copy(isRefreshing = true, refreshError = null) }
        viewModelScope.launch {
            val started = nowMillis()
            val estateId = state.estateId
            // A server read on a weak connection can wait for a long time; give up rather than spin forever.
            val result = if (estateId == null) Result.success(null)
            else withTimeoutOrNull(REFRESH_TIMEOUT_MS) { postRepository.fetchLatestPosts(estateId, POST_PAGE_SIZE) }
                ?: Result.failure(Exception("Refresh timed out"))
            delay((MIN_REFRESH_MS - (nowMillis() - started)).coerceAtLeast(0))
            result
                .onSuccess { fresh ->
                    if (fresh == null) {
                        _uiState.update { it.copy(isRefreshing = false) }
                        return@onSuccess
                    }
                    livePosts = fresh
                    olderPosts = emptyList()
                    _uiState.update {
                        it.copy(
                            posts = mergePosts(),
                            isRefreshing = false,
                            hasMorePosts = fresh.size.toLong() == POST_PAGE_SIZE
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isRefreshing = false, refreshError = "Couldn't refresh. Check your connection and try again.")
                    }
                }
        }
    }

    fun consumeRefreshError() {
        _uiState.update { it.copy(refreshError = null) }
    }

    private fun mergePosts(): List<Post> {
        val merged = LinkedHashMap<String, Post>()
        livePosts.forEach { merged[it.id] = it }
        olderPosts.forEach { merged.getOrPut(it.id) { it } }
        return merged.values.filter { it.authorId !in blockedIds }.sortedByDescending { it.createdAt }
    }

    private fun visibleComments(): List<Comment> = allComments.filter { it.authorId !in blockedIds }

    fun createPost(
        content: String,
        mediaUri: String? = null,
        isVideo: Boolean = false,
        category: PostCategory? = null,
        mentions: Map<String, String> = emptyMap(),
        location: String = ""
    ) {
        val state = _uiState.value
        val estateId = state.estateId ?: return
        val userId = state.currentUserId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true, error = null, uploadProgress = if (isVideo) 0f else null) }

            fun fail(e: Throwable, fallback: String) {
                _uiState.update { it.copy(isPosting = false, uploadProgress = null, error = e.friendlyMessage(fallback)) }
            }

            // Upload first: a post that referenced a failed upload would render a broken image.
            var imageUrl: String? = null
            var video: UploadedVideo? = null
            if (mediaUri != null && isVideo) {
                video = storageRepository.uploadPostVideo(userId, mediaUri) { progress ->
                    _uiState.update { it.copy(uploadProgress = progress) }
                }.getOrElse { e -> fail(e, "Couldn't upload the video"); return@launch }
            } else if (mediaUri != null) {
                imageUrl = storageRepository.uploadPostImage(userId, mediaUri)
                    .getOrElse { e -> fail(e, "Couldn't upload the photo"); return@launch }
            }

            val post = Post(
                authorId = userId,
                authorName = state.currentUserName ?: "Neighbor",
                authorImageUrl = state.currentUserImageUrl,
                estateId = estateId,
                content = content,
                imageUrl = imageUrl,
                videoUrl = video?.url,
                videoThumbnailUrl = video?.thumbnailUrl,
                createdAt = nowMillis(),
                category = category?.key.orEmpty(),
                mentions = mentions,
                location = if (category?.isIncident == true) location else "",
                incidentStatus = if (category?.isIncident == true) IncidentStatus.OPEN.key else ""
            )
            postRepository.createPost(post)
                .onSuccess { postId ->
                    notifyMentioned(mentions.keys, estateId, postId, commentId = null, content)
                    _uiState.update { it.copy(isPosting = false, uploadProgress = null, postCreated = true) }
                }
                .onFailure { e -> fail(e, "Failed to create post") }
        }
    }

    /**
     * Raises an SOS: a post that every member sees, pushed to all of them at once. Sent as soon
     * as it's tapped, with no photo to upload first, so nothing slows it down.
     */
    fun sendEmergency(type: EmergencyType, location: String, details: String) {
        val state = _uiState.value
        val estateId = state.estateId ?: return
        val userId = state.currentUserId ?: return
        if (state.isPosting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true, error = null) }
            val post = Post(
                authorId = userId,
                authorName = state.currentUserName ?: "Neighbor",
                authorImageUrl = state.currentUserImageUrl,
                estateId = estateId,
                content = details,
                createdAt = nowMillis(),
                category = PostCategory.EMERGENCY.key,
                location = location,
                emergencyType = type.key,
                incidentStatus = IncidentStatus.OPEN.key
            )
            postRepository.createPost(post)
                .onSuccess { postId ->
                    val refused = postRepository.alertEmergency(postId)
                    _uiState.update {
                        if (refused != null) it.copy(isPosting = false, emergencyNotPushed = refused)
                        else it.copy(isPosting = false, postCreated = true)
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isPosting = false, error = e.friendlyMessage("Couldn't send the alert")) }
                }
        }
    }

    /** Shows the new status straight away, and puts the old one back if the write fails. */
    fun updateIncidentStatus(postId: String, status: IncidentStatus) {
        val state = _uiState.value
        val userId = state.currentUserId ?: return
        val post = state.posts.find { it.id == postId }
            ?: state.selectedPost?.takeIf { it.id == postId }
            ?: return
        val name = state.currentUserName ?: "A neighbour"
        replacePost(post.copy(incidentStatus = status.key, statusUpdatedByName = name, statusUpdatedAt = nowMillis()))
        viewModelScope.launch {
            postRepository.updateIncidentStatus(postId, status, userId, name).onFailure { e ->
                replacePost(post)
                _uiState.update { it.copy(error = e.friendlyMessage("Couldn't update the status")) }
            }
        }
    }

    /**
     * Saves the author's changes to a post. Anyone newly tagged is told, as on a new post;
     * people already tagged aren't notified twice.
     */
    fun editPost(postId: String, content: String, mentions: Map<String, String>, location: String) {
        val state = _uiState.value
        val post = state.selectedPost?.takeIf { it.id == postId } ?: return
        if (state.isPosting) return
        if (!isEditable(post.createdAt, nowMillis())) {
            _uiState.update { it.copy(error = "Posts can only be edited for 30 minutes after posting") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true, error = null) }
            val isIncident = PostCategory.fromKey(post.category)?.isIncident == true
            postRepository.updatePost(postId, content, mentions, location.takeIf { isIncident })
                .onSuccess {
                    notifyMentioned(mentions.keys - post.mentions.keys, post.estateId, postId, commentId = null, content)
                    _uiState.update { it.copy(isPosting = false, postCreated = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isPosting = false, error = e.friendlyMessage("Couldn't save your changes")) }
                }
        }
    }

    /**
     * Deletes a post the user wrote, or, as an admin, anyone's post in the estate. An author whose
     * post an admin removed is told, with the admin's [reason] if they gave one.
     */
    fun deletePost(postId: String, reason: String = "") {
        val state = _uiState.value
        val post = state.posts.find { it.id == postId } ?: state.selectedPost?.takeIf { it.id == postId }
        viewModelScope.launch {
            postRepository.deletePost(postId)
                .onSuccess {
                    if (post != null && post.authorId != state.currentUserId) notifyRemoved(post, reason)
                    // The live first page drops it on its own; older pages were fetched once.
                    olderPosts = olderPosts.filter { it.id != postId }
                    _uiState.update { it.copy(posts = mergePosts(), postDeleted = true) }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.friendlyMessage("Couldn't delete the post")) } }
        }
    }

    private fun notifyRemoved(post: Post, reason: String) {
        viewModelScope.launch {
            val excerpt = post.content.trim().take(80).let { if (post.content.trim().length > 80) "$it…" else it }
            notificationRepository.createNotification(
                AppNotification(
                    recipientId = post.authorId,
                    type = NotificationType.POST_REMOVED,
                    title = "An admin removed your post",
                    message = when {
                        reason.isNotBlank() -> "Reason: ${reason.trim()}"
                        excerpt.isNotBlank() -> "\"$excerpt\""
                        else -> "It no longer appears in the feed."
                    },
                    estateId = post.estateId,
                    relatedUserId = _uiState.value.currentUserId
                )
            )
        }
    }

    /** "I'm coming": shows straight away, and tells the person who raised the alert. */
    fun respondToEmergency(postId: String) {
        val state = _uiState.value
        val userId = state.currentUserId ?: return
        val post = state.selectedPost?.takeIf { it.id == postId } ?: state.posts.find { it.id == postId } ?: return
        if (userId in post.responders) return
        val name = state.currentUserName ?: "A neighbour"
        replacePost(post.copy(responders = post.responders + (userId to name)))
        viewModelScope.launch {
            postRepository.respondToEmergency(postId, userId, name).onFailure { e ->
                replacePost(post)
                _uiState.update { it.copy(error = e.friendlyMessage("Couldn't let them know")) }
            }
        }
    }

    /** "I'm safe now", from the person who raised the alert: resolves it and tells the estate. */
    fun markSafe(postId: String) {
        val state = _uiState.value
        val userId = state.currentUserId ?: return
        val post = state.selectedPost?.takeIf { it.id == postId } ?: state.posts.find { it.id == postId } ?: return
        val name = state.currentUserName ?: "A neighbour"
        replacePost(post.copy(incidentStatus = IncidentStatus.RESOLVED.key, statusUpdatedByName = name, statusUpdatedAt = nowMillis()))
        viewModelScope.launch {
            postRepository.markSafe(postId, userId, name).onFailure { e ->
                replacePost(post)
                _uiState.update { it.copy(error = e.friendlyMessage("Couldn't update the alert")) }
            }
        }
    }

    /** Marks an emergency a false alarm: shows straight away, and puts it back if the write fails. */
    fun markFalseAlarm(postId: String) {
        val state = _uiState.value
        val userId = state.currentUserId ?: return
        val post = state.selectedPost?.takeIf { it.id == postId } ?: state.posts.find { it.id == postId } ?: return
        if (post.falseAlarm) return
        val name = state.currentUserName ?: "A neighbour"
        replacePost(
            post.copy(
                falseAlarm = true, falseAlarmByName = name, incidentStatus = IncidentStatus.RESOLVED.key,
                statusUpdatedByName = name, statusUpdatedAt = nowMillis()
            )
        )
        viewModelScope.launch {
            postRepository.markFalseAlarm(post, userId, name).onFailure { e ->
                replacePost(post)
                _uiState.update { it.copy(error = e.friendlyMessage("Couldn't mark it as a false alarm")) }
            }
        }
    }

    /** An admin takes the false-alarm mark back off. */
    fun clearFalseAlarm(postId: String) {
        val state = _uiState.value
        val userId = state.currentUserId ?: return
        val post = state.selectedPost?.takeIf { it.id == postId } ?: state.posts.find { it.id == postId } ?: return
        if (!post.falseAlarm) return
        val name = state.currentUserName ?: "An admin"
        replacePost(post.copy(falseAlarm = false, falseAlarmByName = ""))
        viewModelScope.launch {
            postRepository.clearFalseAlarm(post, userId, name).onFailure { e ->
                replacePost(post)
                _uiState.update { it.copy(error = e.friendlyMessage("Couldn't undo the false alarm")) }
            }
        }
    }

    fun consumePostDeleted() {
        _uiState.update { it.copy(postDeleted = false) }
    }

    fun consumeEmergencyNotPushed() {
        _uiState.update { it.copy(emergencyNotPushed = null) }
    }

    fun consumePostCreated() {
        _uiState.update { it.copy(postCreated = false) }
    }

    /** Keeps the open post live, so edits, likes and deletion show without leaving it. */
    fun observePost(postId: String) {
        viewModelScope.launch {
            postRepository.observePost(postId)
                .catch { e -> _uiState.update { it.copy(error = e.friendlyMessage()) } }
                .collect { post ->
                    _uiState.update { it.copy(selectedPost = post ?: it.selectedPost, postMissing = post == null) }
                }
        }
    }

    fun loadPostDetail(postId: String) {
        observePost(postId)
        viewModelScope.launch {
            postRepository.getComments(postId)
                .catch { e -> _uiState.update { it.copy(error = e.friendlyMessage()) } }
                .collect { comments ->
                    allComments = comments
                    _uiState.update { it.copy(comments = visibleComments()) }
                }
        }
    }

    /** Adds a comment to the post, or a reply under the comment [parentId] when given. */
    fun addComment(
        postId: String,
        content: String,
        mentions: Map<String, String> = emptyMap(),
        parentId: String? = null
    ) {
        val state = _uiState.value
        val userId = state.currentUserId ?: return
        viewModelScope.launch {
            val comment = Comment(
                postId = postId,
                authorId = userId,
                authorName = state.currentUserName ?: "Neighbor",
                content = content,
                createdAt = nowMillis(),
                mentions = mentions,
                parentId = parentId
            )
            postRepository.addComment(comment)
                .onSuccess { commentId ->
                    state.estateId?.let { estateId ->
                        notifyMentioned(mentions.keys, estateId, postId, commentId, content)
                        notifyCommented(comment, commentId, estateId)
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.friendlyMessage("Couldn't post your comment")) } }
        }
    }

    /**
     * Saves the author's changes to their comment, within the edit window. Anyone newly tagged is
     * told; people already tagged aren't notified twice.
     */
    fun editComment(postId: String, commentId: String, content: String, mentions: Map<String, String>) {
        val state = _uiState.value
        val comment = allComments.find { it.id == commentId } ?: return
        if (!isEditable(comment.createdAt, nowMillis())) {
            _uiState.update { it.copy(error = "Comments can only be edited for 30 minutes after posting") }
            return
        }
        viewModelScope.launch {
            postRepository.updateComment(postId, commentId, content, mentions)
                .onSuccess {
                    state.estateId?.let { notifyMentioned(mentions.keys - comment.mentions.keys, it, postId, commentId, content) }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.friendlyMessage("Couldn't save your changes")) } }
        }
    }

    /**
     * Tells the post's author about a new comment, and for a reply, the author of the comment it
     * answers. Anyone tagged in it already hears through their mention, so isn't told twice.
     */
    private fun notifyCommented(comment: Comment, commentId: String, estateId: String) {
        val state = _uiState.value
        val me = state.currentUserId ?: return
        val name = state.currentUserName ?: "A neighbour"
        val post = state.selectedPost?.takeIf { it.id == comment.postId } ?: state.posts.find { it.id == comment.postId }
        val parentAuthor = comment.parentId?.let { parent -> allComments.find { it.id == parent }?.authorId }
        val recipients = buildMap {
            parentAuthor?.let { put(it, "$name replied to your comment") }
            // The post's author hears about every comment on it, a reply included.
            post?.authorId?.let { if (!containsKey(it)) put(it, "$name commented on your post") }
        }.filterKeys { it != me && it !in comment.mentions }
        recipients.forEach { (recipientId, title) ->
            viewModelScope.launch {
                notificationRepository.createNotification(
                    AppNotification(
                        recipientId = recipientId,
                        type = NotificationType.COMMENT,
                        title = title,
                        message = comment.content.take(140),
                        estateId = estateId,
                        relatedUserId = me,
                        postId = comment.postId,
                        commentId = commentId
                    )
                )
            }
        }
    }

    /**
     * Removes a comment. Its author, the post's author and the estate's admins can; the comments
     * listener drops it from the screen straight away.
     */
    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            postRepository.deleteComment(postId, commentId)
                .onFailure { e -> _uiState.update { it.copy(error = e.friendlyMessage("Couldn't delete the comment")) } }
        }
    }

    /**
     * Tells each tagged member, in the app and by push. Best-effort, after the post or comment is
     * saved: one notification failing to send shouldn't undo it or block the others.
     */
    private fun notifyMentioned(
        userIds: Collection<String>,
        estateId: String,
        postId: String,
        commentId: String?,
        content: String
    ) {
        val state = _uiState.value
        val me = state.currentUserId ?: return
        val name = state.currentUserName ?: "A neighbour"
        val title = if (commentId == null) "$name tagged you in a post" else "$name tagged you in a comment"
        userIds.filter { it != me }.forEach { recipientId ->
            viewModelScope.launch {
                notificationRepository.createNotification(
                    AppNotification(
                        recipientId = recipientId,
                        type = NotificationType.MENTION,
                        title = title,
                        message = content.take(140),
                        estateId = estateId,
                        relatedUserId = me,
                        postId = postId,
                        commentId = commentId
                    )
                )
            }
        }
    }

    /** Flips the like on screen straight away, and puts it back if the write fails. */
    fun toggleLike(postId: String) {
        val state = _uiState.value
        val userId = state.currentUserId ?: return
        val post = state.posts.find { it.id == postId }
            ?: state.selectedPost?.takeIf { it.id == postId }
            ?: return
        val liked = userId in post.likedBy
        val toggled = post.copy(
            likedBy = if (liked) post.likedBy - userId else post.likedBy + userId,
            likeCount = (post.likeCount + if (liked) -1 else 1).coerceAtLeast(0)
        )
        replacePost(toggled)
        viewModelScope.launch {
            postRepository.toggleLike(postId, userId, liked).onFailure { replacePost(post) }
        }
    }

    private fun replacePost(post: Post) {
        _uiState.update { state ->
            state.copy(
                posts = state.posts.map { if (it.id == post.id) post else it },
                selectedPost = state.selectedPost?.let { if (it.id == post.id) post else it }
            )
        }
    }
}
