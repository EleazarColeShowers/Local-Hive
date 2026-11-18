package com.eleazar.localhive.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.eleazar.localhive.ui.navigation.OnTabReselected
import com.eleazar.localhive.domain.model.isEditable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Emergency
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eleazar.localhive.ui.components.EmergencyBanner
import com.eleazar.localhive.domain.model.Report
import com.eleazar.localhive.domain.model.ReportReason
import com.eleazar.localhive.ui.components.EstateGlance
import com.eleazar.localhive.ui.components.PollsGlance
import com.eleazar.localhive.ui.components.icon
import com.eleazar.localhive.ui.finance.DuesSummary
import com.eleazar.localhive.domain.model.Post
import androidx.compose.runtime.saveable.rememberSaveable
import com.eleazar.localhive.domain.model.UtilityKind
import com.eleazar.localhive.domain.model.UtilityStatus
import com.eleazar.localhive.ui.components.EmptyState
import com.eleazar.localhive.ui.components.PostCard
import com.eleazar.localhive.ui.components.LocalHeaderActions
import com.eleazar.localhive.ui.components.PostSkeletons
import com.eleazar.localhive.ui.components.ScreenGutter
import com.eleazar.localhive.ui.theme.HoneyInk
import com.eleazar.localhive.ui.util.formatDate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import com.eleazar.localhive.domain.model.IncidentStatus
import com.eleazar.localhive.domain.model.PostCategory
import com.eleazar.localhive.ui.theme.AlertRed
import com.eleazar.localhive.ui.util.nowMillis

/** How long an announcement stays pinned to the top of the feed. */
private const val PIN_WINDOW_MILLIS = 7 * 24 * 60 * 60 * 1000L

/** An emergency nobody marks resolved stops being pinned after this. */
private const val EMERGENCY_WINDOW_MILLIS = 24 * 60 * 60 * 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    uiState: FeedUiState,
    onPostClick: (String) -> Unit,
    onCreatePost: () -> Unit,
    onLike: (postId: String) -> Unit,
    onLoadMore: () -> Unit,
    onUserClick: (userId: String) -> Unit = {},
    onEmergency: () -> Unit = {},
    onEditPost: (postId: String) -> Unit = {},
    onDeletePost: (postId: String, reason: String) -> Unit = { _, _ -> },
    onReport: (Report) -> Unit = {},
    utilities: Map<UtilityKind, UtilityStatus> = emptyMap(),
    polls: PollsGlance = PollsGlance(open = 0, awaitingVote = 0),
    dues: DuesSummary? = null,
    onUtilities: () -> Unit = {},
    onPolls: () -> Unit = {},
    onDues: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onRefreshErrorShown: () -> Unit = {},
) {
    // Saved by name: an enum's name survives process death on every platform.
    var filterName by rememberSaveable { mutableStateOf(FeedFilter.ALL.name) }
    val filter = FeedFilter.valueOf(filterName)
    val loadMore by rememberUpdatedState(onLoadMore)
    val listState = rememberLazyListState()
    // Tapping Feed while already on it goes back to the newest posts.
    OnTabReselected { listState.animateScrollToItem(0) }
    val accent = MaterialTheme.colorScheme.primary
    // The effect below outlives recompositions, so read posts through this rather than a stale capture.
    val posts by rememberUpdatedState(uiState.posts)

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { lastVisible ->
                if (posts.isNotEmpty() && lastVisible >= listState.layoutInfo.totalItemsCount - 3) {
                    loadMore()
                }
            }
    }

    // Neighbours posting while you're scrolled down get a pill at the top instead of moving the list.
    val scope = rememberCoroutineScope()
    var newPostsWaiting by remember { mutableStateOf(false) }
    var seenNewestId by remember { mutableStateOf(uiState.posts.firstOrNull()?.id) }
    val newest = uiState.posts.firstOrNull()
    LaunchedEffect(newest?.id) {
        if (newest == null) return@LaunchedEffect
        if (seenNewestId != null && newest.id != seenNewestId
            && newest.authorId != uiState.currentUserId && listState.firstVisibleItemIndex > 2
        ) newPostsWaiting = true
        seenNewestId = newest.id
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { if (it <= 1) newPostsWaiting = false }
    }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(uiState.refreshError) {
        val error = uiState.refreshError ?: return@LaunchedEffect
        onRefreshErrorShown()
        snackbar.showSnackbar(error)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item(key = "header") {
                    FeedHeader(
                        firstName = uiState.currentUserName?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() },
                        estateName = uiState.estateName ?: "LocalHive",
                        memberCount = uiState.memberCount,
                        showSos = uiState.estateId != null,
                        onEmergency = onEmergency
                    )
                }
                val emergencies = uiState.posts.filter {
                    it.category == PostCategory.EMERGENCY.key
                        && it.incidentStatus != IncidentStatus.RESOLVED.key
                        && nowMillis() - it.createdAt < EMERGENCY_WINDOW_MILLIS
                }
                items(emergencies, key = { "emergency-${it.id}" }) { post ->
                    EmergencyBanner(
                        post = post,
                        onClick = { onPostClick(post.id) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                if (uiState.estateId != null) item(key = "glance") {
                    EstateGlance(
                        utilities = utilities,
                        polls = polls,
                        dues = dues,
                        onUtilities = onUtilities,
                        onPolls = onPolls,
                        onDues = onDues,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
                    )
                }
                if (uiState.posts.isNotEmpty() || filter != FeedFilter.ALL) item(key = "filters") {
                    FeedFilterRow(selected = filter, onSelect = { filterName = it.name })
                }

                when {
                    uiState.isLoading && uiState.posts.isEmpty() -> item(key = "loading") {
                        PostSkeletons()
                    }
                    uiState.posts.isEmpty() -> item(key = "empty") {
                        EmptyState(
                            icon = Icons.Outlined.Forum,
                            title = "No posts yet",
                            message = "Say hi to your neighbours, or share something happening around the estate.",
                            modifier = Modifier.padding(top = 48.dp),
                            actionLabel = "Write a post",
                            onAction = onCreatePost
                        )
                    }
                    uiState.posts.none(filter::matches) -> item(key = "empty-filter") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 24.dp, end = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No ${filter.label.lowercase()} posts yet",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                if (uiState.hasMorePosts) "There may be some further back." else "Nothing in this category so far.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                if (uiState.hasMorePosts) {
                                    TextButton(onClick = onLoadMore, enabled = !uiState.isLoadingMore) { Text("Look further back") }
                                }
                                TextButton(onClick = { filterName = FeedFilter.ALL.name }) { Text("Show all posts") }
                            }
                        }
                    }
                    else -> {
                        val shown = uiState.posts.filter(filter::matches)
                        // The newest recent announcement sits above everything; the rest stay in date order.
                        val pinned = shown.firstOrNull {
                            it.category == PostCategory.ANNOUNCEMENT.key && nowMillis() - it.createdAt < PIN_WINDOW_MILLIS
                        }
                        val ordered = listOfNotNull(pinned) + shown.filter { it.id != pinned?.id }
                        items(ordered, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                currentUserId = uiState.currentUserId ?: "",
                                onLike = { onLike(post.id) },
                                onCommentClick = { onPostClick(post.id) },
                                onClick = { onPostClick(post.id) },
                                authorIsAdmin = post.authorId in uiState.adminIds,
                                authorIsTreasurer = post.authorId == uiState.treasurerId,
                                pinned = post.id == pinned?.id,
                                compact = true,
                                onMentionClick = onUserClick,
                                onEdit = { onEditPost(post.id) }.takeIf {
                                    post.authorId == uiState.currentUserId && isEditable(post.createdAt, nowMillis())
                                },
                                onDelete = { reason: String -> onDeletePost(post.id, reason) }.takeIf { post.authorId == uiState.currentUserId || uiState.isAdmin },
                                onReport = { reason: ReportReason, details: String -> onReport(Report.of(post, reason, details)) }
                                    .takeIf { post.authorId != uiState.currentUserId }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        }
                        // A filtered list can be too short to scroll, so it offers older posts itself.
                        if (filter != FeedFilter.ALL && uiState.hasMorePosts && !uiState.isLoadingMore) {
                            item(key = "older") {
                                Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                    TextButton(onClick = onLoadMore) { Text("Load older posts") }
                                }
                            }
                        }
                        if (uiState.isLoadingMore) {
                            item(key = "more") {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = accent, strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = newPostsWaiting,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
        ) {
            Surface(
                onClick = {
                    newPostsWaiting = false
                    scope.launch { listState.animateScrollToItem(0) }
                },
                shape = RoundedCornerShape(50),
                color = accent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New posts", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/** "Good evening, Ada" over the estate's name, with the SOS button beside them. */
@Composable
private fun FeedHeader(
    firstName: String?,
    estateName: String,
    memberCount: Int,
    showSos: Boolean,
    onEmergency: () -> Unit
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val honey = if (dark) MaterialTheme.colorScheme.secondary else HoneyInk
    val hour = formatDate(nowMillis(), "H").toIntOrNull() ?: 12
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ScreenGutter, end = 4.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (firstName != null) "$greeting, $firstName" else greeting,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = honey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                estateName,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (memberCount > 0) {
                Text(
                    if (memberCount == 1) "1 neighbour" else "$memberCount neighbours",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showSos) SosButton(onClick = onEmergency)
        LocalHeaderActions.current(this)
    }
}

/** Opens the SOS screen; nothing is sent until the alert there is held down. */
@Composable
private fun SosButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = AlertRed,
        contentColor = Color.White,
        modifier = Modifier.padding(end = 4.dp).height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Emergency, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("SOS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

/** Narrows the feed to one kind of post. */
private enum class FeedFilter(val label: String, val categories: Set<PostCategory>) {
    ALL("All", emptySet()),
    ANNOUNCEMENTS("Announcements", setOf(PostCategory.ANNOUNCEMENT)),
    // Emergencies are security too, and the one place to find past ones.
    SECURITY("Security", setOf(PostCategory.SECURITY, PostCategory.EMERGENCY)),
    FOR_SALE("For sale", setOf(PostCategory.FOR_SALE)),
    EVENTS("Events", setOf(PostCategory.EVENT)),
    QUESTIONS("Questions", setOf(PostCategory.QUESTION)),
    NOTICES("Notices", setOf(PostCategory.NOTICE));

    fun matches(post: Post): Boolean = this == ALL || PostCategory.fromKey(post.category) in categories
}

/** Text tabs with a green underline under the chosen one; a line under the row closes it off from the posts. */
@Composable
private fun FeedFilterRow(selected: FeedFilter, onSelect: (FeedFilter) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp)
        ) {
            FeedFilter.entries.forEach { option ->
                val isSelected = option == selected
                Column(
                    modifier = Modifier
                        .clickable(onClickLabel = "Show ${option.label.lowercase()}") { onSelect(option) }
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        option.label,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onBackground else muted,
                        modifier = Modifier.padding(top = 10.dp, bottom = 9.dp)
                    )
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .width(if (isSelected) 28.dp else 0.dp)
                            .background(accent, RoundedCornerShape(50))
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
