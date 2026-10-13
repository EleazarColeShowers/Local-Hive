package com.eleazar.localhive.ui.feed

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.PostRepository
import com.eleazar.localhive.domain.StorageRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.Comment
import com.eleazar.localhive.domain.model.Post
import com.eleazar.localhive.domain.model.PostCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val POST_PAGE_SIZE = 20L

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val storageRepository: StorageRepository,
    private val estateRepository: EstateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState = _uiState.asStateFlow()

    private var livePosts: List<Post> = emptyList()
    private var olderPosts: List<Post> = emptyList()
    private var allComments: List<Comment> = emptyList()
    /** Authors whose posts and comments the user has chosen not to see. */
    private var blockedIds: Set<String> = emptySet()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        val userId = authRepository.getCurrentUserId() ?: return
        _uiState.update { it.copy(currentUserId = userId) }
        viewModelScope.launch {
            userRepository.observeBlockedUsers(userId).collect { blocked ->
                blockedIds = blocked.mapTo(HashSet()) { it.id }
                _uiState.update { it.copy(posts = mergePosts(), comments = visibleComments()) }
            }
        }
        viewModelScope.launch {
            userRepository.getUser(userId, cacheFirst = true).onSuccess { user ->
                _uiState.update {
                    it.copy(
                        currentUserName = user.displayName ?: user.username,
                        currentUserImageUrl = user.profileImageUrl,
                        estateId = user.estateId
                    )
                }
                user.estateId?.let {
                    loadPosts(it)
                    loadEstate(it)
                }
            }
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

    private fun loadPosts(estateId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            postRepository.getPostsForEstate(estateId, POST_PAGE_SIZE)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
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
                    _uiState.update { it.copy(isLoadingMore = false, error = e.message) }
                }
        }
    }

    private fun mergePosts(): List<Post> {
        val merged = LinkedHashMap<String, Post>()
        livePosts.forEach { merged[it.id] = it }
        olderPosts.forEach { merged.putIfAbsent(it.id, it) }
        return merged.values.filter { it.authorId !in blockedIds }.sortedByDescending { it.createdAt }
    }

    private fun visibleComments(): List<Comment> = allComments.filter { it.authorId !in blockedIds }

    fun createPost(content: String, imageUri: Uri? = null, category: PostCategory? = null) {
        val state = _uiState.value
        val estateId = state.estateId ?: return
        val userId = state.currentUserId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true, error = null) }

            // Upload first: a post that referenced a failed upload would render a broken image.
            val imageUrl = imageUri?.let { uri ->
                storageRepository.uploadPostImage(userId, uri).fold(
                    onSuccess = { it },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(isPosting = false, error = e.message ?: "Couldn't upload the photo")
                        }
                        return@launch
                    }
                )
            }

            val post = Post(
                authorId = userId,
                authorName = state.currentUserName ?: "Neighbor",
                authorImageUrl = state.currentUserImageUrl,
                estateId = estateId,
                content = content,
                imageUrl = imageUrl,
                createdAt = System.currentTimeMillis(),
                category = category?.key.orEmpty()
            )
            postRepository.createPost(post)
                .onSuccess {
                    _uiState.update { it.copy(isPosting = false, postCreated = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isPosting = false, error = e.message ?: "Failed to create post") }
                }
        }
    }

    fun consumePostCreated() {
        _uiState.update { it.copy(postCreated = false) }
    }

    fun loadPostDetail(postId: String) {
        viewModelScope.launch {
            postRepository.getPost(postId).onSuccess { post ->
                _uiState.update { it.copy(selectedPost = post) }
            }
            postRepository.getComments(postId)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { comments ->
                    allComments = comments
                    _uiState.update { it.copy(comments = visibleComments()) }
                }
        }
    }

    fun addComment(postId: String, content: String) {
        val state = _uiState.value
        val userId = state.currentUserId ?: return
        viewModelScope.launch {
            val comment = Comment(
                postId = postId,
                authorId = userId,
                authorName = state.currentUserName ?: "Neighbor",
                content = content,
                createdAt = System.currentTimeMillis()
            )
            postRepository.addComment(comment)
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
