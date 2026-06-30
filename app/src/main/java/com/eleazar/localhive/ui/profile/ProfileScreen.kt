package com.eleazar.localhive.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eleazar.localhive.R

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEdit by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editUsername by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) onLogout()
    }

    LaunchedEffect(uiState.user) {
        uiState.user?.let { user ->
            editName = user.displayName ?: ""
            editUsername = user.username ?: ""
            editBio = user.bio ?: ""
        }
    }

    val hiveGreen = colorResource(id = R.color.hivegreen)
    val letter = (uiState.user?.displayName ?: uiState.user?.username ?: "?")
        .firstOrNull()?.uppercase() ?: "?"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Green header banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(hiveGreen)
        ) {
            IconButton(
                onClick = { showEdit = !showEdit },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit profile",
                    tint = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        // Avatar overlapping the banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-44).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape),
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = letter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        color = hiveGreen
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (showEdit) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = fieldColors()
                    )
                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = { editUsername = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = fieldColors()
                    )
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Bio") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3,
                        colors = fieldColors()
                    )
                    Button(
                        onClick = {
                            viewModel.updateProfile(editName, editUsername, editBio)
                            showEdit = false
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = editName.isNotBlank() && editUsername.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = hiveGreen)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            } else {
                Text(
                    text = uiState.user?.displayName ?: uiState.user?.username ?: "Neighbor",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                uiState.user?.username?.let {
                    Text(
                        text = "@$it",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                uiState.user?.email?.let {
                    Text(
                        text = it,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                uiState.user?.bio?.let { bio ->
                    if (bio.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = bio,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            Spacer(Modifier.height(24.dp))

            OutlinedButton(
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
            ) {
                Text("Log Out", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = colorResource(id = R.color.hivegreen),
    focusedLabelColor = colorResource(id = R.color.hivegreen),
    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
    cursorColor = colorResource(id = R.color.hivegreen)
)
