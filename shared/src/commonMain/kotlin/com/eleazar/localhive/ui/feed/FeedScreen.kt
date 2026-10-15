package com.eleazar.localhive.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eleazar.localhive.ui.components.ComposePrompt
import com.eleazar.localhive.ui.components.EmergencyBanner
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
import com.eleazar.localhive.ui.components.ScreenHeader
import com.eleazar.localhive.domain.model.IncidentStatus
import com.eleazar.localhive.domain.model.PostCategory
import com.eleazar.localhive.ui.theme.AlertRed
import com.eleazar.localhive.ui.util.nowMillis

/** How long an announcement stays pinned to the top of the feed. */
private const val PIN_WINDOW_MILLIS = 7 * 24 * 60 * 60 * 1000L

/** An emergency nobody marks resolved stops being pinned after this. */
private const val EMERGENCY_WINDOW_MILLIS = 24 * 60 * 60 * 1000L

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
    utilities: Map<UtilityKind, UtilityStatus> = emptyMap(),
    polls: PollsGlance = PollsGlance(open = 0, awaitingVote = 0),
    dues: DuesSummary? = null,
    onUtilities: () -> Unit = {},
    onPolls: () -> Unit = {},
    onDues: () -> Unit = {},
) {
    // Saved by name: an enum's name survives process death on every platform.
    var filterName by rememberSaveable { mutableStateOf(FeedFilter.ALL.name) }
    val filter = FeedFilter.valueOf(filterName)
    val loadMore by rememberUpdatedState(onLoadMore)
    val listState = rememberLazyListState()
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

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item(key = "header") {
            ScreenHeader(title = uiState.estateName ?: "LocalHive", action = {
                if (uiState.estateId != null) SosButton(onClick = onEmergency)
            })
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
        item(key = "compose") {
            ComposePrompt(
                name = uiState.currentUserName,
                photoUrl = uiState.currentUserImageUrl,
                onClick = onCreatePost
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        if (uiState.posts.isNotEmpty() || filter != FeedFilter.ALL) item(key = "filters") {
            FeedFilterRow(selected = filter, onSelect = { filterName = it.name })
        }

        when {
            uiState.isLoading && uiState.posts.isEmpty() -> item(key = "loading") {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent)
                }
            }
            uiState.posts.isEmpty() -> item(key = "empty") {
                EmptyState(
                    icon = Icons.Outlined.Forum,
                    title = "No posts yet",
                    message = "Be the first to share something with your neighbours.",
                    modifier = Modifier.padding(top = 48.dp)
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
                    if (uiState.hasMorePosts) {
                        TextButton(onClick = onLoadMore, enabled = !uiState.isLoadingMore) { Text("Look further back") }
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
                        pinned = post.id == pinned?.id,
                        onMentionClick = onUserClick,
                        onEdit = { onEditPost(post.id) }.takeIf { post.authorId == uiState.currentUserId },
                        onDelete = { reason: String -> onDeletePost(post.id, reason) }.takeIf { post.authorId == uiState.currentUserId || uiState.isAdmin }
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

@Composable
private fun FeedFilterRow(selected: FeedFilter, onSelect: (FeedFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FeedFilter.entries.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option.label) },
                shape = RoundedCornerShape(50),
                // Unselected chips are text only, so the row reads as a quiet control, not a second toolbar.
                border = if (option == selected) null else FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}
