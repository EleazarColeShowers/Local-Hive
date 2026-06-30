package com.eleazar.localhive.ui.directory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eleazar.localhive.R
import com.eleazar.localhive.ui.messages.MessagesViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onMessageUser: (chatId: String) -> Unit,
    directoryViewModel: DirectoryViewModel = hiltViewModel(),
    messagesViewModel: MessagesViewModel = hiltViewModel()
) {
    val directoryState by directoryViewModel.uiState.collectAsState()
    val messagesState by messagesViewModel.uiState.collectAsState()

    LaunchedEffect(userId) {
        directoryViewModel.loadUserProfile(userId)
    }

    LaunchedEffect(messagesState.newChatId) {
        messagesState.newChatId?.let { chatId ->
            messagesViewModel.clearNewChat()
            onMessageUser(chatId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val user = directoryState.selectedUser
        if (user == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colorResource(id = R.color.hivegreen))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(88.dp).clip(CircleShape),
                    color = colorResource(id = R.color.hivegreen).copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (user.displayName ?: user.username ?: "?").firstOrNull()?.uppercase() ?: "?",
                            fontWeight = FontWeight.Bold, fontSize = 32.sp,
                            color = colorResource(id = R.color.hivegreen)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(user.displayName ?: user.username ?: "Neighbor", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                user.username?.let { Text("@$it", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
                user.bio?.let { bio ->
                    if (bio.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(bio, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }

                Spacer(Modifier.height(28.dp))

                if (userId != directoryState.currentUserId) {
                    Button(
                        onClick = {
                            val otherName = user.displayName ?: user.username ?: "Neighbor"
                            messagesViewModel.startChat(userId, otherName)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.hivegreen))
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Send Message", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
