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
import androidx.compose.material.icons.outlined.Settings
import com.eleazar.localhive.ui.components.ScreenHeader
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val isAdmin: Boolean,
    val isOwner: Boolean = false,
    val isTreasurer: Boolean = false,
    /** False while the estate takes requests only, when there's no code worth sharing. */
    val acceptsCodes: Boolean = true
)

private val AvatarSize = 96.dp

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
    onSave: (name: String, username: String, bio: String, occupation: String) -> Unit,
    onCopyInviteCode: (code: String) -> Unit,
    onShareInviteCode: (code: String, estateName: String) -> Unit,
    onSettings: () -> Unit,
    onAdminPanel: (estateId: String) -> Unit,
    onFinances: () -> Unit,
    modifier: Modifier = Modifier,
    onPolls: () -> Unit = {},
    onUtilities: () -> Unit = {},
    onVisitors: () -> Unit = {}
) {
    var showEdit by remember { mutableStateOf(false) }
    var showCodeDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editUsername by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var editOccupation by remember { mutableStateOf("") }

    fun resetFields() {
        editName = user?.displayName ?: ""
        editUsername = user?.username ?: ""
        editBio = user?.bio ?: ""
        editOccupation = user?.occupation ?: ""
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
        ScreenHeader(
            title = "Profile",
            action = {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = colors.darkGray)
                }
            }
        )
        ProfileAvatar(
            letter = letter,
            model = avatarModel,
            editing = showEdit,
            colors = colors,
            onClick = onPickPhoto,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
        )

        Spacer(Modifier.height(12.dp))

        AnimatedContent(
            targetState = showEdit,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
            label = "profile_mode"
        ) { editing ->
            if (editing) {
                ProfileEditContent(
                    name = editName,
                    username = editUsername,
                    bio = editBio,
                    occupation = editOccupation,
                    isLoading = isLoading,
                    colors = colors,
                    onNameChange = { editName = it },
                    onUsernameChange = { editUsername = it },
                    onBioChange = { editBio = it },
                    onOccupationChange = { editOccupation = it },
                    onCancel = {
                        resetFields()
                        showEdit = false
                    },
                    onSave = {
                        onSave(editName, editUsername, editBio, editOccupation)
                        showEdit = false
                    }
                )
            } else {
                ProfileViewContent(
                    user = user,
                    estate = estate,
                    isAdmin = isAdmin,
                    colors = colors,
                    onEdit = { showEdit = true },
                    onShowInviteCode = { showCodeDialog = true },
                    onAdminPanel = onAdminPanel,
                    onFinances = onFinances,
                    onPolls = onPolls,
                    onUtilities = onUtilities,
                    onVisitors = onVisitors
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

// ───────────────────────── Avatar ─────────────────────────

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
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .clickable(enabled = editing, onClickLabel = "Change photo", onClick = onClick)
        ) {
            if (model == null) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = letter,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 38.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.background),
                modifier = Modifier.align(Alignment.BottomEnd).size(30.dp)
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
