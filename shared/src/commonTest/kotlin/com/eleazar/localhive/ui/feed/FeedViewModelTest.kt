package com.eleazar.localhive.ui.feed

import com.eleazar.localhive.domain.model.BlockedUser
import com.eleazar.localhive.domain.model.Estate
import com.eleazar.localhive.domain.model.NotificationType
import com.eleazar.localhive.domain.model.Post
import com.eleazar.localhive.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {
    private val estate = Estate(id = "e1", name = "Palm Court")
    private val me = User(id = "ada", username = "ada", estateId = "e1")
    private val neighbour = User(id = "bo", username = "bo", estateId = "e1")
    private val bosPost = Post(id = "p1", authorId = "bo", estateId = "e1", content = "Gate is open", createdAt = 1_000)

    private lateinit var posts: FakePostRepository
    private lateinit var users: FakeUserRepository
    private lateinit var storage: FakeStorageRepository
    private lateinit var notifications: FakeNotificationRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        posts = FakePostRepository().apply { this.posts.value = listOf(bosPost) }
        users = FakeUserRepository(mapOf(me.id to me, neighbour.id to neighbour))
        storage = FakeStorageRepository()
        notifications = FakeNotificationRepository()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun feed() = FeedViewModel(
        postRepository = posts,
        authRepository = FakeAuthRepository(me.id),
        userRepository = users,
        storageRepository = storage,
        estateRepository = FakeEstateRepository(estate),
        notificationRepository = notifications,
        sosRepository = FakeSosRepository()
    )

    @Test
    fun loadsTheEstateFeedForTheSignedInUser() = runTest {
        val state = feed().uiState.value
        assertEquals("e1", state.estateId)
        assertEquals("Palm Court", state.estateName)
        assertEquals(listOf("p1"), state.posts.map { it.id })
        assertEquals(listOf("bo"), state.members.map { it.id }, "you can't tag yourself")
    }

    @Test
    fun hidesPostsFromBlockedNeighbours() = runTest {
        val vm = feed()
        users.blocked.value = listOf(BlockedUser(id = "bo"))
        assertTrue(vm.uiState.value.posts.isEmpty())
        assertTrue(vm.uiState.value.members.isEmpty())
    }

    @Test
    fun aPhotoIsUploadedBeforeTheCommentIsSaved() = runTest {
        val vm = feed()
        vm.addComment("p1", content = "", imageUri = "content://photo/1")
        advanceUntilIdle()

        assertEquals(listOf("content://photo/1"), storage.uploaded)
        val saved = posts.savedComments.single()
        assertEquals("https://res.cloudinary.com/demo/image/upload/1.jpg", saved.imageUrl)
        val state = vm.uiState.value
        assertFalse(state.sendingComment)
        assertEquals(1, state.commentsPosted, "the composer clears its draft once this goes up")
    }

    @Test
    fun aFailedPhotoUploadSavesNoComment() = runTest {
        storage.uploadFails = true
        val vm = feed()
        vm.addComment("p1", content = "Look at this", imageUri = "content://photo/1")
        advanceUntilIdle()

        assertTrue(posts.savedComments.isEmpty())
        val state = vm.uiState.value
        assertFalse(state.sendingComment)
        assertEquals(0, state.commentsPosted, "the composer keeps the draft so nothing is lost")
        assertNotNull(state.error)
    }

    @Test
    fun aFailedSaveKeepsTheDraft() = runTest {
        posts.addCommentFails = true
        val vm = feed()
        vm.addComment("p1", content = "Hello")
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.commentsPosted)
        assertFalse(vm.uiState.value.sendingComment)
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun thePostsAuthorHearsAboutAPhotoComment() = runTest {
        val vm = feed()
        vm.addComment("p1", content = "", imageUri = "content://photo/1")
        advanceUntilIdle()

        val sent = notifications.sent.single()
        assertEquals("bo", sent.recipientId)
        assertEquals(NotificationType.COMMENT, sent.type)
        assertEquals("📷 Photo", sent.message)
    }

    @Test
    fun taggedNeighboursHearOnceNotTwice() = runTest {
        val vm = feed()
        vm.addComment("p1", content = "@bo come see", mentions = mapOf("bo" to "bo"))
        advanceUntilIdle()

        assertEquals(listOf(NotificationType.MENTION), notifications.sent.map { it.type })
    }

    @Test
    fun aLikeShowsStraightAwayAndUndoesIfTheWriteFails() = runTest {
        posts.likeFails = true
        val vm = feed()
        vm.toggleLike("p1")
        advanceUntilIdle()

        val post = vm.uiState.value.posts.single()
        assertEquals(0, post.likeCount)
        assertTrue("ada" !in post.likedBy)
    }

    @Test
    fun aLikeSticksWhenTheWriteWorks() = runTest {
        val vm = feed()
        vm.toggleLike("p1")
        advanceUntilIdle()

        val post = vm.uiState.value.posts.single()
        assertEquals(1, post.likeCount)
        assertTrue("ada" in post.likedBy)
    }

    @Test
    fun aFailedRefreshSaysSoAndStopsSpinning() = runTest {
        posts.latestPosts = Result.failure(Exception("Offline"))
        val vm = feed()
        vm.refresh()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isRefreshing)
        assertNotNull(vm.uiState.value.refreshError)
        vm.consumeRefreshError()
        assertNull(vm.uiState.value.refreshError)
    }

    @Test
    fun mergingKeepsTheLiveCopyAndDropsBlockedAuthors() {
        val live = listOf(Post(id = "a", content = "edited", createdAt = 3), Post(id = "b", authorId = "x", createdAt = 2))
        val older = listOf(Post(id = "a", content = "old", createdAt = 3), Post(id = "c", createdAt = 1))

        val merged = mergeFeed(live, older, blockedIds = setOf("x"))

        assertEquals(listOf("a", "c"), merged.map { it.id })
        assertEquals("edited", merged.first().content)
    }
}
