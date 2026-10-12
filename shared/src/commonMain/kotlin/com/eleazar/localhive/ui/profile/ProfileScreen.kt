package com.eleazar.localhive.ui.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage

/** Platform-free colors so the UI doesn't depend on Android R.color. */
data class ProfileColors(
    val hiveGreen: Color,
    val honeyGold: Color,
    val backgroundGold: Color,
    val lightGold: Color,
    val darkGray: Color,
    val cardBackground: Color = Color.White
)

@Composable
fun rememberProfileColors(): ProfileColors {
    val colorScheme = MaterialTheme.colorScheme
    return ProfileColors(
        hiveGreen = colorScheme.primary,
        honeyGold = colorScheme.secondary,
        backgroundGold = colorScheme.background,
        lightGold = colorScheme.surfaceVariant,
        darkGray = colorScheme.onBackground,
        cardBackground = colorScheme.surface
    )
}

data class ProfileUserUi(
    val displayName: String?,
    val username: String?,
    val bio: String?,
    val profileImageUrl: String?,
    val email: String?,
    val address: String?,
    val phone: String?,
    val occupation: String?
)

data class ProfileEstateUi(
    val id: String,
    val name: String,
    val address: String,
    val inviteCode: String,
    val memberCount: Int,
    val isAdmin: Boolean
)

private val HeroHeight = 168.dp
private val AvatarSize = 112.dp

/**
 * Bare profile UI. No ViewModel, Context, Toast, Intents or pickers in here.
 *
 * @param pickedImage  platform image model (e.g. Android Uri) chosen by the route, shown as avatar preview
 * @param onPickPhoto  called when the avatar is tapped in edit mode; route launches its photo picker
 * @param onEditModeChange called whenever edit mode toggles, so the route can reset picked-image state
 */
@Composable
fun ProfileScreen(
    user: ProfileUserUi?,
    estate: ProfileEstateUi?,
    isLoading: Boolean,
    error: String?,
    pickedImage: Any?,
    colors: ProfileColors,
    onPickPhoto: () -> Unit,
    onEditModeChange: (Boolean) -> Unit,
    onSave: (name: String, username: String, bio: String) -> Unit,
    onCopyInviteCode: (code: String) -> Unit,
    onShareInviteCode: (code: String, estateName: String) -> Unit,
    onSettings: () -> Unit,
    onAdminPanel: (estateId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEdit by remember { mutableStateOf(false) }
    var showCodeDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editUsername by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }

    fun resetFields() {
        editName = user?.displayName ?: ""
        editUsername = user?.username ?: ""
        editBio = user?.bio ?: ""
    }

    LaunchedEffect(user) { resetFields() }
    LaunchedEffect(showEdit) { onEditModeChange(showEdit) }

    val isAdmin = estate?.isAdmin == true
    val letter = (user?.displayName ?: user?.username ?: "?").firstOrNull()?.uppercase() ?: "?"
    val avatarModel: Any? = pickedImage ?: user?.profileImageUrl?.takeIf { it.isNotBlank() }

    if (showCodeDialog && estate != null) {
        InviteCodeDialog(
            inviteCode = estate.inviteCode,
            estateName = estate.name,
            colors = colors,
            onDismiss = { showCodeDialog = false },
            onCopy = { onCopyInviteCode(estate.inviteCode) },
            onShare = { onShareInviteCode(estate.inviteCode, estate.name) }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundGold)
            .verticalScroll(rememberScrollState())
    ) {
        ProfileHeader(
            letter = letter,
            avatarModel = avatarModel,
            editing = showEdit,
            colors = colors,
            onToggleEdit = {
                if (showEdit) resetFields() // cancelling discards unsaved edits
                showEdit = !showEdit
            },
            onSettings = onSettings,
            onPickPhoto = onPickPhoto
        )

        Spacer(Modifier.height(16.dp))

        AnimatedContent(
            targetState = showEdit,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
            label = "profile_mode"
        ) { editing ->
            if (editing) {
                EditContent(
                    name = editName,
                    username = editUsername,
                    bio = editBio,
                    isLoading = isLoading,
                    colors = colors,
                    onNameChange = { editName = it },
                    onUsernameChange = { editUsername = it },
                    onBioChange = { editBio = it },
                    onCancel = {
                        resetFields()
                        showEdit = false
                    },
                    onSave = {
                        onSave(editName, editUsername, editBio)
                        showEdit = false
                    }
                )
            } else {
                ViewContent(
                    user = user,
                    estate = estate,
                    isAdmin = isAdmin,
                    colors = colors,
                    onShowInviteCode = { showCodeDialog = true },
                    onAdminPanel = onAdminPanel
                )
            }
        }

        error?.let {
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            ) {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ───────────────────────── Header ─────────────────────────

@Composable
private fun ProfileHeader(
    letter: String,
    avatarModel: Any?,
    editing: Boolean,
    colors: ProfileColors,
    onToggleEdit: () -> Unit,
    onSettings: () -> Unit,
    onPickPhoto: () -> Unit
) {
    val heroBrush = Brush.verticalGradient(
        listOf(colors.hiveGreen, lerp(colors.hiveGreen, Color.Black, 0.3f))
    )
    val glass = IconButtonDefaults.iconButtonColors(
        containerColor = Color.White.copy(alpha = 0.18f),
        contentColor = Color.White
    )

    // Avatar sits at the bottom of this box and overlaps the hero by half its height.
    Box(modifier = Modifier.fillMaxWidth().height(HeroHeight + AvatarSize / 2)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HeroHeight)
                .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .background(heroBrush)
        ) {
            // Soft decorative circles
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-40).dp, y = (-30).dp)
                    .size(140.dp)
                    .background(Color.White.copy(alpha = 0.07f), CircleShape)
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 30.dp, y = 40.dp)
                    .size(120.dp)
                    .background(Color.White.copy(alpha = 0.07f), CircleShape)
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onToggleEdit, colors = glass) {
                    Icon(
                        imageVector = if (editing) Icons.Default.Close else Icons.Default.Edit,
                        contentDescription = if (editing) "Cancel editing" else "Edit profile"
                    )
                }
                IconButton(onClick = onSettings, colors = glass) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }

        ProfileAvatar(
            letter = letter,
            model = avatarModel,
            editing = editing,
            colors = colors,
            onClick = onPickPhoto,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ProfileAvatar(
    letter: String,
    model: Any?,
    editing: Boolean,
    colors: ProfileColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(AvatarSize)) {
        Surface(
            shape = CircleShape,
            color = colors.cardBackground,
            shadowElevation = 8.dp,
            border = BorderStroke(4.dp, colors.backgroundGold),
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .clickable(enabled = editing, onClickLabel = "Change photo", onClick = onClick)
        ) {
            if (model == null) {
                Box(
                    modifier = Modifier.background(
                        Brush.linearGradient(
                            listOf(
                                colors.honeyGold.copy(alpha = 0.40f),
                                colors.honeyGold.copy(alpha = 0.12f)
                            )
                        )
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 42.sp,
                        color = colors.hiveGreen
                    )
                }
            } else {
                AsyncImage(
                    model = model,
                    contentDescription = "Profile photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        if (editing) {
            Surface(
                shape = CircleShape,
                color = colors.hiveGreen,
                border = BorderStroke(3.dp, colors.backgroundGold),
                shadowElevation = 4.dp,
                modifier = Modifier.align(Alignment.BottomEnd).size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ───────────────────────── View mode ─────────────────────────

@Composable
private fun ViewContent(
    user: ProfileUserUi?,
    estate: ProfileEstateUi?,
    isAdmin: Boolean,
    colors: ProfileColors,
    onShowInviteCode: () -> Unit,
    onAdminPanel: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = user?.displayName ?: user?.username ?: "Neighbor",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colors.darkGray,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        user?.username?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = colors.hiveGreen.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "@$it",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.hiveGreen,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        user?.bio?.takeIf { it.isNotBlank() }?.let { bio ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = bio,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.darkGray.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 36.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        if (estate != null) {
            SectionLabel("Your estate", colors)
            SectionCard(colors) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = estate.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.darkGray
                            )
                            if (estate.address.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = colors.darkGray.copy(alpha = 0.45f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = estate.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.darkGray.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                        if (isAdmin) {
                            Surface(shape = RoundedCornerShape(50), color = colors.hiveGreen) {
                                Text(
                                    text = "Admin",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colors.honeyGold.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "${estate.memberCount} member${if (estate.memberCount != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.darkGray.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                if (isAdmin) {
                    HorizontalDivider(color = colors.darkGray.copy(alpha = 0.07f))
                    ActionRow(
                        icon = Icons.Default.Share,
                        title = "Share invite code",
                        subtitle = "Let neighbours join ${estate.name}",
                        colors = colors,
                        onClick = onShowInviteCode
                    )
                    HorizontalDivider(
                        color = colors.darkGray.copy(alpha = 0.07f),
                        modifier = Modifier.padding(start = 72.dp)
                    )
                    ActionRow(
                        icon = Icons.Default.Lock,
                        title = "Admin panel",
                        subtitle = "Manage your estate",
                        colors = colors,
                        onClick = { onAdminPanel(estate.id) }
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        val contactRows = listOfNotNull(
            user?.email?.let { Triple(Icons.Default.Email, "Email", it) },
            user?.phone?.let { Triple(Icons.Default.Phone, "Phone", it) },
            user?.address?.let { Triple(Icons.Default.Home, "Address", it) },
            user?.occupation?.let { Triple(Icons.Default.AccountBox, "Occupation", it) }
        )
        if (contactRows.isNotEmpty()) {
            SectionLabel("About", colors)
            SectionCard(colors) {
                contactRows.forEachIndexed { index, (icon, label, value) ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = colors.darkGray.copy(alpha = 0.07f),
                            modifier = Modifier.padding(start = 72.dp)
                        )
                    }
                    InfoRow(icon, label, value, colors)
                }
            }
        }
    }
}

// ───────────────────────── Edit mode ─────────────────────────

@Composable
private fun EditContent(
    name: String,
    username: String,
    bio: String,
    isLoading: Boolean,
    colors: ProfileColors,
    onNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tap your photo to change it",
            style = MaterialTheme.typography.bodySmall,
            color = colors.darkGray.copy(alpha = 0.55f)
        )
        Spacer(Modifier.height(16.dp))

        SectionCard(colors) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Edit profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.darkGray
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Full name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = fieldColors(colors)
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("Username") },
                    prefix = { Text("@") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = fieldColors(colors)
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = onBioChange,
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 3,
                    maxLines = 5,
                    colors = fieldColors(colors)
                )

                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold, color = colors.darkGray)
                    }
                    Button(
                        onClick = onSave,
                        enabled = name.isNotBlank() && username.isNotBlank() && !isLoading,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.hiveGreen,
                            contentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── Building blocks ─────────────────────────

@Composable
private fun SectionLabel(text: String, colors: ProfileColors) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = colors.darkGray.copy(alpha = 0.5f),
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SectionCard(
    colors: ProfileColors,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        border = BorderStroke(1.dp, colors.darkGray.copy(alpha = 0.06f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content
    )
}

@Composable
private fun IconBadge(icon: ImageVector, colors: ProfileColors, size: Dp = 40.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.hiveGreen.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = colors.hiveGreen, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String, colors: ProfileColors) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon, colors)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.darkGray.copy(alpha = 0.5f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colors.darkGray
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    colors: ProfileColors,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon, colors)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.darkGray
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.darkGray.copy(alpha = 0.55f)
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.darkGray.copy(alpha = 0.35f)
        )
    }
}

@Composable
private fun InviteCodeDialog(
    inviteCode: String,
    estateName: String,
    colors: ProfileColors,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconBadge(Icons.Default.Share, colors, size = 52.dp)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Invite to $estateName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.darkGray,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Share this code with neighbours so they can join",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.darkGray.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.hiveGreen.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, colors.hiveGreen.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "INVITE CODE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.hiveGreen.copy(alpha = 0.75f),
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = inviteCode,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.hiveGreen,
                            letterSpacing = 6.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { onCopy(); onDismiss() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.hiveGreen)
                    ) {
                        Text("Copy", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onShare(); onDismiss() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.hiveGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun fieldColors(colors: ProfileColors) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = colors.hiveGreen,
    focusedLabelColor = colors.hiveGreen,
    unfocusedBorderColor = colors.darkGray.copy(alpha = 0.2f),
    cursorColor = colors.hiveGreen,
    focusedContainerColor = colors.cardBackground,
    unfocusedContainerColor = colors.cardBackground
)