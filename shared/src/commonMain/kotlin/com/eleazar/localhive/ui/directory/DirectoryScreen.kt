package com.eleazar.localhive.ui.directory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eleazar.localhive.ui.components.EmptyState
import com.eleazar.localhive.ui.components.ScreenGutter
import com.eleazar.localhive.ui.components.ScreenHeader
import com.eleazar.localhive.ui.components.UserCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryScreen(
    uiState: DirectoryUiState,
    onSearch: (query: String) -> Unit,
    onRefresh: () -> Unit = {},
    onUserClick: (String) -> Unit,
    onMessageUser: (String) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val neighbourCount = uiState.members.count { it.id != uiState.currentUserId }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Neighbours",
            subtitle = when (neighbourCount) {
                0 -> null
                1 -> "1 person in your estate"
                else -> "$neighbourCount people in your estate"
            }
        )

        // A filled pill, like the search bars in most apps, rather than a form field.
        TextField(
            value = uiState.searchQuery,
            onValueChange = { onSearch(it) },
            placeholder = { Text("Search", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearch("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenGutter)
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(50),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = accent
            )
        )

        // The list opens from the phone's saved copy; pulling down fetches it fresh.
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent)
                    }
                }
                uiState.filteredMembers.isEmpty() && uiState.searchQuery.isBlank() -> EmptyState(
                    icon = Icons.Outlined.Groups,
                    title = "No neighbours yet",
                    message = "Members of your estate will appear here once they join."
                )
                uiState.filteredMembers.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "No matches",
                    message = "Nobody matches \"${uiState.searchQuery}\". Try a different name."
                )
                else -> {
                    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                        // Admins first, so residents can see who runs the estate.
                        val ordered = uiState.filteredMembers.sortedBy { it.id !in uiState.adminIds }
                        itemsIndexed(ordered, key = { _, user -> user.id }) { index, user ->
                            if (index > 0) {
                                // Inset to the text, as list dividers usually are.
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(start = 76.dp)
                                )
                            }
                            UserCard(
                                user = user,
                                onClick = { onUserClick(user.id) },
                                onMessageClick = { onMessageUser(user.id) },
                                isAdmin = user.id in uiState.adminIds
                            )
                        }
                    }
                }
            }
        }
    }
}
