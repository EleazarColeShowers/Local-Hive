package com.eleazar.localhive.ui.directory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eleazar.localhive.domain.model.Chat
import com.eleazar.localhive.ui.blocking.BlockConfirmDialog
import com.eleazar.localhive.ui.components.Avatar
import com.eleazar.localhive.ui.components.ScreenGutter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onMessageUser: (chatId: String) -> Unit,
    directoryState: DirectoryUiState,
    isBlocked: Boolean = false,
    onBlock: () -> Unit = {},
    onUnblock: () -> Unit = {},
) {
    val accent = MaterialTheme.colorScheme.primary
    val loadedUser = directoryState.selectedUser?.takeIf { it.id == userId }
    val loadedName = loadedUser?.let { it.displayName?.takeIf { n -> n.isNotBlank() } ?: it.username } ?: "this person"
    val isSelf = userId == directoryState.currentUserId
    var menuOpen by remember { mutableStateOf(false) }
    var confirmBlock by remember { mutableStateOf<Boolean?>(null) }

    confirmBlock?.let { blocking ->
        BlockConfirmDialog(
            name = loadedName,
            blocking = blocking,
            onConfirm = if (blocking) onBlock else onUnblock,
            onDismiss = { confirmBlock = null }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (loadedUser != null && !isSelf) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isBlocked) "Unblock" else "Block",
                                            color = if (isBlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        confirmBlock = !isBlocked
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        val user = directoryState.selectedUser?.takeIf { it.id == userId }
        if (user == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
            return@Scaffold
        }

        val name = user.displayName?.takeIf { it.isNotBlank() } ?: user.username ?: "Neighbour"
        val currentUserId = directoryState.currentUserId

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenGutter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Avatar(imageUrl = user.profileImageUrl, name = name, size = 104.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                name,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            user.username?.takeIf { it.isNotBlank() && it != name }?.let {
                Text("@$it", fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
            }

            if (currentUserId != null && userId != currentUserId && isBlocked) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "You've blocked $name. You won't see their posts, and you can't message each other.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { confirmBlock = false },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Unblock", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            } else if (currentUserId != null && userId != currentUserId) {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { onMessageUser(Chat.idFor(currentUserId, userId)) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Message", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            val details = listOfNotNull(
                user.bio?.takeIf { it.isNotBlank() }?.let { Triple(Icons.Outlined.Info, "About", it) },
                user.occupation?.takeIf { it.isNotBlank() }?.let { Triple(Icons.Outlined.Work, "Occupation", it) },
                user.address?.takeIf { it.isNotBlank() }?.let { Triple(Icons.Outlined.Home, "Address", it) }
            )
            if (details.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        details.forEachIndexed { index, (icon, label, value) ->
                            DetailRow(icon = icon, label = label, value = value)
                            if (index < details.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 56.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp).size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            Text(value, fontSize = 15.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
