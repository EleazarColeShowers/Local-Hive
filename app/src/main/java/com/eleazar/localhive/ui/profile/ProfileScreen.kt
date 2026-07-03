package com.eleazar.localhive.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.eleazar.localhive.R

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    onLeaveEstate: () -> Unit = {},
    onAdminPanel: (estateId: String) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showEdit by remember { mutableStateOf(false) }
    var showCodeDialog by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editUsername by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isLoggedOut) { if (uiState.isLoggedOut) onLogout() }
    LaunchedEffect(uiState.hasLeftEstate) { if (uiState.hasLeftEstate) onLeaveEstate() }
    LaunchedEffect(uiState.user) {
        uiState.user?.let {
            editName = it.displayName ?: ""
            editUsername = it.username ?: ""
            editBio = it.bio ?: ""
        }
    }

    val hiveGreen = colorResource(id = R.color.hivegreen)
    val honeyGold = colorResource(id = R.color.honeygold)
    val backgroundGold = colorResource(id = R.color.backgroundgold)
    val lightGold = colorResource(id = R.color.lightgold)
    val darkGray = colorResource(id = R.color.darkgray)

    val user = uiState.user
    val estate = uiState.estate
    val isAdmin = estate != null && user != null && user.id in estate.adminIds
    val letter = (user?.displayName ?: user?.username ?: "?").firstOrNull()?.uppercase() ?: "?"

    if (showCodeDialog && estate != null) {
        InviteCodeDialog(
            inviteCode = estate.inviteCode,
            estateName = estate.name,
            onDismiss = { showCodeDialog = false },
            onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Invite Code", estate.inviteCode))
                Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
            },
            onShare = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Join ${estate.name} on LocalHive! Use code: ${estate.inviteCode}")
                }
                context.startActivity(Intent.createChooser(intent, "Share Invite Code"))
            },
            hiveGreen = hiveGreen,
            lightGold = lightGold,
            darkGray = darkGray
        )
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Leave Estate?", fontWeight = FontWeight.Bold) },
            text = { Text("You'll need a new invite code to re-join ${estate?.name ?: "this estate"}.") },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveConfirm = false
                    viewModel.leaveEstate()
                }) {
                    Text("Leave", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGold)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(hiveGreen)
        ) {
            IconButton(
                onClick = { showEdit = !showEdit },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Icon(
                    if (showEdit) Icons.Default.Person else Icons.Default.Edit,
                    contentDescription = if (showEdit) "View profile" else "Edit profile",
                    tint = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-52).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .border(3.dp, honeyGold, CircleShape)
                    .padding(4.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    color = Color.White,
                    shadowElevation = 6.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(letter, fontWeight = FontWeight.Bold, fontSize = 38.sp, color = hiveGreen)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            if (showEdit) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = lightGold),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Edit Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = darkGray,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = fieldColors(hiveGreen)
                        )
                        OutlinedTextField(
                            value = editUsername,
                            onValueChange = { editUsername = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = fieldColors(hiveGreen)
                        )
                        OutlinedTextField(
                            value = editBio,
                            onValueChange = { editBio = it },
                            label = { Text("Bio") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3,
                            colors = fieldColors(hiveGreen)
                        )
                        if (uiState.isLoading) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = hiveGreen, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                            }
                        } else {
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
                    }
                }
            } else {
                Text(
                    user?.displayName ?: user?.username ?: "Neighbor",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = darkGray
                )
                user?.username?.let {
                    Text("@$it", fontSize = 15.sp, color = darkGray.copy(alpha = 0.5f), modifier = Modifier.padding(top = 2.dp))
                }
                user?.bio?.let { bio ->
                    if (bio.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            bio,
                            fontSize = 14.sp,
                            color = darkGray.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Estate card
                if (estate != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = lightGold),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(estate.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = darkGray)
                                    if (estate.address.isNotBlank()) {
                                        Text(
                                            estate.address,
                                            fontSize = 13.sp,
                                            color = darkGray.copy(alpha = 0.55f),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    Text(
                                        "${estate.memberCount} member${if (estate.memberCount != 1) "s" else ""}",
                                        fontSize = 12.sp,
                                        color = hiveGreen,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                if (isAdmin) {
                                    Surface(shape = RoundedCornerShape(20.dp), color = hiveGreen) {
                                        Text(
                                            "Admin",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = darkGray.copy(alpha = 0.08f))
                            Spacer(Modifier.height(12.dp))

                            if (isAdmin) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showCodeDialog = true }
                                        .background(hiveGreen.copy(alpha = 0.08f))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = hiveGreen, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Share Invite Code", fontWeight = FontWeight.SemiBold, color = hiveGreen, fontSize = 14.sp)
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { estate?.id?.let { onAdminPanel(it) } }
                                        .background(hiveGreen.copy(alpha = 0.08f))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = hiveGreen, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Admin Panel", fontWeight = FontWeight.SemiBold, color = hiveGreen, fontSize = 14.sp)
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showLeaveConfirm = true }
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.07f))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Leave Estate", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                }

                // Contact info card
                val hasInfoRows = user?.email != null || user?.address != null ||
                        user?.phone != null || user?.occupation != null
                if (hasInfoRows) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = lightGold),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            user?.email?.let { ProfileInfoRow(Icons.Default.Email, it, hiveGreen) }
                            user?.address?.let { ProfileInfoRow(Icons.Default.Home, it, hiveGreen) }
                            user?.phone?.let { ProfileInfoRow(Icons.Default.Phone, it, hiveGreen) }
                            user?.occupation?.let { ProfileInfoRow(Icons.Default.Star, it, hiveGreen) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Log out card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = lightGold),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.logout() }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Log Out", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error, fontSize = 15.sp)
                    }
                }
            }

            uiState.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InviteCodeDialog(
    inviteCode: String,
    estateName: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    hiveGreen: Color,
    lightGold: Color,
    darkGray: Color
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = lightGold),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Invite to $estateName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = darkGray,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Share this code with neighbours to let them join",
                    fontSize = 13.sp,
                    color = darkGray.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = hiveGreen.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "INVITE CODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = hiveGreen.copy(alpha = 0.7f),
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            inviteCode,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = hiveGreen,
                            letterSpacing = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { onCopy(); onDismiss() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = hiveGreen)
                    ) {
                        Text("Copy", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onShare(); onDismiss() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = hiveGreen)
                    ) {
                        Text("Share", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(icon: ImageVector, label: String, tint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = colorResource(id = R.color.darkgray).copy(alpha = 0.8f))
    }
}

@Composable
private fun fieldColors(hiveGreen: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = hiveGreen,
    focusedLabelColor = hiveGreen,
    unfocusedBorderColor = colorResource(id = R.color.darkgray).copy(alpha = 0.3f),
    cursorColor = hiveGreen,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)
