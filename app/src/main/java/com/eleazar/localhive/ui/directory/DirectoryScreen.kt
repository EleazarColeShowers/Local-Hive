package com.eleazar.localhive.ui.directory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eleazar.localhive.R
import com.eleazar.localhive.ui.components.UserCard

@Composable
fun DirectoryScreen(
    onUserClick: (String) -> Unit = {},
    onMessageUser: (String) -> Unit = {},
    viewModel: DirectoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val hiveGreen = colorResource(id = R.color.hivegreen)
    val backgroundGold = colorResource(id = R.color.backgroundgold)
    val darkGray = colorResource(id = R.color.darkgray)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGold)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            "Neighbours",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = darkGray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.search(it) },
            placeholder = { Text("Search by name or username…", color = darkGray.copy(alpha = 0.4f)) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = hiveGreen)
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = hiveGreen,
                unfocusedBorderColor = darkGray.copy(alpha = 0.2f),
                cursorColor = hiveGreen,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = hiveGreen)
                }
            }
            uiState.filteredMembers.isEmpty() && uiState.searchQuery.isBlank() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No neighbours yet", fontWeight = FontWeight.Medium, color = darkGray.copy(alpha = 0.5f))
                        Text("Members of your estate will appear here", fontSize = 13.sp, color = darkGray.copy(alpha = 0.35f))
                    }
                }
            }
            uiState.filteredMembers.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No results for \"${uiState.searchQuery}\"",
                        color = darkGray.copy(alpha = 0.5f)
                    )
                }
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.filteredMembers, key = { it.id }) { user ->
                        UserCard(
                            user = user,
                            onClick = { onUserClick(user.id) },
                            onMessageClick = { onMessageUser(user.id) }
                        )
                    }
                }
            }
        }
    }
}
