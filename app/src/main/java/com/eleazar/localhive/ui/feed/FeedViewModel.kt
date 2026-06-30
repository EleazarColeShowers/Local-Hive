package com.eleazar.localhive.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.PostRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.model.Comment
import com.eleazar.localhive.domain.model.Post
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUserId: String? = null,
    val currentUserName: String? = null,
    val estateId: String? = null,
    val selectedPost: Post? = null,
    val comments: List<Comment> = emptyList()
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        val userId = authRepository.getCurrentUserId() ?: return
        _uiState.update { it.copy(currentUserId = userId) }
        viewModelScope.launch {
            userRepository.getUser(userId).onSuccess { user ->
                _uiState.update { it.copy(currentUserName = user.displayName ?: user.username, estateId = user.estateId) }
                user.estateId?.let { loadPosts(it) }
            }
        }
    }

    private fun loadPosts(estateId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            postRepository.getPostsForEstate(estateId)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { posts -> _uiState.update { it.copy(posts = posts, isLoading = false) } }
        }
    }

    fun createPost(content: String) {
        val state = _uiState.value
        val estateId = state.estateId ?: return
        val userId = state.currentUserId ?: return
        viewModelScope.launch {
            val post = Post(
                authorId = userId,
                authorName = state.currentUserName ?: "Neighbor",
                estateId = estateId,
                content = content,
                createdAt = System.currentTimeMillis()
            )
            postRepository.createPost(post)
        }
    }

    fun loadPostDetail(postId: String) {
        viewModelScope.launch {
            postRepository.getPost(postId).onSuccess { post ->
                _uiState.update { it.copy(selectedPost = post) }
            }
            postRepository.getComments(postId)
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { comments -> _uiState.update { it.copy(comments = comments) } }
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

    fun toggleLike(postId: String) {
        val userId = _uiState.value.currentUserId ?: return
        val post = _uiState.value.posts.find { it.id == postId } ?: return
        val liked = userId in post.likedBy
        viewModelScope.launch {
            postRepository.toggleLike(postId, userId, liked)
        }
    }
}
