package com.eleazar.localhive.ui.directory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eleazar.localhive.ui.components.EmptyState
import com.eleazar.localhive.ui.components.ScreenGutter
import com.eleazar.localhive.ui.components.ScreenHeader
import com.eleazar.localhive.ui.components.UserCard

@Composable
fun DirectoryScreen(
    uiState: DirectoryUiState,
    onSearch: (query: String) -> Unit,
    onUserClick: (String) -> Unit,
    onMessageUser: (String) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val onBackground = MaterialTheme.colorScheme.onBackground
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

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { onSearch(it) },
            placeholder = { Text("Search by name or username", color = onBackground.copy(alpha = 0.45f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = onBackground.copy(alpha = 0.6f)) },
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
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = onBackground.copy(alpha = 0.12f),
                cursorColor = accent,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

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
                LazyColumn(
                    contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Admins first, so residents can see who runs the estate.
                    val ordered = uiState.filteredMembers.sortedBy { it.id !in uiState.adminIds }
                    items(ordered, key = { it.id }) { user ->
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
